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

package top.continew.starter.messaging.mqtt.autoconfigure;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.messaging.mqtt.autoconfigure.properties.MqttConsumerProperties;
import top.continew.starter.messaging.mqtt.autoconfigure.properties.MqttExecutorProperties;
import top.continew.starter.messaging.mqtt.autoconfigure.properties.MqttProducerProperties;
import top.continew.starter.messaging.mqtt.autoconfigure.properties.MqttProperties;
import top.continew.starter.messaging.mqtt.autoconfigure.properties.MqttWillProperties;
import top.continew.starter.messaging.mqtt.constant.MqttConstant;
import top.continew.starter.messaging.mqtt.handler.MqttMessageInboundHandler;
import top.continew.starter.messaging.mqtt.handler.MqttShutdownHandler;
import top.continew.starter.messaging.mqtt.msg.MqttMessageConsumer;
import top.continew.starter.messaging.mqtt.msg.MqttMessageProducer;
import top.continew.starter.messaging.mqtt.strategy.MqttOptions;
import top.continew.starter.messaging.mqtt.strategy.MqttTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * MQTT 自动配置类
 * <p>
 * 用于配置 MQTT 连接参数、入站/出站通道及消息处理器等组件。
 * </p>
 *
 * @author echo
 * @since 2.15.0
 */
@AutoConfiguration
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.MESSAGING_MQTT,
    value = PropertiesConstants.ENABLED, havingValue = "true")
