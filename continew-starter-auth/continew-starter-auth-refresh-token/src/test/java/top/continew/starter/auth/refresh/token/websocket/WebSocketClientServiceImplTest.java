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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHttpHeaders;
import top.continew.starter.auth.refresh.token.api.AccessSessionValidator;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.core.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * WebSocket 握手凭证与可记录客户端标识分离测试。
 *
 * @author Charles7c
 * @since 2.17.0
 */
class WebSocketClientServiceImplTest {

    private static final String SECRET = "test-only-refresh-token-secret-with-32-bytes";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJsb2dpbklkIjoxfQ.dGVzdA";
    private static final String QUERY_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJsb2dpbklkIjoyfQ.dGVzdA";

    private final RefreshTokenCodec codec = this.codec();
    private final AccessSessionValidator validator = mock(AccessSessionValidator.class);
    private final WebSocketCredentialRegistry registry = spy(new WebSocketCredentialRegistry());
    private final WebSocketClientServiceImpl service = new WebSocketClientServiceImpl(validator,
        codec, registry);

    @Test
    void shouldReturnIrreversibleFingerprintAsClientIdAndRegisterRawToken() {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN)).thenReturn(1L);

            String clientId = service.getClientId(this.handshake(ACCESS_TOKEN));
            assertNotEquals(ACCESS_TOKEN, clientId);
            assertEquals(codec.fingerprint(ACCESS_TOKEN), clientId);
            assertEquals(ACCESS_TOKEN, registry.resolve(clientId));

            String secondTab = service.getClientId(this.handshake(ACCESS_TOKEN));
            assertEquals(clientId, secondTab);
        }
    }

    @Test
    void shouldRejectHandshakeWithoutTokenOrInvalidSession() {
        when(validator.isInvalid(ACCESS_TOKEN)).thenReturn(true);
        ServletServerHttpRequest blankHandshake = this.handshake(null);
        ServletServerHttpRequest tokenHandshake = this.handshake(ACCESS_TOKEN);
        assertThrows(BusinessException.class, () -> service.getClientId(blankHandshake));

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN)).thenReturn(1L);
            assertThrows(BusinessException.class, () -> service.getClientId(tokenHandshake));
            assertNull(registry.resolve(codec.fingerprint(ACCESS_TOKEN)));
            verify(registry, never()).register(any(), any());
        }
    }

    @Test
    void shouldAcceptTokenFromSecWebSocketProtocol() {
        String protocols = "continew.auth.v1, " + this.bearerProtocol(ACCESS_TOKEN);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN)).thenReturn(1L);

            String clientId = service.getClientId(this.handshake(null, protocols));
            assertEquals(codec.fingerprint(ACCESS_TOKEN), clientId);
            assertEquals(ACCESS_TOKEN, registry.resolve(clientId));
        }
    }

    @Test
    void shouldPreferFirstDecodableTokenAcrossHeaderLinesOverQuery() {
        String accessOffer = this.bearerProtocol(ACCESS_TOKEN);
        String queryOffer = this.bearerProtocol(QUERY_TOKEN);
        ServletServerHttpRequest request = this.handshake(QUERY_TOKEN,
            "continew.auth.v10, continew.auth.v1", "Bearer.A, " + accessOffer, queryOffer);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN)).thenReturn(1L);
            stpUtil.when(() -> StpUtil.getLoginIdByToken(QUERY_TOKEN)).thenReturn(2L);

            String clientId = service.getClientId(request);
            assertEquals(codec.fingerprint(ACCESS_TOKEN), clientId);
            assertEquals(ACCESS_TOKEN, registry.resolve(clientId));
            assertNull(registry.findClientIdByToken(QUERY_TOKEN));
            stpUtil.verify(() -> StpUtil.getLoginIdByToken(QUERY_TOKEN), never());
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());
            assertEquals(List.of("continew.auth.v10", "continew.auth.v1", "Bearer.A", accessOffer,
                queryOffer), headers.getSecWebSocketProtocol());
        }
    }

    @Test
    void shouldSelectBearerProtocolAmongMultipleProtocols() {
        String protocols = "chat.v2, " + this.bearerProtocol(ACCESS_TOKEN) + ", "
            + this.bearerProtocol(QUERY_TOKEN);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN)).thenReturn(1L);

            String clientId = service.getClientId(this.handshake(null, protocols));
            assertEquals(codec.fingerprint(ACCESS_TOKEN), clientId);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer.A", "Bearer.", "chat.v2", ""})
    void shouldFallBackToQueryWhenNoBearerTokenDecodes(String protocol) {
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(QUERY_TOKEN)).thenReturn(2L);

            String clientId = service.getClientId(this.handshake(QUERY_TOKEN, protocol));
            assertEquals(codec.fingerprint(QUERY_TOKEN), clientId);
            assertEquals(QUERY_TOKEN, registry.resolve(clientId));
        }
    }

    @Test
    void shouldSkipMalformedBearerProtocolAndUseNextValidOne() {
        String protocols = "Bearer.A, " + this.bearerProtocol(ACCESS_TOKEN);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN)).thenReturn(1L);

            String clientId = service.getClientId(this.handshake(null, protocols));
            assertEquals(codec.fingerprint(ACCESS_TOKEN), clientId);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer.A", "chat.v2"})
    void shouldRejectMalformedProtocolWithoutQueryToken(String protocol) {
        ServletServerHttpRequest request = this.handshake(null, protocol);
        assertThrows(BusinessException.class, () -> service.getClientId(request));
        verify(registry, never()).register(any(), any());
    }

    @Test
    void shouldRejectInvalidQueryTokenWhenNoBearerTokenDecodes() {
        ServletServerHttpRequest request = this.handshake(QUERY_TOKEN, "Bearer.A");
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            assertThrows(BusinessException.class, () -> service.getClientId(request));
            stpUtil.verify(() -> StpUtil.getLoginIdByToken(QUERY_TOKEN));
            verify(registry, never()).register(any(), any());
        }
    }

    @Test
    void shouldRejectUnknownProtocolTokenDespiteValidAlternatives() {
        ServletServerHttpRequest request = this.handshake(QUERY_TOKEN,
            this.bearerProtocol(ACCESS_TOKEN), this.bearerProtocol(QUERY_TOKEN));
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(QUERY_TOKEN)).thenReturn(2L);

            assertThrows(BusinessException.class, () -> service.getClientId(request));
            stpUtil.verify(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN));
            stpUtil.verifyNoMoreInteractions();
            verifyNoInteractions(validator);
            verify(registry, never()).register(any(), any());
        }
    }

    @Test
    void shouldRejectRevokedProtocolTokenDespiteValidQueryToken() {
        ServletServerHttpRequest request = this.handshake(QUERY_TOKEN,
            this.bearerProtocol(ACCESS_TOKEN));
        when(validator.isInvalid(ACCESS_TOKEN)).thenReturn(true);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken(ACCESS_TOKEN)).thenReturn(1L);
            stpUtil.when(() -> StpUtil.getLoginIdByToken(QUERY_TOKEN)).thenReturn(2L);

            assertThrows(BusinessException.class, () -> service.getClientId(request));
            verify(validator).isInvalid(ACCESS_TOKEN);
            stpUtil.verify(() -> StpUtil.getLoginIdByToken(QUERY_TOKEN), never());
            verify(registry, never()).register(any(), any());
        }
    }

    private RefreshTokenCodec codec() {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setSecret(SECRET);
        return new RefreshTokenCodec(properties);
    }

    private ServletServerHttpRequest handshake(String queryToken, String... protocolHeaders) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (queryToken != null) {
            request.setParameter("token", queryToken);
        }
        for (String header : protocolHeaders) {
            request.addHeader(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, header);
        }
        return new ServletServerHttpRequest(request);
    }

    private String bearerProtocol(String rawToken) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
        return "Bearer." + encoded;
    }
}
