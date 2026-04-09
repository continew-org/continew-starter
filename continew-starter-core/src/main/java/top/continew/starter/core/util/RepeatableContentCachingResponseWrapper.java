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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 可重复读取响应体包装器
 *
 * <p>继承 Spring 官方的 ContentCachingResponseWrapper 以保持生态兼容。</p>
 * <p>支持检测流式响应（SSE），对于流式响应直接透传原始响应流，不进行缓存。</p>
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class RepeatableContentCachingResponseWrapper extends ContentCachingResponseWrapper {

    /**
     * 是否为流式响应
     */
    private boolean isStreamingResponse = false;

    /**
     * 原始响应输出流
     */
    private ServletOutputStream originalOutputStream;

    /**
     * 原始响应写入器
     */
    private PrintWriter originalWriter;

    /**
     * 缓存的输出流
     */
    private final FastByteArrayOutputStream cachedContent;

    /**
     * 自定义输出流
     */
    private ServletOutputStream outputStream;

    /**
     * 自定义写入器
     */
    private PrintWriter writer;

    public RepeatableContentCachingResponseWrapper(HttpServletResponse response) {
        super(response);
        this.isStreamingResponse = ServletUtils.isStream(response);
        int contentLength = response.getBufferSize();
        this.cachedContent = (contentLength > 0)
            ? new FastByteArrayOutputStream(contentLength)
            : new FastByteArrayOutputStream();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.isStreamingResponse) {
            if (this.originalOutputStream == null) {
                this.originalOutputStream = super.getResponse().getOutputStream();
            }
            return this.originalOutputStream;
        }
        if (this.outputStream == null) {
            this.outputStream = new ResponseServletOutputStream(this.cachedContent);
        }
        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.isStreamingResponse) {
            if (this.originalWriter == null) {
                this.originalWriter = super.getResponse().getWriter();
            }
            return this.originalWriter;
        }
        if (this.writer == null) {
            this.writer = new ResponsePrintWriter(this.cachedContent, getCharacterEncoding());
        }
        return this.writer;
    }

    @Override
    public void copyBodyToResponse() throws IOException {
        if (!this.isStreamingResponse && this.cachedContent.size() > 0) {
            super.getResponse().getOutputStream().write(this.cachedContent.toByteArray());
        }
    }

    @Override
    public byte[] getContentAsByteArray() {
        if (this.isStreamingResponse) {
            return new byte[0];
        }
        return this.cachedContent.toByteArray();
    }

    /**
     * 是否为流式响应
     *
     * @return 是否为流式响应
     */
    public boolean isStreamingResponse() {
        return isStreamingResponse;
    }

    /**
     * 响应 Servlet 输出流
     */
    private static class ResponseServletOutputStream extends ServletOutputStream {

        private final FastByteArrayOutputStream cachedContent;

        ResponseServletOutputStream(FastByteArrayOutputStream cachedContent) {
            this.cachedContent = cachedContent;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new UnsupportedOperationException("WriteListener not supported");
        }

        @Override
        public void write(int b) throws IOException {
            this.cachedContent.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.cachedContent.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.cachedContent.write(b, off, len);
        }
    }

    /**
     * 响应 PrintWriter
     */
    private static class ResponsePrintWriter extends PrintWriter {

        private final FastByteArrayOutputStream cachedContent;

        ResponsePrintWriter(FastByteArrayOutputStream cachedContent, String characterEncoding) {
            super(cachedContent, true);
            this.cachedContent = cachedContent;
        }
    }
}
