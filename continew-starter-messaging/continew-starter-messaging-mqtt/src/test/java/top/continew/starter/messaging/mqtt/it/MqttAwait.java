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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.awaitility.Awaitility;
import top.continew.starter.messaging.mqtt.model.MqttMessage;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MQTT 异步到达等待工具：统一「轮询 + 超时限定 + 主线程终态断言」口径，不用 sleep。
 *
 * <p>负向等待的成立前提由同一测试内的正向投递保证（管道活性已被先证），超时即失败、
 * 不静默吞掉。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
final class MqttAwait {

    private MqttAwait() {
    }

    /**
     * 等待监听器收到指定 payload 的消息并返回该消息。
     *
     * @param listener 订阅监听器
     * @param payload  消息负载
     * @return 实际到达的消息
     */
    static MqttMessage awaitReceived(CollectingListener listener, String payload) {
        AtomicReference<MqttMessage> matched = new AtomicReference<>();
        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                Optional<MqttMessage> found = listener.byPayload(payload);
                assertTrue(found.isPresent(), () -> "应在时限内收到 payload=" + payload
                    + " 的消息，实际到达 " + listener.received().size() + " 条");
                matched.set(found.get());
            });
        return matched.get();
    }

    /**
     * 等待监听器收到指定 topic 上的保留消息（retained 标记必须为 true）。
     *
     * @param listener 订阅监听器
     * @param topic    保留消息 topic
     * @param payload  消息负载
     * @return 实际到达的保留消息
     */
    static MqttMessage awaitRetained(CollectingListener listener, String topic, String payload) {
        AtomicReference<MqttMessage> matched = new AtomicReference<>();
        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> {
                Optional<MqttMessage> found = findRetained(listener, topic, payload);
                found.ifPresent(matched::set);
                return found.isPresent();
            });
        return matched.get();
    }

    /**
     * 负向等待：静置窗口期内指定 payload 始终未到达该监听器。
     *
     * @param listener 订阅监听器
     * @param payload  消息负载
     */
    static void awaitNotReceived(CollectingListener listener, String payload) {
        Awaitility.await()
            .pollInterval(Duration.ofMillis(100))
            .during(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(4))
            .until(() -> listener.byPayload(payload).isEmpty());
    }

    private static Optional<MqttMessage> findRetained(CollectingListener listener, String topic,
        String payload) {
        return listener.received()
            .stream()
            .filter(message -> Boolean.TRUE.equals(message.getRetained()))
            .filter(message -> topic.equals(message.getTopic()))
            .filter(message -> payload.equals(message.getPayload()))
            .findFirst();
    }
}
