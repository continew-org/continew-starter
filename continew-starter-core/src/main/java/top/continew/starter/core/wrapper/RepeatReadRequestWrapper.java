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

package top.continew.starter.core.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StreamUtils;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.ServletUtils;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 可重复读取请求体的包装器 支持文件流直接透传，非文件流可重复读取
 * <p>
 * 虽然这里可以多次读取流里面的数据， 但是建议还是调用getContentAsString()/getCachedContent() 方法， 已经把内容缓存在内存中了。
 * </p>
 *
 * @author Jasmine
 * @since 2.12.1
 */
public class RepeatReadRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 缓存内容
     */
    private final FastByteArrayOutputStream cachedContent;

    /*** 用于缓存输入流 */
    private ContentCachingInputStream contentCachingInputStream;

    /**
     * 字符编码
     */
    private final String characterEncoding;

    // private BufferedReader reader;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request the {@link HttpServletRequest} to be wrapped.
     * @throws IllegalArgumentException if the request is null
     */
    public RepeatReadRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.characterEncoding = request.getCharacterEncoding() != null
            ? request.getCharacterEncoding()
            : StandardCharsets.UTF_8.name();
        int contentLength = super.getRequest().getContentLength();
        cachedContent = (contentLength > 0)
            ? new FastByteArrayOutputStream(contentLength)
            : new FastByteArrayOutputStream();
        // 判断是否为文件上传请求
        if (!ServletUtils.isMultipart(request)) {
            if (ServletUtils.isForm(request)) {
                writeRequestParametersToCachedContent();
            } else {
                StreamUtils.copy(request.getInputStream(), cachedContent);
            }
            contentCachingInputStream = new ContentCachingInputStream(cachedContent.toByteArray());
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 如果是文件上传，直接返回原始输入流
        if (ServletUtils.isMultipart((HttpServletRequest)super.getRequest())) {
            return super.getRequest().getInputStream();
        }
        synchronized (this) {
            contentCachingInputStream.reset();
            return contentCachingInputStream;
        }
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // 如果是文件上传，直接返回原始Reader
        if (ServletUtils.isMultipart((HttpServletRequest)super.getRequest())) {
            return super.getRequest().getReader();
        }

        // BufferedReader不支持多次reset()（除非手动调用 mark() 并控制其生命周期），最安全的方式是每次调用getReader()时基于缓存内容重新创建一个新的BufferedReader实例。
        synchronized (this) {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }
    }

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

    public String getContentAsString() {
        return this.cachedContent.toString(Charset.forName(getCharacterEncoding()));
    }

    public FastByteArrayOutputStream getCachedContent() {
        return cachedContent;
    }

    /**
     * Body 缓存的ServletInputStream实现 DefaultServerRequestBuilder.BodyInputStream
     */
    private static class ContentCachingInputStream extends ServletInputStream {

        private final InputStream delegate;

        public ContentCachingInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        public boolean isFinished() {
            return false;
        }

        public boolean isReady() {
            return true;
        }

        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        public int read() throws IOException {
            return this.delegate.read();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return this.delegate.read(b, off, len);
        }

        public int read(byte[] b) throws IOException {
            return this.delegate.read(b);
        }

        public long skip(long n) throws IOException {
            return this.delegate.skip(n);
        }

        public int available() throws IOException {
            return this.delegate.available();
        }

        public void close() throws IOException {
            this.delegate.close();
        }

        public synchronized void mark(int readlimit) {
            this.delegate.mark(readlimit);
        }

        public synchronized void reset() throws IOException {
            this.delegate.reset();
        }

        public boolean markSupported() {
            return this.delegate.markSupported();
        }
    }
}