public class MqttAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttAutoConfiguration.class);

    private final MqttProperties mqttProperties;

    public MqttAutoConfiguration(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
    }

    /**
     * 配置 MQTT 客户端连接选项
     * <p>
     * 该方法创建并配置 {@link MqttConnectOptions} 实例，用于建立 MQTT 客户端与服务器的连接。
     * 包含认证信息、连接参数、重连策略、SSL/TLS 配置以及遗嘱消息等完整配置。
     * </p>
     *
     * @return 配置完成的 MQTT 连接选项对象
     * @see MqttConnectOptions
     * @see MqttProperties
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

        // 设置 MQTT 服务器地址，支持 tcp://host:port 或 ssl://host:port 格式
        mqttConnectOptions.setServerURIs(new String[] {mqttProperties.getHost()});

        // 设置用户名（用于认证）
        mqttConnectOptions.setUserName(mqttProperties.getUsername());

        // 设置密码（转为 char[] 以增强安全性）
        mqttConnectOptions.setPassword(mqttProperties.getPassword().toCharArray());

        // 设置连接超时时间（单位：秒），默认 30 秒
        mqttConnectOptions.setConnectionTimeout(mqttProperties.getConnectionTimeout());

        // 设置心跳间隔（单位：秒），用于维持长连接，默认 60 秒
        mqttConnectOptions.setKeepAliveInterval(mqttProperties.getKeepAliveInterval());

        // 设置是否清除会话
        // true：每次连接时清除上次会话状态
        // false：保留订阅信息与未确认消息
        mqttConnectOptions.setCleanSession(mqttProperties.getCleanSession());

        // 启用自动重连机制，默认 true
        mqttConnectOptions.setAutomaticReconnect(mqttProperties.getAutomaticReconnect());

        // 设置最大重连延迟（单位：毫秒），防止频繁重连，默认 128000 毫秒
        mqttConnectOptions.setMaxReconnectDelay(mqttProperties.getMaxReconnectDelay());

        // 设置最大允许未确认的 QoS>0 消息数量，控制并发发送能力，默认 10
        mqttConnectOptions.setMaxInflight(mqttProperties.getMaxInflight());

        // 设置自定义 WebSocket 请求头（仅在使用 ws:// 或 wss:// 协议时生效）
        mqttConnectOptions.setCustomWebSocketHeaders(mqttProperties.getCustomWebSocketHeaders());

        // 启用 HTTPS 主机名验证（适用于 SSL/TLS）
        mqttConnectOptions.setHttpsHostnameVerificationEnabled(
            mqttProperties.getHttpsHostnameVerificationEnabled());

        // 设置 SSL 连接所需的客户端属性，如证书、密钥、信任库等
        mqttConnectOptions.setSSLProperties(mqttProperties.getSslClientProps());

        // 设置关闭 ExecutorService 的超时时间（单位：秒），默认 1 秒
        mqttConnectOptions.setExecutorServiceTimeout(mqttProperties.getExecutorServiceTimeout());

        // 设置遗嘱消息（当客户端异常断开连接时由服务器自动发送）
        MqttWillProperties will = mqttProperties.getWill();
        if (ObjectUtil.isNotEmpty(will)) {
            // 设置遗嘱主题
            // 设置遗嘱消息内容（字节数组）
            // 设置 QoS 等级
            // 设置是否保留该消息（true 表示新订阅者会收到）
            mqttConnectOptions.setWill(will.getTopic(), will.getPayload()
                .getBytes(StandardCharsets.UTF_8), will.getQos(),
                will
                    .getRetained());
        }

        return mqttConnectOptions;
    }

    /**
     * 配置 MQTT 客户端工厂，关联连接参数。
     */
    @Bean
    public MqttPahoClientFactory mqttPahoClientFactory(MqttConnectOptions mqttConnectOptions) {
        DefaultMqttPahoClientFactory clientFactory = new DefaultMqttPahoClientFactory();
        clientFactory.setConnectionOptions(mqttConnectOptions);
        return clientFactory;
    }

    /**
     * 配置入站消息通道。
     * - 若 consumer.async = true，则使用线程池处理（异步）
     * - 否则使用 DirectChannel（同步）
     */
    @Bean(name = MqttConstant.MQTT_INPUT_CHANNEL_NAME)
    public MessageChannel mqttInputChannel() {
        MqttConsumerProperties consumer = mqttProperties.getConsumer();
        Boolean async = consumer.getAsync();
        if (Boolean.TRUE.equals(async)) {
            return new ExecutorChannel(mqttConsumerExecutor());
        } else {
            return new DirectChannel();
        }
    }

    /**
     * 配置 MQTT 消息消费处理通道（队列模式），供消费者拉取使用。
     */
    @Bean(name = MqttConstant.MQTT_OUT_BOUND_CHANNEL_NAME)
    public MessageChannel consumerChannel() {
        MqttProducerProperties producer = mqttProperties.getProducer();
        Boolean async = producer.getAsync();

        if (Boolean.TRUE.equals(async)) {
            return new ExecutorChannel(mqttProducerExecutor());
        } else {
            return new DirectChannel();
        }
    }

    /**
     * 配置 MQTT 出站通道，向 broker 发送消息。
     */
    @Bean(name = MqttConstant.CONSUMER_CHANNEL_NAME)
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * 配置 MQTT 入站适配器，接收来自 MQTT broker 的消息。
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttPahoMessageDrivenChannelAdapter(
        MqttPahoClientFactory mqttPahoClientFactory,
        @Qualifier(MqttConstant.MQTT_INPUT_CHANNEL_NAME) MessageChannel mqttInputChannel,
        Environment environment) {

        MqttConsumerProperties consumer = mqttProperties.getConsumer();
        String clientId = consumer.getClientId();
        if (!StringUtils.hasText(clientId)) {
            clientId = getClientId(environment);
        }

        MqttPahoMessageDrivenChannelAdapter adapter =
            new MqttPahoMessageDrivenChannelAdapter(clientId,
                mqttPahoClientFactory);
        adapter.setAutoStartup(consumer.getAutoStartUp());
        adapter.setOutputChannel(mqttInputChannel);
        adapter.setQos(consumer.getQos());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setCompletionTimeout(consumer.getCompletionTimeout());
        adapter.setDisconnectCompletionTimeout(consumer.getDisconnectCompletionTimeout());
        return adapter;
    }

    /**
     * 配置 MQTT 出站消息处理器，用于发布消息。
     */
    @Bean
    @ServiceActivator(inputChannel = MqttConstant.MQTT_OUT_BOUND_CHANNEL_NAME)
    public MqttPahoMessageHandler mqttOutbound(MqttPahoClientFactory mqttPahoClientFactory,
        Environment environment) {
        MqttProducerProperties producer = mqttProperties.getProducer();
        String clientId = producer.getClientId();
        if (!StringUtils.hasText(clientId)) {
            clientId = getClientId(environment);
        }

        MqttPahoMessageHandler messageHandler =
            new MqttPahoMessageHandler(clientId, mqttPahoClientFactory);
        messageHandler.setAsync(producer.getAsync());
        messageHandler.setAsyncEvents(producer.getAsyncEvents());
        messageHandler.setDefaultTopic(producer.getDefaultTopic());
        messageHandler.setDefaultQos(producer.getDefaultQos());
        messageHandler.setConverter(new DefaultPahoMessageConverter());
        messageHandler.setDefaultRetained(producer.getDefaultRetained());
        return messageHandler;
    }

    /**
     * 构造封装的 MqttTemplate 工具类，提供更易用的发送/订阅能力。
     */
    @Bean
    public MqttOptions mqttOptions(MqttPahoMessageDrivenChannelAdapter adapter) {
        return new MqttTemplate(adapter);
    }

    /**
     * 构造入站消息处理器，分发到自定义监听器中。
     */
    @Bean
    public MqttMessageInboundHandler mqttMessageInboundHandler(
        List<MqttMessageConsumer> messageListeners,
        MqttOptions mqttOptions,
        Environment environment) {
        return new MqttMessageInboundHandler(messageListeners, mqttOptions, environment);
    }

    /**
     * 配置 MQTT 消息生产者网关
     */
    @Bean
    public GatewayProxyFactoryBean<MqttMessageProducer> mqttMessageProducer(
        @Qualifier(MqttConstant.MQTT_OUT_BOUND_CHANNEL_NAME) MessageChannel outboundChannel) {
        GatewayProxyFactoryBean<MqttMessageProducer> factoryBean =
            new GatewayProxyFactoryBean<>(MqttMessageProducer.class);
        factoryBean.setDefaultRequestChannel(outboundChannel);
        return factoryBean;
    }

    /**
     * 消费者异步线程池
     */
    public ThreadPoolTaskExecutor mqttConsumerExecutor() {
        MqttExecutorProperties executorProperties = mqttProperties.getConsumer().getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorProperties.getCorePoolSize());
        executor.setMaxPoolSize(executorProperties.getMaxPoolSize());
        executor.setQueueCapacity(executorProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(executorProperties.getKeepAliveSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("mqtt-consumer-");
        executor.initialize();
        return executor;
    }

    /**
     * 生产者异步线程池
     *
     * @return {@link ThreadPoolTaskExecutor }
     */
    public ThreadPoolTaskExecutor mqttProducerExecutor() {
        MqttExecutorProperties executorProperties = mqttProperties.getProducer().getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorProperties.getCorePoolSize());
        executor.setMaxPoolSize(executorProperties.getMaxPoolSize());
        executor.setQueueCapacity(executorProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(executorProperties.getKeepAliveSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("mqtt-producer-");
        executor.initialize();
        return executor;
    }

    /**
     * 自动生成客户端 ID，避免冲突。
     */
    private String getClientId(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "mqtt");
        return applicationName + StringConstants.DASHED + UUID.randomUUID()
            .toString()
            .replace(StringConstants.DASHED, "");
    }

    /**
     * mqtt关闭处理程序
     *
     * @param inboundAdapter  Mqtt Paho消息驱动通道适配器
     * @param outboundHandler MQTT Paho 消息处理器
     * @return {@link MqttShutdownHandler }
     */
    @Bean
    public MqttShutdownHandler mqttShutdownHandler(
        MqttPahoMessageDrivenChannelAdapter inboundAdapter,
        MqttPahoMessageHandler outboundHandler) {
        return new MqttShutdownHandler(inboundAdapter, outboundHandler);
    }

    /**
     * 自动配置类完成初始化时打印日志。
     */
    @PostConstruct
    public void postConstruct() {
        LOGGER.info("[ContiNew Starter] - Auto Configuration 'MQTT' completed initialization.");
    }
}
