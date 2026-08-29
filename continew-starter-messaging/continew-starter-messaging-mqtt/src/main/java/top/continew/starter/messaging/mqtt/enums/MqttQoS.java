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

package top.continew.starter.messaging.mqtt.enums;

import top.continew.starter.messaging.mqtt.exception.MqttException;

/**
 * qos 消息质量等级枚举
 *
 * @author echo
 * @since 2.15.0
 */
public enum MqttQoS {

    /**
     * QoS level 0 至多发送一次，发送即丢弃。没有确认消息，也不知道对方是否收到。
     */
    AT_MOST_ONCE(0),
    /**
     * QoS level 1 至少一次，都要在可变头部中附加一个16位的消息ID，SUBSCRIBE 和 UNSUBSCRIBE 消息使用 QoS level 1。
     */
    AT_LEAST_ONCE(1),
    /**
     * QoS level 2 确保只有一次，仅仅在 PUBLISH 类型消息中出现，要求在可变头部中要附加消息ID。
     */
    EXACTLY_ONCE(2),
    /**
     * 失败
     */
    FAILURE(0x80);

    private final int value;

    MqttQoS(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    /**
     * 根据 QoS 等级值获取枚举
     *
     * @param value QoS 等级值
     * @return MqttQoS 枚举
     */
    public static MqttQoS valueOf(int value) {
        return switch (value) {
            case 0 -> AT_MOST_ONCE;
            case 1 -> AT_LEAST_ONCE;
            case 2 -> EXACTLY_ONCE;
            case 0x80 -> FAILURE;
            default -> throw new MqttException("无效的 QoS: " + value);
        };
    }

    @Override
    public String toString() {
        return "QoS" + value;
    }
}
