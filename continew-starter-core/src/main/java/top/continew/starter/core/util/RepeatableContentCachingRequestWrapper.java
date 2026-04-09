/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.continew.starter.core.util;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import top.continew.starter.core.constant.StringConstants;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import top.continew.starter.core.util.NumberUtils;

/**
 * 可重复读取请求体包装器
 *
 * <p>继承 Spring 官方的 ContentCachingRequestWrapper 以保持生态兼容。</p>
 * <p>支持文件上传（multipart/form-data）场景，文件部分直接透传原始请求流，其他表单参数仍然可以缓存读取。</p>
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class RepeatableContentCachingRequestWrapper extends ContentCachingRequestWrapper {

    /**
     * 是否为 multipart 请求
     */
    private final boolean isMultipart;

    /**
     * 字符编码
     */
    private final String characterEncoding;

    /**
     * 缓存内容
     */
    private final FastByteArrayOutputStream cachedContent;

    /**
     * 缓存的输入流
     */
    private RepeatableInputStream repeatableInputStream;

    public RepeatableContentCachingRequestWrapper(HttpServletRequest request) throws IOException {
        this(request, 0);
    }

    public RepeatableContentCachingRequestWrapper(HttpServletRequest request, int cacheLimit) throws IOException {
        super(request, cacheLimit > 0 ? cacheLimit : NumberUtils.zero2Default(request.getContentLength(), 256));
        this.isMultipart = ServletUtils.isMultipart(request);
        this.characterEncoding = request.getCharacterEncoding() != null
            ? request.getCharacterEncoding()
            : StandardCharsets.UTF_8.name();
        int contentLength = request.getContentLength();
        this.cachedContent = (contentLength > 0)
            ? new FastByteArrayOutputStream(contentLength)
            : new FastByteArrayOutputStream();

        if (!this.isMultipart) {
            if (ServletUtils.isFormPost(request)) {
                writeRequestParametersToCachedContent();
            } else {
                StreamUtils.copy(super.getInputStream(), cachedContent);
            }
            repeatableInputStream = new RepeatableInputStream(cachedContent.toByteArray());
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (this.isMultipart) {
            return super.getRequest().getInputStream();
        }
        synchronized (this) {
            repeatableInputStream.reset();
            return repeatableInputStream;
        }
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.isMultipart) {
            return super.getRequest().getReader();
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
    }

    /**
     * 将请求参数写入缓存内容
     */
    private void writeRequestParametersToCachedContent() {
        try {
            if (this.cachedContent.size() == 0) {
                Map<String, String[]> form = super.getParameterMap();
                for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext();) {
                    String name = nameIterator.next();
                    List<String> values = Arrays.asList(form.get(name));
                    for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext();) {
                        String value = valueIterator.next();
                        this.cachedContent.write(URLEncoder.encode(name, characterEncoding).getBytes());
                        if (value != null) {
                            this.cachedContent.write(StringConstants.EQUALS.getBytes());
                            this.cachedContent.write(URLEncoder.encode(value, characterEncoding).getBytes());
                            if (valueIterator.hasNext()) {
                                this.cachedContent.write(StringConstants.AMP.getBytes());
                            }
                        }
                    }
                    if (nameIterator.hasNext()) {
                        this.cachedContent.write(StringConstants.AMP.getBytes());
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write request parameters to cached content", ex);
        }
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    /**
     * 重复可读输入流
     */
    private static class RepeatableInputStream extends ServletInputStream {

        private final InputStream is;

        private RepeatableInputStream(byte[] content) {
            this.is = new ByteArrayInputStream(content);
        }

        @Override
        public int read() throws IOException {
            return this.is.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return this.is.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return this.is.read(b, off, len);
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Repeatable stream does not support async setReadListener");
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void close() throws IOException {
            this.is.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            this.is.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            this.is.reset();
        }

        @Override
        public boolean markSupported() {
            return this.is.markSupported();
        }
    }
}
