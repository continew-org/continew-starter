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

package top.continew.starter.auth.refresh.token.websocket;

import org.springframework.context.Lifecycle;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import top.continew.starter.auth.refresh.token.support.WebSocketTokenExtractor;

import jakarta.servlet.ServletContext;
import java.util.Map;

/**
 * 使用不含凭证协议项的请求头执行 Spring 握手。
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class AuthHandshakeHandler implements HandshakeHandler, ServletContextAware, Lifecycle {

    private final DefaultHandshakeHandler delegate = new DefaultHandshakeHandler();

    @Override
    public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Map<String, Object> attributes) {
        ServletServerHttpRequest handshake =
            WebSocketTokenExtractor.forHandshake((ServletServerHttpRequest) request);
        return delegate.doHandshake(handshake, response, wsHandler, attributes);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        delegate.setServletContext(servletContext);
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }
}
