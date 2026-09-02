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

package top.continew.starter.messaging.mqtt.model;

import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import top.continew.starter.messaging.mqtt.enums.MqttQoS;

import java.io.Serializable;
import java.util.Objects;

/**
 * mqtt消息实体
 *
 * @author echo
 * @since 2.15.0
 */
// 消息仅在 Spring Integration 管道内传递，payload 为任意业务对象，不会走 Java 原生序列化
@SuppressWarnings("java:S1948")
public class MqttMessage implements Serializable {

    /**
     * 消息头信息，包含 MQTT 消息的元数据，如 topic、QOS、retained 等。
     */
    private MessageHeaders messageHeaders;

    /**
     * 消息体（负载），可以是字符串、字节数组或其他对象类型。
     */
    private Object payload;

    /**
     * MQTT 主题，用于标识消息的发布/订阅通道。
     */
    private String topic;

    /**
     * 消息服务质量等级（QOS）：
     * 0 - 最多一次，消息可能会丢失；
     * 1 - 至少一次，消息可能重复；
     * 2 - 只有一次，确保消息不重复也不丢失。
     */
    private Integer qos;

    /**
     * 是否保留该消息（Retained）：
     * true 表示该消息会保留在 MQTT 服务器上，供新订阅者立即获取；
     * false 表示仅当前订阅者接收到该消息。
     */
    private Boolean retained;

    public MqttMessage(Message<?> message) {
        this.messageHeaders = message.getHeaders();
        this.topic =
            (String) Objects.requireNonNull(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        this.qos =
            (Integer) Objects.requireNonNull(message.getHeaders().get(MqttHeaders.RECEIVED_QOS));
        this.retained = (Boolean) Objects
            .requireNonNull(message.getHeaders().get(MqttHeaders.RECEIVED_RETAINED));
        this.payload = message.getPayload();
    }

    public MqttMessage(Object payload, String topic) {
        this.payload = payload;
        this.topic = topic;
        this.qos = MqttQoS.AT_MOST_ONCE.value();
        this.retained = false;
    }

    public MqttMessage(Object payload, String topic, Integer qos) {
        this.payload = payload;
        this.topic = topic;
        this.qos = qos;
        this.retained = false;
    }

    public MqttMessage(Object payload, String topic, Integer qos, Boolean retained) {
        this.payload = payload;
        this.topic = topic;
        this.qos = qos;
        this.retained = retained;
    }

    public static MqttMessage of(Message<?> message) {
        return new MqttMessage(message);
    }

    public static MqttMessage of(Object payload, String topic, Integer qos) {
        return new MqttMessage(payload, topic, qos);
    }

    public MessageHeaders getMessageHeaders() {
        return messageHeaders;
    }

    public void setMessageHeaders(MessageHeaders messageHeaders) {
        this.messageHeaders = messageHeaders;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    public Boolean getRetained() {
        return retained;
    }

    public void setRetained(Boolean retained) {
        this.retained = retained;
    }
}
