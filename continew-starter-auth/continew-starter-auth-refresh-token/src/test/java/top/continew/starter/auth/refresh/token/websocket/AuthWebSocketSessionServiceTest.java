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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import top.continew.starter.auth.refresh.token.api.AccessSessionValidator;
import top.continew.starter.auth.refresh.token.api.AuthSessionConstants;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import java.util.Set;

/**
 * Refresh Session 撤销与 WebSocket 连接联动测试。
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
class AuthWebSocketSessionServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldCloseLocalConnectionAndPublishClusterRevocation() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider = mock(ObjectProvider.class);
        ObjectProvider<AccessSessionValidator> authSessionApiProvider = mock(ObjectProvider.class);
        WebSocketSessionDao sessionDao = mock(WebSocketSessionDao.class);
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        when(redissonClient.getTopic(anyString())).thenReturn(topic);
        when(topic.addListener(eq(String.class), any(MessageListener.class))).thenReturn(1);
        when(sessionDaoProvider.getIfAvailable()).thenReturn(sessionDao);
        when(sessionDao.listAllSessionIds()).thenReturn(Set.of("client-id"));
        when(sessionDao.get("client-id")).thenReturn(webSocketSession);
        when(webSocketSession.isOpen()).thenReturn(true);
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        registry.register("client-id", "raw-access-token");

        AuthWebSocketSessionService service = this.service(redissonClient, sessionDaoProvider,
            authSessionApiProvider, registry);
        service.subscribe();
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getExtra("raw-access-token",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn("session-1");

            service.notifyRevoked("session-1");
        } finally {
            service.unsubscribe();
        }

        verify(webSocketSession).close(CloseStatus.POLICY_VIOLATION);
        verify(sessionDao).delete("client-id");
        verify(topic).publish("session-1");
        verify(topic).removeListener(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStillCloseLocalConnectionWhenClusterPublishFails() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider = mock(ObjectProvider.class);
        ObjectProvider<AccessSessionValidator> authSessionApiProvider = mock(ObjectProvider.class);
        WebSocketSessionDao sessionDao = mock(WebSocketSessionDao.class);
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        when(redissonClient.getTopic(anyString())).thenReturn(topic);
        when(topic.addListener(eq(String.class), any(MessageListener.class))).thenReturn(1);
        when(topic.publish("session-1")).thenThrow(new IllegalStateException("Redis unavailable"));
        when(sessionDaoProvider.getIfAvailable()).thenReturn(sessionDao);
        when(sessionDao.listAllSessionIds()).thenReturn(Set.of("client-id"));
        when(sessionDao.get("client-id")).thenReturn(webSocketSession);
        when(webSocketSession.isOpen()).thenReturn(true);
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        registry.register("client-id", "raw-access-token");

        AuthWebSocketSessionService service = this.service(redissonClient, sessionDaoProvider,
            authSessionApiProvider, registry);
        service.subscribe();
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getExtra("raw-access-token",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn("session-1");

            service.notifyRevoked("session-1");
        } finally {
            service.unsubscribe();
        }

        verify(webSocketSession).close(CloseStatus.POLICY_VIOLATION);
        verify(sessionDao).delete("client-id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCloseOnlyInvalidConnectionsDuringPeriodicValidation() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider = mock(ObjectProvider.class);
        ObjectProvider<AccessSessionValidator> authSessionApiProvider = mock(ObjectProvider.class);
        WebSocketSessionDao sessionDao = mock(WebSocketSessionDao.class);
        AccessSessionValidator authSessionApi = mock(AccessSessionValidator.class);
        WebSocketSession invalidSession = mock(WebSocketSession.class);
        when(sessionDaoProvider.getIfAvailable()).thenReturn(sessionDao);
        when(authSessionApiProvider.getIfAvailable()).thenReturn(authSessionApi);
        // 第一次枚举用于逐连接校验，关闭失效连接后第二次枚举已不再包含 invalid-client。
        when(sessionDao.listAllSessionIds())
            .thenReturn(Set.of("invalid-client", "valid-client"), Set.of("valid-client"));
        when(authSessionApi.isInvalid("raw-invalid-token")).thenReturn(true);
        when(authSessionApi.isInvalid("raw-valid-token")).thenReturn(false);
        when(sessionDao.get("invalid-client")).thenReturn(invalidSession);
        when(invalidSession.isOpen()).thenReturn(true);
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        registry.register("invalid-client", "raw-invalid-token");
        registry.register("valid-client", "raw-valid-token");

        AuthWebSocketSessionService service = this.service(redissonClient, sessionDaoProvider,
            authSessionApiProvider, registry);
        service.validateLocalSessions();

        verify(invalidSession).close(CloseStatus.POLICY_VIOLATION);
        verify(sessionDao).delete("invalid-client");
        verify(sessionDao, never()).delete("valid-client");
        // 已关闭连接的凭证登记随周期校验一并清理
        assertNull(registry.resolve("invalid-client"));
        assertEquals("raw-valid-token", registry.resolve("valid-client"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCloseAllConnectionsOfSameTokenViaMultiDao() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider = mock(ObjectProvider.class);
        ObjectProvider<AccessSessionValidator> authSessionApiProvider = mock(ObjectProvider.class);
        MultiWebSocketSessionDao sessionDao = mock(MultiWebSocketSessionDao.class);
        WebSocketSession firstTab = mock(WebSocketSession.class);
        WebSocketSession secondTab = mock(WebSocketSession.class);
        when(sessionDaoProvider.getIfAvailable()).thenReturn(sessionDao);
        when(sessionDao.listAllSessionIds()).thenReturn(Set.of("client-id"));
        when(sessionDao.removeAll("client-id")).thenReturn(java.util.List.of(firstTab, secondTab));
        when(firstTab.isOpen()).thenReturn(true);
        when(secondTab.isOpen()).thenReturn(true);
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        registry.register("client-id", "raw-access-token");

        AuthWebSocketSessionService service = this.service(redissonClient, sessionDaoProvider,
            authSessionApiProvider, registry);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getExtra("raw-access-token",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn("session-1");

            service.notifyRevoked("session-1");
        }

        verify(firstTab).close(CloseStatus.POLICY_VIOLATION);
        verify(secondTab).close(CloseStatus.POLICY_VIOLATION);
        verify(sessionDao).removeAll("client-id");
        verify(sessionDao, never()).delete(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReuseLocalSessionCacheAcrossBatchRevocations() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider = mock(ObjectProvider.class);
        ObjectProvider<AccessSessionValidator> authSessionApiProvider = mock(ObjectProvider.class);
        WebSocketSessionDao sessionDao = mock(WebSocketSessionDao.class);
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);
        WebSocketSession sessionC = mock(WebSocketSession.class);
        when(sessionDaoProvider.getIfAvailable()).thenReturn(sessionDao);
        // 首次撤销后 client-a 的连接被关闭并从 DAO 移除，后续扫描只剩 b、c。
        when(sessionDao.listAllSessionIds())
            .thenReturn(Set.of("client-a", "client-b", "client-c"),
                Set.of("client-b", "client-c"));
        when(sessionDao.get("client-a")).thenReturn(sessionA);
        when(sessionDao.get("client-b")).thenReturn(sessionB);
        when(sessionDao.get("client-c")).thenReturn(sessionC);
        when(sessionA.isOpen()).thenReturn(true);
        when(sessionB.isOpen()).thenReturn(true);
        when(sessionC.isOpen()).thenReturn(true);
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        registry.register("client-a", "raw-token-a");
        registry.register("client-b", "raw-token-b");
        registry.register("client-c", "raw-token-c");

        AuthWebSocketSessionService service = this.service(redissonClient, sessionDaoProvider,
            authSessionApiProvider, registry);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getExtra("raw-token-a",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn("session-a");
            stpUtil.when(() -> StpUtil.getExtra("raw-token-b",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn("session-b");
            stpUtil.when(() -> StpUtil.getExtra("raw-token-c",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn("session-c");

            // 首次撤销：三个客户端的会话声明全部冷启动回源并写入本地索引。
            service.notifyRevoked("session-a");
            verify(sessionA).close(CloseStatus.POLICY_VIOLATION);
            verify(sessionB, never()).close(any());
            stpUtil.verify(() -> StpUtil.getExtra(anyString(), anyString()), times(3));

            // 第二次撤销：client-b/client-c 命中本地索引，不再回源 Redis。
            service.notifyRevoked("session-b");
            verify(sessionB).close(CloseStatus.POLICY_VIOLATION);
            stpUtil.verify(() -> StpUtil.getExtra(anyString(), anyString()), times(3));
        }
    }

    private AuthWebSocketSessionService service(RedissonClient redissonClient,
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider,
        ObjectProvider<AccessSessionValidator> accessSessionValidatorProvider,
        WebSocketCredentialRegistry credentialRegistry) {
        return new AuthWebSocketSessionService(redissonClient, sessionDaoProvider,
            accessSessionValidatorProvider, credentialRegistry);
    }
}
