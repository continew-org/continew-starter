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

package top.continew.starter.messaging.mqtt.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.messaging.mqtt.annotation.MqttListener;
import top.continew.starter.messaging.mqtt.model.MqttMessage;
import top.continew.starter.messaging.mqtt.msg.MqttMessageProducer;
import top.continew.starter.messaging.mqtt.strategy.MqttOptions;

import java.util.UUID;

/**
 * MQTT 网关真实 HiveMQ 集成测试：经 {@code MqttMessageProducer} 网关发布、入站适配器
 * 真实订阅回调，验证精确 topic、QoS、通配符过滤、共享订阅与保留消息。
 *
 * <p>发布端与订阅端是两条独立 Paho 连接，监听器收到消息即证明真实 broker 完成
 * 转发，无需 mock 自证；异步到达一律由轮询 + 超时限定等待，断言在主线程终态执行。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("MQTT 网关真实 HiveMQ 集成测试")
@SpringBootTest(classes = MqttGatewayIT.GatewayTestApp.class)
class MqttGatewayIT extends HivemqContainerSupport {

    private static final String EXACT_TOPIC = "it/gateway/exact";
    private static final String WILDCARD_FILTER = "it/gateway/+/data";
    private static final String SHARED_FILTER = "$share/it-group/it/gateway/shared/req";
    private static final String SHARED_TOPIC = "it/gateway/shared/req";

    @Autowired
    private MqttMessageProducer producer;

    @Autowired
    private MqttOptions mqttOptions;

    @Autowired
    @Qualifier("exactTopicListener")
    private CollectingListener exactListener;

    @Autowired
    @Qualifier("wildcardTopicListener")
    private CollectingListener wildcardListener;

    @Autowired
    @Qualifier("sharedSubscriptionListener")
    private CollectingListener sharedListener;

    @Autowired
    @Qualifier("plainListener")
    private CollectingListener plainListener;

    @DynamicPropertySource
    static void mqttProperties(DynamicPropertyRegistry registry) {
        HivemqContainerSupport.ensureStarted();
        registry.add("spring.application.name", () -> "mqtt-it");
        registry.add("continew-starter.messaging.mqtt.enabled", () -> "true");
        registry.add("continew-starter.messaging.mqtt.host", HivemqContainerSupport::brokerUrl);
        registry.add("continew-starter.messaging.mqtt.username", () -> "it-user");
        registry.add("continew-starter.messaging.mqtt.password", () -> "it-pass");
    }

    @Test
    @DisplayName("精确 topic 发布经真实 broker 回转，订阅端按 QoS1 收到一致负载")
    void shouldPublishAndReceiveOnExactTopicWithQos1() {
        String payload = "p-" + UUID.randomUUID();

        producer.sendToMqtt(EXACT_TOPIC, 1, false, payload);

        MqttMessage message = MqttAwait.awaitReceived(exactListener, payload);
        assertEquals(EXACT_TOPIC, message.getTopic());
        assertEquals(1, message.getQos());
        assertFalse(message.getRetained());
    }

    @Test
    @DisplayName("通配符过滤器命中单级通配消息，越层消息与未订阅 topic 均不投递")
    void shouldRouteWildcardMatchAndRejectMismatch() {
        String matchedPayload = "wild-hit-" + UUID.randomUUID();
        String crossedPayload = "wild-cross-" + UUID.randomUUID();
        String unsubscribedPayload = "wild-silent-" + UUID.randomUUID();

        producer.sendToMqtt("it/gateway/room1/data", 0, false, matchedPayload);

        MqttMessage message = MqttAwait.awaitReceived(wildcardListener, matchedPayload);
        assertEquals("it/gateway/room1/data", message.getTopic());

        // 越层消息：+/data 只吃一级通配，多出的层级不该命中通配过滤器
        producer.sendToMqtt("it/gateway/room1/data/extra", 0, false, crossedPayload);
        // 未被任何订阅覆盖的 topic：broker 无转发出路，入站分发同样按未订阅短路
        producer.sendToMqtt("it/gateway/nowhere", 0, false, unsubscribedPayload);
        MqttAwait.awaitNotReceived(wildcardListener, crossedPayload);
        MqttAwait.awaitNotReceived(wildcardListener, unsubscribedPayload);
        MqttAwait.awaitNotReceived(plainListener, crossedPayload);
    }

    @Test
    @DisplayName("$share 共享订阅组收到转发消息，topic 与 QoS 与发布声明一致")
    void shouldDeliverThroughSharedSubscriptionGroup() {
        String payload = "shared-" + UUID.randomUUID();

        producer.sendToMqtt(SHARED_TOPIC, 1, false, payload);

        MqttMessage message = MqttAwait.awaitReceived(sharedListener, payload);
        assertEquals(SHARED_TOPIC, message.getTopic());
        assertEquals(1, message.getQos());
    }

    @Test
    @DisplayName("保留消息在订阅建立前发布，后订阅端立即收到并携带 retained 标记")
    void shouldDeliverRetainedMessageOnLateSubscription() {
        String retainedTopic = "it/retained/" + UUID.randomUUID().toString().replace("-", "");
        String payload = "retained-" + UUID.randomUUID();

        // 发布时无人订阅该 topic，broker 落保留消息；随后动态订阅立即触发保留投递
        producer.sendToMqtt(retainedTopic, 1, true, payload);
        mqttOptions.addTopic(retainedTopic, 1);

        MqttMessage message = MqttAwait.awaitRetained(plainListener, retainedTopic, payload);
        assertEquals(retainedTopic, message.getTopic());
        assertEquals(payload, message.getPayload());
        assertTrue(message.getRetained());

        // 发布 0 字节保留消息清除 broker 残留，避免污染后续轮次
        producer.sendToMqtt(retainedTopic, 1, true, "");
    }

    /**
     * 网关测试应用：三个注解订阅监听器覆盖精确/通配符/共享订阅，另备一个无注解监听器
     * 承接动态订阅广播。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class GatewayTestApp {

        @Bean
        ExactTopicListener exactTopicListener() {
            return new ExactTopicListener();
        }

        @Bean
        WildcardTopicListener wildcardTopicListener() {
            return new WildcardTopicListener();
        }

        @Bean
        SharedSubscriptionListener sharedSubscriptionListener() {
            return new SharedSubscriptionListener();
        }

        @Bean
        CollectingListener plainListener() {
            return new CollectingListener();
        }
    }

    /**
     * 精确 topic 订阅（QoS1）。
     */
    @MqttListener(topic = EXACT_TOPIC, qos = "1")
    static class ExactTopicListener extends CollectingListener {
    }

    /**
     * 单级通配符订阅（默认 QoS0）。
     */
    @MqttListener(topic = WILDCARD_FILTER)
    static class WildcardTopicListener extends CollectingListener {
    }

    /**
     * 带群组共享订阅：过滤器以 $share 前缀登记，转发后的真实 topic 为去前缀形式。
     */
    @MqttListener(topic = SHARED_FILTER, qos = "1")
    static class SharedSubscriptionListener extends CollectingListener {
    }
}
