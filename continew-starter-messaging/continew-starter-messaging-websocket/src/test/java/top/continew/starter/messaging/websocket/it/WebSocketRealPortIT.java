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

package top.continew.starter.messaging.websocket.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;
import top.continew.starter.messaging.websocket.util.WebSocketUtils;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 真实端口端到端链路集成测试
 *
 * <p>内嵌 Tomcat 监听随机端口，标准 WebSocket 客户端发起真实握手与协议升级，覆盖
 * 连接建立 → 会话注册 → 服务端推送真实到达 → 客户端收发 → 断开注销全链路。推送统一
 * 经 {@link WebSocketUtils}，同时覆盖其静态 DAO 绑定在真实容器内的取值路径。</p>
 *
 * <p>消息到达等待统一轮询 + deadline + 终态断言；负向等待的成立前提由同一测试内的
 * 正向投递保证。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("WebSocket 真实端口端到端链路")
@SpringBootTest(classes = WebSocketRealPortIT.WebSocketTestApp.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketRealPortIT {

    private static final Duration RECEIVE_DEADLINE = Duration.ofSeconds(10);
    private static final Duration QUIET_WINDOW = Duration.ofSeconds(2);

    @LocalServerPort
    private int port;

    @Autowired
    private WebSocketSessionDao sessionDao;

    private final StandardWebSocketClient client = new StandardWebSocketClient();

    @Test
    @DisplayName("连接建立后会话注册进 DAO，服务端推送客户端真实收到")
    void shouldRegisterSessionThenDeliverServerPush() throws Exception {
        String clientId = uniqueClientId();
        CollectingWebSocketHandler handler = new CollectingWebSocketHandler();

        WebSocketSession clientSession = connect(clientId, handler);

        awaitRegistration(clientId);
        String payload = "push-" + UUID.randomUUID();
        WebSocketUtils.sendMessage(clientId, payload);

        awaitMessage(handler, payload);
        assertThat(clientSession.isOpen()).isTrue();
    }

    @Test
    @DisplayName("客户端上行消息后链路保持存活，服务端推送仍可到达")
    void shouldKeepLinkAliveAfterClientMessage() throws Exception {
        String clientId = uniqueClientId();
        CollectingWebSocketHandler handler = new CollectingWebSocketHandler();
        WebSocketSession clientSession = connect(clientId, handler);
        awaitRegistration(clientId);

        clientSession.sendMessage(new TextMessage("ping"));

        String payload = "after-uplink-" + UUID.randomUUID();
        WebSocketUtils.sendMessage(clientId, payload);
        awaitMessage(handler, payload);
        assertThat(sessionDao.get(clientId).isOpen()).isTrue();
    }

    @Test
    @DisplayName("客户端断开后会话应从 DAO 注销")
    void shouldUnregisterSessionOnClientDisconnect() throws Exception {
        String clientId = uniqueClientId();
        CollectingWebSocketHandler handler = new CollectingWebSocketHandler();
        WebSocketSession clientSession = connect(clientId, handler);
        awaitRegistration(clientId);

        clientSession.close();

        await().atMost(RECEIVE_DEADLINE)
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> assertThat(sessionDao.get(clientId)).isNull());
    }

    @Test
    @DisplayName("广播应到达每一条在线连接")
    void shouldBroadcastToEveryConnectedClient() throws Exception {
        CollectingWebSocketHandler first = new CollectingWebSocketHandler();
        CollectingWebSocketHandler second = new CollectingWebSocketHandler();
        connect(uniqueClientId(), first);
        connect(uniqueClientId(), second);

        String payload = "broadcast-" + UUID.randomUUID();
        WebSocketUtils.sendMessage(payload);

        awaitMessage(first, payload);
        awaitMessage(second, payload);
    }

    @Test
    @DisplayName("批量发送应只到达目标列表内的连接，列表外连接静置")
    void shouldDeliverBatchOnlyToTargetClients() throws Exception {
        CollectingWebSocketHandler targetOne = new CollectingWebSocketHandler();
        CollectingWebSocketHandler targetTwo = new CollectingWebSocketHandler();
        CollectingWebSocketHandler outOfList = new CollectingWebSocketHandler();
        String clientIdOne = uniqueClientId();
        String clientIdTwo = uniqueClientId();
        connect(clientIdOne, targetOne);
        connect(clientIdTwo, targetTwo);
        connect(uniqueClientId(), outOfList);

        String payload = "batch-" + UUID.randomUUID();
        WebSocketUtils.sendMessage(List.of(clientIdOne, clientIdTwo), payload);

        awaitMessage(targetOne, payload);
        awaitMessage(targetTwo, payload);
        awaitQuiet(outOfList, payload);
    }

    @Test
    @DisplayName("同一客户端 ID 重连应顶掉旧会话，推送只到达新连接")
    void shouldReplaceSessionWhenSameClientIdReconnects() throws Exception {
        // 默认 DAO 为单连接 put 语义：同 Key 二次注册覆盖首次连接，推送仅到达后连者
        String clientId = uniqueClientId();
        CollectingWebSocketHandler oldHandler = new CollectingWebSocketHandler();
        CollectingWebSocketHandler newHandler = new CollectingWebSocketHandler();
        connect(clientId, oldHandler);
        connect(clientId, newHandler);
        awaitRegistration(clientId);

        String payload = "reconnect-" + UUID.randomUUID();
        WebSocketUtils.sendMessage(clientId, payload);

        awaitMessage(newHandler, payload);
        awaitQuiet(oldHandler, payload);
    }

    private WebSocketSession connect(String clientId, CollectingWebSocketHandler handler)
        throws Exception {
        URI uri = URI.create("ws://localhost:" + port + "/websocket?clientId=" + clientId);
        // CompletableFuture.get 以终态等待协议升级完成，deadline 到期即失败
        return client.execute(handler, new WebSocketHttpHeaders(), uri).get(10, TimeUnit.SECONDS);
    }

    private void awaitRegistration(String clientId) {
        await().atMost(RECEIVE_DEADLINE)
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> assertThat(sessionDao.get(clientId)).isNotNull());
    }

    private void awaitMessage(CollectingWebSocketHandler handler, String payload) {
        await().atMost(RECEIVE_DEADLINE)
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> assertThat(handler.getPayloads()).contains(payload));
    }

    private void awaitQuiet(CollectingWebSocketHandler handler, String payload) {
        await().pollInterval(Duration.ofMillis(100))
            .during(QUIET_WINDOW)
            .atMost(Duration.ofSeconds(4))
            .until(() -> !handler.getPayloads().contains(payload));
    }

    private static String uniqueClientId() {
        return "it-" + UUID.randomUUID();
    }

    /**
     * 收集型客户端处理器：真实协议栈回调记录到达的文本负载，供主线程轮询断言。
     */
    static class CollectingWebSocketHandler extends TextWebSocketHandler {

        private final BlockingQueue<String> payloads = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            payloads.add(message.getPayload());
        }

        BlockingQueue<String> getPayloads() {
            return payloads;
        }
    }

    /**
     * WebSocket 测试应用：走真实自动配置装配，clientId 经握手请求参数解析。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class WebSocketTestApp {

        @Bean
        WebSocketClientService webSocketClientService() {
            return request -> request.getServletRequest().getParameter("clientId");
        }
    }
}
