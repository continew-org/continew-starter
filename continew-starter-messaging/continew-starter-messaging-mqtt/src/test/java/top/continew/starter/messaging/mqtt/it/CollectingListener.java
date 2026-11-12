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

import top.continew.starter.messaging.mqtt.model.MqttMessage;
import top.continew.starter.messaging.mqtt.msg.MqttMessageConsumer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 收集型订阅监听器：记录真实回调收到的全部消息，供测试按负载检索。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CollectingListener implements MqttMessageConsumer {

    private final List<MqttMessage> received = new CopyOnWriteArrayList<>();

    @Override
    public void onMessage(MqttMessage message) {
        received.add(message);
    }

    /**
     * 获取已收到的全部消息。
     *
     * @return 消息列表（到达顺序）
     */
    List<MqttMessage> received() {
        return received;
    }

    /**
     * 按 payload 精确检索第一条匹配消息。
     *
     * @param payload 消息负载
     * @return 匹配消息
     */
    Optional<MqttMessage> byPayload(String payload) {
        return received.stream().filter(message -> payload.equals(message.getPayload()))
            .findFirst();
    }
}
