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

package top.continew.starter.messaging.mqtt.msg;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import top.continew.starter.messaging.mqtt.constant.MqttConstant;

/**
 * 消息发送 - 生产者
 *
 * @author echo
 * @since 2.15.0
 **/
@MessagingGateway(defaultRequestChannel = MqttConstant.MQTT_OUT_BOUND_CHANNEL_NAME)
public interface MqttMessageProducer {

    /**
     * 消息发送 - 默认topic
     *
     * @param payload 消息体
     */
    void sendToMqtt(String payload);

    /**
     * 指定topic进行消息发送
     *
     * @param topic   topic
     * @param payload 消息体
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Payload String payload);

    /**
     * 指定topic进行消息发送
     *
     * @param topic   topic
     * @param qos     qos
     * @param payload 消息体
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic,
        @Header(MqttHeaders.QOS) int qos,
        @Header(MqttHeaders.RETAINED) boolean retained,
        @Payload String payload);

    /**
     * 指定topic进行消息发送
     *
     * @param topic   topic
     * @param qos     qos
     * @param payload 消息体
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic,
        @Header(MqttHeaders.QOS) int qos,
        @Header(MqttHeaders.RETAINED) boolean retained,
        @Payload byte[] payload);
}
