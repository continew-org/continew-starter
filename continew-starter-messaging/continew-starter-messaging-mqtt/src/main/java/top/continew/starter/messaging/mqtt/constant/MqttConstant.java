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

package top.continew.starter.messaging.mqtt.constant;

/**
 * mqtt常量
 *
 * @author echo
 * @since 2.15.0
 */
public class MqttConstant {

    private MqttConstant() {
    }

    /**
     * MQTT 入站通道名称（消费者使用，接收消息的入口）
     */
    public static final String MQTT_INPUT_CHANNEL_NAME = "mqttInputChannel";

    /**
     * MQTT 出站通道名称（生产者使用，发送消息的出口）
     */
    public static final String MQTT_OUT_BOUND_CHANNEL_NAME = "mqttOutboundChannel";

    /**
     * 应用级消息消费处理通道名称（接收到 MQTT 消息后转发到此通道供业务处理）
     */
    public static final String CONSUMER_CHANNEL_NAME = "consumerChannel";

}
