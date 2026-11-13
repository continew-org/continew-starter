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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import top.continew.starter.messaging.websocket.autoconfigure.WebSocketProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link WebSocketInterceptor} 握手拦截器测试
 *
 * <p>请求与响应使用 spring-test 真实 Servlet 模拟对象，clientId 取值链路由 mock 的
 * {@link WebSocketClientService} 承接；无 HttpSession 的裸握手请求即为被测输入。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("WebSocketInterceptor 握手拦截器")
class WebSocketInterceptorTest {

    private final WebSocketClientService clientService = mock(WebSocketClientService.class);
    private final WebSocketProperties properties = new WebSocketProperties();
    private final WebSocketInterceptor interceptor = new WebSocketInterceptor(properties,
        clientService);

    @Test
    @DisplayName("握手前应将客户端服务解析结果放入 attributes 并放行")
    void shouldPutClientIdIntoAttributesAndAllowHandshake() {
        when(clientService.getClientId(any(ServletServerHttpRequest.class))).thenReturn("user-1");

        Map<String, Object> attributes = new HashMap<>();
        boolean allowed = interceptor.beforeHandshake(request(), response(), mock(
            WebSocketHandler.class), attributes);

        assertTrue(allowed);
        assertEquals("user-1", attributes.get(properties.getClientIdKey()));
    }

    @Test
    @DisplayName("客户端 ID 应存入配置指定的 attribute Key")
    void shouldUseConfiguredClientIdKey() {
        properties.setClientIdKey("UID");
        when(clientService.getClientId(any(ServletServerHttpRequest.class)))
            .thenReturn("user-2");

        Map<String, Object> attributes = new HashMap<>();
        interceptor.beforeHandshake(request(), response(), mock(WebSocketHandler.class),
            attributes);

        assertEquals("user-2", attributes.get("UID"));
    }

    @Test
    @DisplayName("原始握手请求应原样透传给客户端服务")
    void shouldPassSameRequestToClientService() {
        ServletServerHttpRequest request = request();

        interceptor.beforeHandshake(request, response(), mock(WebSocketHandler.class),
            new HashMap<>());

        verify(clientService).getClientId(same(request));
    }

    private static ServletServerHttpRequest request() {
        return new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/websocket"));
    }

    private static ServerHttpResponse response() {
        return new ServletServerHttpResponse(new MockHttpServletResponse());
    }
}
