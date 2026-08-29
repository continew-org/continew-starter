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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取响应内容的包装器
 * 支持缓存响应内容，便于日志记录和后续处理 (不缓存SSE)
 *
 * @author echo
 * @author Charles7c
 * @since 2.10.0
 */
public class RepeatReadResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream cachedOutputStream = new ByteArrayOutputStream();
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
        ServletOutputStream originalOutputStream = super.getOutputStream();
        return new ServletOutputStream() {

            @Override
            public boolean isReady() {
                return originalOutputStream.isReady();
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                originalOutputStream.setWriteListener(writeListener);
            }

            @Override
            public void write(int b) throws IOException {
                cachedOutputStream.write(b);
                originalOutputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                cachedOutputStream.write(b);
                originalOutputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                cachedOutputStream.write(b, off, len);
                originalOutputStream.write(b, off, len);
            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (isStreamingResponse) {
            // 对于 SSE 流式响应，直接返回原始响应写入器，不做额外处理
            return super.getWriter();
        }
        if (cachedWriter == null) {
            PrintWriter originalWriter = super.getWriter();
            cachedWriter = new PrintWriter(new java.io.OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    cachedOutputStream.write(b);
                    originalWriter.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    cachedOutputStream.write(b, off, len);
                    originalWriter.write(new String(b, off, len, StandardCharsets.UTF_8));
                }

                @Override
                public void flush() throws IOException {
                    originalWriter.flush();
                }
            }, true);
        }
        return cachedWriter;
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
            return cachedOutputStream.toString(StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * 将缓存的响应内容复制到原始响应中
     * 注意：由于已经在写入时同步到原始响应流，此方法不再需要执行任何操作
     *
     * @throws IOException IO 异常
     */
    public void copyBodyToResponse() throws IOException {
        // 不再需要复制，因为数据已经在写入时同步到原始响应流
    }

    /**
     * 是否为流式响应
     *
     * @return 是否为流式响应
     */
    public boolean isStreamingResponse() {
        return isStreamingResponse;
    }
}
