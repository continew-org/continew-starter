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
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 可重复读取请求体包装器
 *
 * <p>继承 Spring 官方的 ContentCachingRequestWrapper 以保持生态兼容。</p>
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class RepeatableContentCachingRequestWrapper extends ContentCachingRequestWrapper {

    public RepeatableContentCachingRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        StreamUtils.drain(super.getInputStream());
    }

    public RepeatableContentCachingRequestWrapper(HttpServletRequest request, int cacheLimit) throws IOException {
        super(request, cacheLimit > 0 ? cacheLimit : NumberUtils.zero2Default(request.getContentLength(), 256));
        StreamUtils.drain(super.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new RepeatableInputStream(super.getContentAsByteArray());
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
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Repeatable stream does not support async setReadListener");
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void close() throws IOException {
            this.is.close();
        }
    }
}
