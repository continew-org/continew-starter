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

package top.continew.starter.messaging.websocket.core;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import top.continew.starter.messaging.websocket.autoconfigure.WebSocketProperties;

import java.util.Map;

/**
 * WebSocket 拦截器
 *
 * @author WeiRan
 * @author Charles7c
 * @since 2.1.0
 */
public class WebSocketInterceptor extends HttpSessionHandshakeInterceptor {

    private final WebSocketProperties webSocketProperties;
    private final WebSocketClientService webSocketClientService;

    public WebSocketInterceptor(WebSocketProperties webSocketProperties,
                                WebSocketClientService webSocketClientService) {
        this.webSocketProperties = webSocketProperties;
        this.webSocketClientService = webSocketClientService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String clientId = webSocketClientService.getClientId((ServletServerHttpRequest)request);
        attributes.put(webSocketProperties.getClientIdKey(), clientId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        super.afterHandshake(request, response, wsHandler, exception);
    }
}
