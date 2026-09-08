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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import top.continew.starter.core.util.ServletUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取响应内容的包装器
 * <p>
 * 写入时会同时镜像到缓存和原始响应流：缓存用于日志记录等后续读取，原始流保证数据实时推送给客户端，
 * 避免网关（如 Spring Cloud Gateway 代理 Servlet 服务）在响应体较大时因通道提前关闭而截断响应。
 * 流式响应（SSE）不做缓存处理。
 *
 * @author echo
 * @author Charles7c
 * @since 2.10.0
 */
public class RepeatReadResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream cachedOutputStream = new ByteArrayOutputStream();
    /**
     * 原始响应输出流（记忆化，避免重复调用 {@code super.getOutputStream()} 触发 IllegalStateException）
     */
    private ServletOutputStream originalOutputStream;
    /**
     * 同时写入缓存与原始响应流的输出流（记忆化）
     */
    private ServletOutputStream cachingOutputStream;
    /**
     * 同时写入缓存与原始响应流的字符写入器（记忆化）
     */
    private PrintWriter cachedWriter;
    /**
     * 是否为流式响应
     */
    private boolean isStreamingResponse = false;

    public RepeatReadResponseWrapper(HttpServletResponse response) {
        super(response);
        isStreamingResponse = ServletUtils.isStream(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        // 对于 SSE 流式响应，直接返回原始响应流，不做额外处理
        if (isStreamingResponse) {
            return super.getOutputStream();
        }
        if (cachingOutputStream == null) {
            final ServletOutputStream original = this.getOriginalOutputStream();
            cachingOutputStream = new ServletOutputStream() {

                @Override
                public boolean isReady() {
                    return original.isReady();
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    original.setWriteListener(writeListener);
                }

                @Override
                public void write(int b) throws IOException {
                    cachedOutputStream.write(b);
                    original.write(b);
                }

                @Override
                public void write(byte[] b) throws IOException {
                    cachedOutputStream.write(b);
                    original.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    cachedOutputStream.write(b, off, len);
                    original.write(b, off, len);
                }

                @Override
                public void flush() throws IOException {
                    original.flush();
                }
            };
        }
        return cachingOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (isStreamingResponse) {
            // 对于 SSE 流式响应，直接返回原始响应写入器，不做额外处理
            return super.getWriter();
        }
        if (cachedWriter == null) {
            final ServletOutputStream original = this.getOriginalOutputStream();
            // 字符按响应编码编码为字节后，直接写入原始字节流，避免 byte -> String -> byte 往返导致的乱码
            OutputStream teeOutputStream = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    cachedOutputStream.write(b);
                    original.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    cachedOutputStream.write(b, off, len);
                    original.write(b, off, len);
                }

                @Override
                public void flush() throws IOException {
                    original.flush();
                }
            };
            cachedWriter =
                new PrintWriter(new OutputStreamWriter(teeOutputStream, this.getCharset()), true);
        }
        return cachedWriter;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (cachedWriter != null) {
            cachedWriter.flush();
        }
        if (cachingOutputStream != null) {
            cachingOutputStream.flush();
        }
        super.flushBuffer();
    }

    /**
     * 获取缓存的响应内容
     *
     * @return 缓存的响应内容
     */
    public String getResponseContent() {
        if (!isStreamingResponse) {
            if (cachedWriter != null) {
                cachedWriter.flush();
            }
            return cachedOutputStream.toString(this.getCharset());
        }
        return null;
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
     * 获取原始响应输出流（记忆化）
     *
     * @return 原始响应输出流
     * @throws IOException IO 异常
     */
    private ServletOutputStream getOriginalOutputStream() throws IOException {
        if (originalOutputStream == null) {
            originalOutputStream = super.getOutputStream();
        }
        return originalOutputStream;
    }

    /**
     * 获取响应字符集（未指定时回退到 UTF-8）
     *
     * @return 字符集
     */
    private Charset getCharset() {
        String encoding = this.getCharacterEncoding();
        return (encoding == null || encoding.isEmpty()) ? StandardCharsets.UTF_8
            : Charset.forName(encoding);
    }
}
