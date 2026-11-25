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

package top.continew.starter.auth.refresh.token.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import top.continew.starter.auth.refresh.token.api.AuthSessionConstants;
import top.continew.starter.auth.refresh.token.service.RefreshTokenService;
import top.continew.starter.auth.refresh.token.support.AccessTokenClaimsReader;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.auth.refresh.token.websocket.AuthWebSocketHandler;
import top.continew.starter.auth.refresh.token.websocket.WebSocketClientServiceImpl;
import top.continew.starter.auth.refresh.token.websocket.WebSocketCredentialRegistry;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * WebSocket 握手协议真实端口集成测试。
 *
 * <p>嵌入式 Tomcat 与 JDK WebSocket 客户端执行真实协议升级；Redis 支持的 Refresh Session
 * 校验由测试替身提供，Sa-Token JWT 签发、claims 读取及 WebSocket 生产链路保持真实。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("Refresh Token WebSocket 握手协议真实端口")
@SpringBootTest(
    classes = WebSocketHandshakeProtocolIT.WebSocketTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "server.address=127.0.0.1",
        "spring.application.name=websocket-handshake-it",
        "continew-starter.refresh-token.secret=local-refresh-secret-for-websocket-it",
        "continew-starter.refresh-token.access-session-cache-enabled=false",
        "continew-starter.refresh-token.cookie-insecure-allowed=true"
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebSocketHandshakeProtocolIT {

    private static final String AUTH_SUBPROTOCOL = "continew.auth.v1";
    private static final String BEARER_SUBPROTOCOL_PREFIX = "Bearer.";
    private static final String SUBPROTOCOL_HEADER_PREFIX = "Sec-WebSocket-Protocol:";
    private static final String JWT_SECRET = "local-jwt-secret-for-websocket-handshake-it";
    private static final int MAX_RESPONSE_HEADER_BYTES = 16 * 1024;
    private static final long LOGIN_ID = 735184L;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REGISTRATION_TIMEOUT = Duration.ofSeconds(5);

    private HttpClient httpClient;
    private final SecureRandom secureRandom = new SecureRandom();

    @LocalServerPort
    private int port;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenCodec refreshTokenCodec;

    @Autowired
    private WebSocketCredentialRegistry credentialRegistry;

    @Autowired
    private WebSocketSessionDao sessionDao;

    @Autowired
    private WebSocketClientService webSocketClientService;

    @Autowired
    private AuthWebSocketHandler webSocketHandler;

    private StpLogic previousStpLogic;
    private WebSocket webSocket;

    @BeforeEach
    void configureLocalJwtSession() {
        previousStpLogic = StpUtil.getStpLogic();
        httpClient = HttpClient.newHttpClient();
        SaTokenConfig config = new SaTokenConfig();
        config.setJwtSecretKey(JWT_SECRET);
        StpLogicJwtForSimple jwtLogic = new StpLogicJwtForSimple();
        jwtLogic.setConfig(config);
        StpUtil.setStpLogic(jwtLogic);

        assertThat(webSocketClientService).isExactlyInstanceOf(WebSocketClientServiceImpl.class);
        assertThat(webSocketHandler).isExactlyInstanceOf(AuthWebSocketHandler.class);
        assertThat(webSocketHandler.getSubProtocols()).containsExactly(AUTH_SUBPROTOCOL);
    }

    @AfterEach
    void closeClientAndRestoreSaTokenLogic() throws Exception {
        try {
            if (webSocket != null && !webSocket.isOutputClosed()) {
                try {
                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete")
                        .get(CONNECT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String causeType = cause == null ? "unknown"
                        : cause.getClass().getSimpleName();
                    throw new AssertionError("WebSocket close failed (" + causeType + ")");
                }
            }
        } finally {
            try {
                if (previousStpLogic != null) {
                    StpUtil.setStpLogic(previousStpLogic);
                }
            } finally {
                if (httpClient != null) {
                    httpClient.shutdownNow();
                }
            }
        }
    }

    @Test
    @DisplayName("无 query 的双协议 offer 完成升级且只协商固定认证协议")
    void shouldNegotiateFixedProtocolAndRegisterProtocolToken() throws Exception {
        String accessToken = this.issueAccessToken();
        when(refreshTokenService.isInvalid(accessToken)).thenReturn(false);
        String bearerProtocol = this.bearerProtocol(accessToken);

        webSocket = this.connect(this.endpointUri(), AUTH_SUBPROTOCOL, bearerProtocol);

        String acceptedProtocol = webSocket.getSubprotocol();
        assertThat(AUTH_SUBPROTOCOL.equals(acceptedProtocol)
            && !acceptedProtocol.startsWith(BEARER_SUBPROTOCOL_PREFIX)).isTrue();
        this.assertRegisteredOpenSession(accessToken, List.of(AUTH_SUBPROTOCOL));
    }

    @Test
    @DisplayName("不 offer 子协议时旧 query token 仍可连接且不协商协议")
    void shouldAcceptLegacyQueryTokenWithoutProtocolOffer() throws Exception {
        String accessToken = this.issueAccessToken();
        when(refreshTokenService.isInvalid(accessToken)).thenReturn(false);

        webSocket = this.connect(this.queryUri(accessToken));

        String acceptedProtocol = webSocket.getSubprotocol();
        assertThat(acceptedProtocol == null || acceptedProtocol.isEmpty()).isTrue();
        this.assertRegisteredOpenSession(accessToken, List.of());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("多物理行凭证不进入握手头，固定协议只在声明时协商")
    void shouldStripCredentialsAcrossPhysicalHeaderLines(boolean offerFixed) throws Exception {
        String accessToken = this.issueAccessToken();
        when(refreshTokenService.isInvalid(accessToken)).thenReturn(false);
        String bearerProtocol = this.bearerProtocol(accessToken);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port),
                (int) CONNECT_TIMEOUT.toMillis());
            socket.setSoTimeout((int) HANDSHAKE_TIMEOUT.toMillis());
            String request = this.repeatedHeaderUpgradeRequest(bearerProtocol, offerFixed);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
            socket.getOutputStream().flush();

            String response = this.readResponseHeaders(socket.getInputStream());
            int statusLineEnd = response.indexOf("\r\n");
            String statusLine = statusLineEnd < 0 ? "" : response.substring(0, statusLineEnd);
            boolean switchingProtocols = "HTTP/1.1 101".equals(statusLine)
                || statusLine.startsWith("HTTP/1.1 101 ");
            List<String> protocolHeaders = response.lines()
                .filter(line -> line.regionMatches(true, 0, SUBPROTOCOL_HEADER_PREFIX, 0,
                    SUBPROTOCOL_HEADER_PREFIX.length()))
                .toList();
            List<String> accepted = protocolHeaders.stream()
                .map(line -> line.substring(SUBPROTOCOL_HEADER_PREFIX.length()).trim())
                .toList();
            assertThat(switchingProtocols).isTrue();
            assertThat(accepted).withFailMessage("Unexpected negotiated protocol.")
                .isEqualTo(offerFixed ? List.of(AUTH_SUBPROTOCOL) : List.of());
            assertThat(response.contains(accessToken) || response.contains(bearerProtocol))
                .isFalse();
            this.assertRegisteredOpenSession(accessToken,
                offerFixed ? List.of(AUTH_SUBPROTOCOL, "chat.v2") : List.of());
        }
    }

    private String issueAccessToken() {
        StpLogic stpLogic = StpUtil.getStpLogic();
        String sessionId = UUID.randomUUID().toString();
        SaLoginParameter loginParameter = stpLogic.createSaLoginParameter()
            .setExtra(AuthSessionConstants.SESSION_ID_CLAIM, sessionId);
        String accessToken = stpLogic.createLoginSession(LOGIN_ID, loginParameter);

        assertThat(AccessTokenClaimsReader.resolveLoginIdQuietly(accessToken))
            .isEqualTo(LOGIN_ID);
        assertThat(AccessTokenClaimsReader.resolveSessionIdQuietly(accessToken))
            .isEqualTo(sessionId);
        return accessToken;
    }

    private String bearerProtocol(String accessToken) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(accessToken.getBytes(StandardCharsets.UTF_8));
        return BEARER_SUBPROTOCOL_PREFIX + encoded;
    }

    private URI endpointUri() {
        return URI.create("ws://127.0.0.1:" + port + "/websocket");
    }

    private URI queryUri(String accessToken) {
        String encoded = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        return URI.create("ws://127.0.0.1:" + port + "/websocket?token=" + encoded);
    }

    private String repeatedHeaderUpgradeRequest(String bearerProtocol, boolean offerFixed) {
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        return "GET /websocket HTTP/1.1\r\n"
            + "Host: 127.0.0.1:" + port + "\r\n"
            + "Upgrade: websocket\r\n"
            + "Connection: Upgrade\r\n"
            + "Sec-WebSocket-Key: " + Base64.getEncoder().encodeToString(key) + "\r\n"
            + "Sec-WebSocket-Version: 13\r\n"
            + SUBPROTOCOL_HEADER_PREFIX + " "
            + (offerFixed ? AUTH_SUBPROTOCOL + ", chat.v2" : "Bearer.A") + "\r\n"
            + SUBPROTOCOL_HEADER_PREFIX + " " + bearerProtocol + ", Bearer.\r\n"
            + "Origin: http://127.0.0.1\r\n"
            + "\r\n";
    }

    private String readResponseHeaders(InputStream input) throws IOException {
        StringBuilder responseHeaders = new StringBuilder();
        while (responseHeaders.length() < MAX_RESPONSE_HEADER_BYTES) {
            int value = input.read();
            if (value < 0) {
                throw new IOException("WebSocket response ended before headers completed.");
            }
            responseHeaders.append((char) value);
            int length = responseHeaders.length();
            if (length >= 4 && "\r\n\r\n".equals(responseHeaders.substring(length - 4))) {
                return responseHeaders.toString();
            }
        }
        throw new IOException("WebSocket response headers exceeded the size limit.");
    }

    private WebSocket connect(URI uri, String... protocols)
        throws InterruptedException, TimeoutException {
        WebSocket.Builder builder = httpClient.newWebSocketBuilder()
            .connectTimeout(CONNECT_TIMEOUT);
        if (protocols.length > 0) {
            builder.subprotocols(protocols[0], Arrays.copyOfRange(protocols, 1, protocols.length));
        }
        try {
            return builder.buildAsync(uri, new WebSocket.Listener() {
            }).get(HANDSHAKE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            String causeType = cause == null ? "unknown" : cause.getClass().getSimpleName();
            throw new AssertionError("WebSocket handshake failed (" + causeType + ")");
        }
    }

    private void assertRegisteredOpenSession(String accessToken, List<String> expectedProtocols) {
        await().pollInterval(Duration.ofMillis(100)).atMost(REGISTRATION_TIMEOUT)
            .untilAsserted(() -> {
                String clientId = credentialRegistry.findClientIdByToken(accessToken);
                assertThat(clientId).isNotNull();
                assertThat(clientId).withFailMessage("Client ID must not be the raw access token.")
                    .isNotEqualTo(accessToken);
                assertThat(clientId).withFailMessage("Client ID must match the token fingerprint.")
                    .isEqualTo(refreshTokenCodec.fingerprint(accessToken));
                assertThat(credentialRegistry.resolve(clientId))
                    .withFailMessage("Registered credential must match the issued access token.")
                    .isEqualTo(accessToken);

                WebSocketSession serverSession = sessionDao.get(clientId);
                assertThat(serverSession).isNotNull();
                assertThat(serverSession.isOpen()).isTrue();
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders(serverSession
                    .getHandshakeHeaders());
                assertThat(headers.getSecWebSocketProtocol())
                    .withFailMessage("Unexpected protocols in Spring handshake headers.")
                    .isEqualTo(expectedProtocols);
                assertThat(headers.containsHeader(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL))
                    .isEqualTo(!expectedProtocols.isEmpty());
            });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class WebSocketTestApplication {

        @Bean
        RedissonClient redissonClient() {
            RedissonClient redissonClient = mock(RedissonClient.class);
            RTopic revocationTopic = mock(RTopic.class);
            when(redissonClient.getTopic(anyString())).thenReturn(revocationTopic);
            return redissonClient;
        }

        @Bean
        RefreshTokenService refreshTokenService() {
            RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
            when(refreshTokenService.isInvalid(anyString())).thenReturn(true);
            return refreshTokenService;
        }
    }
}
