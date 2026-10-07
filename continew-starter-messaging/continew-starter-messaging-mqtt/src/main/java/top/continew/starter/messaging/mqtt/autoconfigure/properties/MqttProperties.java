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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import top.continew.starter.core.constant.PropertiesConstants;

import java.util.Properties;
import javax.net.ssl.HostnameVerifier;

/**
 * 配置参数
 *
 * @author echo
 * @since 2.15.0
 */
@ConfigurationProperties(prefix = PropertiesConstants.MESSAGING_MQTT)
public class MqttProperties {

    /**
     * 开关
     */
    private boolean enabled;

    /**
     * 地址 格式 tcp://192.168.20.95:1883
     */
    private String host;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 保持连接的间隔时间（秒）。客户端会按照此间隔向服务器发送心跳，以维持连接。
     * 默认值：60 秒。
     */
    private Integer keepAliveInterval = 60;

    /**
     * 客户端允许同时存在的最大未确认消息数。
     * 如果超出此数量，新的消息将被阻塞直到有确认消息返回。
     * 默认值：10。
     */
    private Integer maxInflight = 10;

    /**
     * 遗嘱消息内容。客户端异常断开连接时，由服务器向指定主题发送的消息。
     * 可设置 topic payload、QOS、retained 等属性。
     */
    @NestedConfigurationProperty
    private MqttWillProperties will;

    /**
     * 配置 SSL 连接所需的客户端属性。
     * 例如：证书路径、密钥密码等。
     */
    @NestedConfigurationProperty
    private Properties sslClientProps;

    /**
     * 是否启用 HTTPS 主机名验证。
     * 若为 true，将校验服务端证书中的主机名是否与实际连接地址一致。
     * 默认值：false。
     */
    private Boolean httpsHostnameVerificationEnabled = false;

    /**
     * 自定义的主机名校验器，用于验证 SSL/TLS 连接时服务端主机名是否合法。
     * 可用于替换默认的验证策略。
     */
    private HostnameVerifier sslHostnameVerifier;

    /**
     * 是否使用清洁会话。
     * true 表示连接建立时清除之前的会话信息（订阅、未送达消息等），
     * false 表示会话持久化。
     * 默认值：false（持久会话）。
     */
    private Boolean cleanSession = false;

    /**
     * 连接超时时间（秒）。客户端尝试连接服务器的最长等待时间。
     * 默认值：30 秒。
     */
    private Integer connectionTimeout = 30;

    /**
     * 是否启用自动重连。当连接断开时，是否自动尝试重新连接。
     * 默认值：true。
     */
    private Boolean automaticReconnect = true;

    /**
     * 最大重连延迟时间（毫秒）。用于自动重连时的退避策略上限。
     * 默认值：128000（约 2 分钟）。
     */
    private Integer maxReconnectDelay = 128000;

    /**
     * 自定义 WebSocket 请求头。
     * 用于配置使用 WebSocket 协议连接时的额外 HTTP 请求头参数。
     */
    @NestedConfigurationProperty
    private Properties customWebSocketHeaders;

    /**
     * 终止执行服务时等待多长时间（以秒为单位）
     */
    private Integer executorServiceTimeout = 1;

    /**
     * 生产者
     */
    @NestedConfigurationProperty
    private MqttProducerProperties producer = new MqttProducerProperties();

    /**
     * 消费者
     */
    @NestedConfigurationProperty
    private MqttConsumerProperties consumer = new MqttConsumerProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(Integer keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public Integer getMaxInflight() {
        return maxInflight;
    }

    public void setMaxInflight(Integer maxInflight) {
        this.maxInflight = maxInflight;
    }

    public MqttWillProperties getWill() {
        return will;
    }

    public void setWill(MqttWillProperties will) {
        this.will = will;
    }

    public Properties getSslClientProps() {
        return sslClientProps;
    }

    public void setSslClientProps(Properties sslClientProps) {
        this.sslClientProps = sslClientProps;
    }

    public Boolean getHttpsHostnameVerificationEnabled() {
        return httpsHostnameVerificationEnabled;
    }

    public void setHttpsHostnameVerificationEnabled(Boolean httpsHostnameVerificationEnabled) {
        this.httpsHostnameVerificationEnabled = httpsHostnameVerificationEnabled;
    }

    public HostnameVerifier getSslHostnameVerifier() {
        return sslHostnameVerifier;
    }

    public void setSslHostnameVerifier(HostnameVerifier sslHostnameVerifier) {
        this.sslHostnameVerifier = sslHostnameVerifier;
    }

    public Boolean getCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(Boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Boolean getAutomaticReconnect() {
        return automaticReconnect;
    }

    public void setAutomaticReconnect(Boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public Integer getMaxReconnectDelay() {
        return maxReconnectDelay;
    }

    public void setMaxReconnectDelay(Integer maxReconnectDelay) {
        this.maxReconnectDelay = maxReconnectDelay;
    }

    public Properties getCustomWebSocketHeaders() {
        return customWebSocketHeaders;
    }

    public void setCustomWebSocketHeaders(Properties customWebSocketHeaders) {
        this.customWebSocketHeaders = customWebSocketHeaders;
    }

    public Integer getExecutorServiceTimeout() {
        return executorServiceTimeout;
    }

    public void setExecutorServiceTimeout(Integer executorServiceTimeout) {
        this.executorServiceTimeout = executorServiceTimeout;
    }

    public MqttProducerProperties getProducer() {
        return producer;
    }

    public void setProducer(MqttProducerProperties producer) {
        this.producer = producer;
    }

    public MqttConsumerProperties getConsumer() {
        return consumer;
    }

    public void setConsumer(MqttConsumerProperties consumer) {
        this.consumer = consumer;
    }
}
