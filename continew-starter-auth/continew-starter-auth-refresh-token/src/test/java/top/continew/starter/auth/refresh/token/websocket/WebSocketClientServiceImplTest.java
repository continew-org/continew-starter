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

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import top.continew.starter.auth.refresh.token.api.AccessSessionValidator;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.core.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * WebSocket 握手凭证与可记录客户端标识分离测试。
 *
 * @author Charles7c
 * @since 2.17.0
 */
class WebSocketClientServiceImplTest {

    private static final String SECRET = "test-only-refresh-token-secret-with-32-bytes";

    @Test
    void shouldReturnIrreversibleFingerprintAsClientIdAndRegisterRawToken() {
        RefreshTokenCodec codec = this.codec();
        AccessSessionValidator validator = mock(AccessSessionValidator.class);
        when(validator.isInvalid(anyString())).thenReturn(false);
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        WebSocketClientServiceImpl service = new WebSocketClientServiceImpl(validator, codec,
            registry);
        String rawToken = codec.issue(codec.newSessionId()).rawToken();

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(rawToken)).thenReturn(1L);

            String clientId = service.getClientId(this.handshake(rawToken));
            // 客户端 ID 会进入 DAO 索引与连接生命周期日志，绝不能是原始凭证本身。
            assertNotEquals(rawToken, clientId);
            assertEquals(codec.fingerprint(rawToken), clientId);
            assertEquals(rawToken, registry.resolve(clientId));

            // 同一 Access Token 的多标签页握手得到同一客户端 ID。
            String secondTab = service.getClientId(this.handshake(rawToken));
            assertEquals(clientId, secondTab);
        }
    }

    @Test
    void shouldRejectHandshakeWithoutTokenOrInvalidSession() {
        RefreshTokenCodec codec = this.codec();
        AccessSessionValidator validator = mock(AccessSessionValidator.class);
        when(validator.isInvalid(anyString())).thenReturn(true);
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        WebSocketClientServiceImpl service = new WebSocketClientServiceImpl(validator, codec,
            registry);
        String rawToken = codec.issue(codec.newSessionId()).rawToken();
        ServletServerHttpRequest blankHandshake = this.handshake(null);
        ServletServerHttpRequest tokenHandshake = this.handshake(rawToken);

        // 未携带 Token
        assertThrows(BusinessException.class, () -> service.getClientId(blankHandshake));

        // 携带 Token 但认证会话已失效，握手失败时不得登记任何凭证映射
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(rawToken)).thenReturn(1L);

            assertThrows(BusinessException.class, () -> service.getClientId(tokenHandshake));
            assertNull(registry.resolve(codec.fingerprint(rawToken)));
        }
    }

    private RefreshTokenCodec codec() {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setSecret(SECRET);
        return new RefreshTokenCodec(properties);
    }

    private ServletServerHttpRequest handshake(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.setParameter("token", token);
        }
        return new ServletServerHttpRequest(request);
    }
}
