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

package top.continew.starter.messaging.websocket.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * {@link WebSocketUtils} 发送工具行为测试
 *
 * <p>{@code SESSION_DAO} 在工具类加载时经 {@code SpringUtil} 静态取回，故先以 mock
 * 上下文完成绑定再触发类加载；会话行为（开关状态、发送异常）由 mock 会话裁决，
 * 真实收发链路由真实端口 IT 覆盖，两者分工不重叠。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("WebSocketUtils 发送工具行为")
class WebSocketUtilsTest {

    private static final WebSocketSessionDao SESSION_DAO = mock(WebSocketSessionDao.class);

    @BeforeAll
    static void bindMockContextBeforeClassLoading() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(WebSocketSessionDao.class)).thenReturn(SESSION_DAO);
        new SpringUtil().setApplicationContext(applicationContext);
    }

    @Test
    @DisplayName("对开启状态的会话发送应投递相同负载")
    void shouldDeliverPayloadToOpenSession() throws IOException {
        WebSocketSession session = openSession();

        WebSocketUtils.sendMessage(session, "payload");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertEquals("payload", captor.getValue().getPayload());
    }

    @Test
    @DisplayName("对 null 会话发送应静默跳过")
    void shouldSkipNullSessionQuietly() {
        assertDoesNotThrow(() -> WebSocketUtils.sendMessage((WebSocketSession) null, "payload"));
    }

    @Test
    @DisplayName("对已关闭会话发送应静默跳过")
    void shouldSkipClosedSessionQuietly() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);

        WebSocketUtils.sendMessage(session, "payload");

        verify(session, never()).sendMessage(any());
    }

    @Test
    @DisplayName("发送出现 IO 异常应记录日志且不向外抛出")
    void shouldSwallowSendErrorOnDeliver() throws IOException {
        WebSocketSession session = openSession();
        doThrow(new IOException("broken pipe")).when(session).sendMessage(any());

        assertDoesNotThrow(() -> WebSocketUtils.sendMessage(session, "payload"));
    }

    @Test
    @DisplayName("按客户端 ID 发送应经 DAO 解析目标会话")
    void shouldResolveSessionFromDaoByClientId() throws IOException {
        WebSocketSession session = openSession();
        when(SESSION_DAO.get("user-1")).thenReturn(session);

        WebSocketUtils.sendMessage("user-1", "payload");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertEquals("payload", captor.getValue().getPayload());
    }

    @Test
    @DisplayName("批量发送应只投递到在线会话与目标列表的交集")
    void shouldDeliverBatchOnlyToIntersection() throws IOException {
        WebSocketSession targetA = openSession();
        WebSocketSession targetC = openSession();
        // b 在线但不在目标列表，交集运算应将其排除
        WebSocketSession outOfList = openSession();
        when(SESSION_DAO.listAllSessionIds()).thenReturn(Set.of("a", "b", "c"));
        when(SESSION_DAO.get("a")).thenReturn(targetA);
        when(SESSION_DAO.get("c")).thenReturn(targetC);

        WebSocketUtils.sendMessage(List.of("a", "c"), "batch-payload");

        verify(targetA).sendMessage(any());
        verify(targetC).sendMessage(any());
        verify(outOfList, never()).sendMessage(any());
    }

    @Test
    @DisplayName("广播应投递到当前全部在线会话")
    void shouldBroadcastToAllRegisteredSessions() throws IOException {
        WebSocketSession first = openSession();
        WebSocketSession second = openSession();
        when(SESSION_DAO.listAll()).thenReturn(List.of(first, second));

        WebSocketUtils.sendMessage("broadcast-payload");

        verify(first).sendMessage(any());
        verify(second).sendMessage(any());
    }

    @Test
    @DisplayName("按 Key 全量发送应投递到该 Key 下每一条连接")
    void shouldDeliverToEveryConnectionUnderKey() throws IOException {
        // 模拟支持多标签页的 DAO 实现：同一 Key 登记多条连接，默认实现下该 Key 仅一条
        WebSocketSession first = openSession();
        WebSocketSession second = openSession();
        when(SESSION_DAO.listByKey("multi-tab-user")).thenReturn(List.of(first, second));

        WebSocketUtils.sendMessageToAll("multi-tab-user", "multi-payload");

        verify(first).sendMessage(any());
        verify(second).sendMessage(any());
    }

    private static WebSocketSession openSession() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
