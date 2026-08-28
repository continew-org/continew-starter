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

package top.continew.starter.messaging.mqtt.autoconfigure.properties;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.integration.mqtt.core.ClientManager;
import top.continew.starter.messaging.mqtt.enums.MqttQoS;

/**
 * 消费者属性
 *
 * @author echo
 * @since 2.15.0
 */
public class MqttConsumerProperties {

    /**
     * MQTT 服务质量等级（QoS, Quality of Service）。
     * 0：最多一次（AT_MOST_ONCE），不保证消息到达；
     * 1：至少一次（AT_LEAST_ONCE），可能重复；
     * 2：只有一次（EXACTLY_ONCE），确保消息仅到达一次。
     * 默认使用 QoS 0。
     */
    private Integer qos = MqttQoS.AT_MOST_ONCE.value();

    /**
     * 消息发送完成等待超时时间（单位：毫秒）。
     * 控制发送消息时，等待 broker 响应的最大时长，超过将报错。
     * 默认值参考 ClientManager.DEFAULT_COMPLETION_TIMEOUT。
     */
    private Long completionTimeout = ClientManager.DEFAULT_COMPLETION_TIMEOUT;

    /**
     * 是否自动启动客户端连接。
     * 设置为 true 则在应用启动时自动连接 MQTT 服务器并订阅 Topic。
     * 默认值为 true。
     */
    private Boolean autoStartUp = true;

    /**
     * MQTT 客户端 ID。
     * 用于唯一标识客户端连接，同一 broker 下不能重复。
     * 如果为空，可能由系统自动生成。
     */
    private String clientId;

    /**
     * 是否启用异步消息发送。
     * 设置为 true 则消息发送不阻塞当前线程，适用于高吞吐场景；
     * 设置为 false 则同步发送，便于确认是否成功。
     * 默认值为 false。
     */
    private Boolean async = false;

    /**
     * 客户端断开连接时的完成超时时间（单位：毫秒）。
     * 用于控制断连操作的最长等待时间。
     * 默认值参考 ClientManager.DISCONNECT_COMPLETION_TIMEOUT。
     */
    private Long disconnectCompletionTimeout = ClientManager.DISCONNECT_COMPLETION_TIMEOUT;

    /**
     * MQTT 消息处理线程池配置。
     * 包含核心线程数、最大线程数、队列容量等，控制消费者消息处理能力。
     * 默认配置为 new MqttExecutorProperties()。
     */
    @NestedConfigurationProperty
    private MqttExecutorProperties executor = new MqttExecutorProperties();

    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    public Long getCompletionTimeout() {
        return completionTimeout;
    }

    public void setCompletionTimeout(Long completionTimeout) {
        this.completionTimeout = completionTimeout;
    }

    public Boolean getAutoStartUp() {
        return autoStartUp;
    }

    public void setAutoStartUp(Boolean autoStartUp) {
        this.autoStartUp = autoStartUp;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public Long getDisconnectCompletionTimeout() {
        return disconnectCompletionTimeout;
    }

    public void setDisconnectCompletionTimeout(Long disconnectCompletionTimeout) {
        this.disconnectCompletionTimeout = disconnectCompletionTimeout;
    }

    public MqttExecutorProperties getExecutor() {
        return executor;
    }

    public void setExecutor(MqttExecutorProperties executor) {
        this.executor = executor;
    }
}
