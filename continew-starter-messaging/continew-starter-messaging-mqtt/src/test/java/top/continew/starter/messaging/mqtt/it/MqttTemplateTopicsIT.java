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
import top.continew.starter.messaging.mqtt.model.MqttMessage;
import top.continew.starter.messaging.mqtt.msg.MqttMessageProducer;
import top.continew.starter.messaging.mqtt.strategy.MqttOptions;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link MqttOptions}（MqttTemplate）真实 HiveMQ 集成测试：动态订阅/退订、topic 清点与
 * 广播投递经真实 broker 验证。
 *
 * <p>独立上下文刻意不放任何 @MqttListener 监听器：适配器初始零订阅，动态增删与
 * clearAllTopics 的行为可全量观察，也不会误清注解订阅。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("MqttTemplate topic 管理真实 HiveMQ 集成测试")
@SpringBootTest(classes = MqttTemplateTopicsIT.TopicsTestApp.class)
class MqttTemplateTopicsIT extends HivemqContainerSupport {

    @Autowired
    private MqttOptions mqttOptions;

    @Autowired
    private MqttMessageProducer producer;

    @Autowired
    @Qualifier("dynamicTopicListener")
    private CollectingListener dynamicListener;

    @DynamicPropertySource
    static void mqttProperties(DynamicPropertyRegistry registry) {
        HivemqContainerSupport.ensureStarted();
        registry.add("spring.application.name", () -> "mqtt-it-topics");
        registry.add("continew-starter.messaging.mqtt.enabled", () -> "true");
        registry.add("continew-starter.messaging.mqtt.host", HivemqContainerSupport::brokerUrl);
        registry.add("continew-starter.messaging.mqtt.username", () -> "it-user");
        registry.add("continew-starter.messaging.mqtt.password", () -> "it-pass");
    }

    @Test
    @DisplayName("动态订阅后广播链路投递消息，退订后同 topic 不再投递")
    void shouldBroadcastAfterDynamicSubscribeAndStopAfterUnsubscribe() {
        String filter = "it/dynamic/" + UUID.randomUUID() + "/#";
        String publishTopic = filter.substring(0, filter.length() - 2) + "/leaf";
        String firstPayload = "dyn-hit-" + UUID.randomUUID();
        String secondPayload = "dyn-silent-" + UUID.randomUUID();

        mqttOptions.addTopic(filter, 1);
        assertTrue(mqttOptions.containsTopic(filter));

        producer.sendToMqtt(publishTopic, 1, false, firstPayload);
        MqttMessage message = MqttAwait.awaitReceived(dynamicListener, firstPayload);
        assertEquals(publishTopic, message.getTopic());
        assertEquals(1, message.getQos());

        mqttOptions.removeTopic(filter);
        assertFalse(mqttOptions.containsTopic(filter));

        producer.sendToMqtt(publishTopic, 1, false, secondPayload);
        MqttAwait.awaitNotReceived(dynamicListener, secondPayload);
    }

    @Test
    @DisplayName("多 topic 增删各重载生效：清点数量与 containsTopic 反映真实订阅集合")
    void shouldManageMultipleTopicsWithAllOverloads() {
        String base = "it/mgmt/" + UUID.randomUUID();
        String topicA = base + "/a";
        String topicB = base + "/b";
        String topicC = base + "/c";
        int before = mqttOptions.getTopicCount();

        mqttOptions.addTopic(topicA);
        mqttOptions.addTopics(1, topicB, topicC);
        assertEquals(before + 3, mqttOptions.getTopicCount());
        assertTrue(mqttOptions.containsTopic(topicA));
        assertTrue(mqttOptions.containsTopic(topicB));
        assertTrue(mqttOptions.containsTopic(topicC));

        mqttOptions.addTopics(List.of(topicA + "/sub", topicB + "/sub"));
        assertEquals(before + 5, mqttOptions.getTopicCount());

        mqttOptions.removeTopics(List.of(topicA, topicB));
        assertEquals(before + 3, mqttOptions.getTopicCount());
        assertFalse(mqttOptions.containsTopic(topicA));

        mqttOptions.removeTopics(Set.of(topicC, topicA + "/sub", topicB + "/sub"));
        assertEquals(before, mqttOptions.getTopicCount());
        assertFalse(mqttOptions.containsTopic(topicC));
    }

    @Test
    @DisplayName("clearAllTopics 清空全部动态订阅并使清点归零")
    void shouldClearAllTopics() {
        String base = "it/clear/" + UUID.randomUUID();
        mqttOptions.addTopics(1, base + "/x", base + "/y");
        assertTrue(mqttOptions.getTopicCount() >= 2);

        mqttOptions.clearAllTopics();

        assertTrue(mqttOptions.listTopics().isEmpty());
        assertEquals(0, mqttOptions.getTopicCount());
        assertFalse(mqttOptions.containsTopic(base + "/x"));
    }

    /**
     * topic 管理测试应用：仅保留一个无注解监听器承接广播。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TopicsTestApp {

        @Bean
        CollectingListener dynamicTopicListener() {
            return new CollectingListener();
        }
    }
}
