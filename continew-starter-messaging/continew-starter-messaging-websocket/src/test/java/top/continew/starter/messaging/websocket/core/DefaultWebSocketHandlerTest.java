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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import top.continew.starter.messaging.websocket.autoconfigure.WebSocketProperties;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link DefaultWebSocketHandler} 生命周期回调测试
 *
 * <p>会话注册与注销由 mock DAO 记账裁决，握手 attributes 的真实解析路径由真实端口
 * IT 覆盖。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("DefaultWebSocketHandler 生命周期回调")
class DefaultWebSocketHandlerTest {

    private static final String CLIENT_ID_KEY = "CLIENT_ID";

    private final WebSocketSessionDao sessionDao = mock(WebSocketSessionDao.class);
    private final DefaultWebSocketHandler handler =
        new DefaultWebSocketHandler(new WebSocketProperties(), sessionDao);
    private final Map<String, Object> attributes = new HashMap<>();

    private WebSocketSession session;

    @BeforeEach
    void prepareSession() {
        session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(attributes);
    }

    @Test
    @DisplayName("连接建立应按 attributes 中的客户端 ID 注册会话")
    void shouldRegisterSessionUnderAttributesClientId() {
        attributes.put(CLIENT_ID_KEY, "user-1");

        handler.afterConnectionEstablished(session);

        verify(sessionDao).add("user-1", session);
    }

    @Test
    @DisplayName("attributes 缺失客户端 ID 时应以 null 登记（现行为锁定）")
    void shouldRegisterNullClientIdWhenAttributesMissing() {
        handler.afterConnectionEstablished(session);

        verify(sessionDao).add(isNull(), eq(session));
    }

    @Test
    @DisplayName("连接关闭应按客户端 ID 注销会话")
    void shouldRemoveSessionOnConnectionClosed() {
        attributes.put(CLIENT_ID_KEY, "user-1");

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionDao).delete("user-1");
    }

    @Test
    @DisplayName("传输错误且会话开启时应关闭并注销会话")
    void shouldCloseAndRemoveSessionOnTransportErrorWhenOpen() throws IOException {
        attributes.put(CLIENT_ID_KEY, "user-1");
        when(session.isOpen()).thenReturn(true);

        handler.handleTransportError(session, new IllegalStateException("boom"));

        verify(session).close();
        verify(sessionDao).delete("user-1");
    }

    @Test
    @DisplayName("传输错误且会话已关闭时应只注销不再重复关闭")
    void shouldOnlyRemoveSessionOnTransportErrorWhenClosed() throws IOException {
        attributes.put(CLIENT_ID_KEY, "user-1");
        when(session.isOpen()).thenReturn(false);

        handler.handleTransportError(session, new IllegalStateException("boom"));

        verify(session, never()).close();
        verify(sessionDao).delete("user-1");
    }

    @Test
    @DisplayName("收到文本消息不应触碰会话 DAO，且不中断连接")
    void shouldTolerateTextMessageWithoutDaoSideEffect() {
        attributes.put(CLIENT_ID_KEY, "user-1");

        assertDoesNotThrow(() -> handler.handleTextMessage(session, new TextMessage("ping")));

        verifyNoInteractions(sessionDao);
    }
}
