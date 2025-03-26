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

package top.continew.starter.web.util;

import cn.hutool.core.io.IoUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取请求体的包装器
 * 支持文件流直接透传，非文件流可重复读取
 *
 * @author echo
 * @since 2.10.0
 */
public class RepeatReadRequestWrapper extends HttpServletRequestWrapper {

    private byte[] cachedBody;
    private final HttpServletRequest originalRequest;

    public RepeatReadRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.originalRequest = request;

        // 判断是否为文件上传请求
        if (!isMultipartContent(request)) {
            this.cachedBody = IoUtil.readBytes(request.getInputStream(), false);
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 如果是文件上传，直接返回原始输入流
        if (isMultipartContent(originalRequest)) {
            return originalRequest.getInputStream();
        }

        // 非文件上传，返回可重复读取的输入流
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 非阻塞I/O，这里可以根据需要实现
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // 如果是文件上传，直接返回原始Reader
        if (isMultipartContent(originalRequest)) {
            return originalRequest.getReader();
        }
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
    }

    /**
     * 检查是否为文件上传请求
     * 
     * @param request 请求对象
     * @return 是否为文件上传请求
     */
    private boolean isMultipartContent(HttpServletRequest request) {
        return request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/");
    }
}
