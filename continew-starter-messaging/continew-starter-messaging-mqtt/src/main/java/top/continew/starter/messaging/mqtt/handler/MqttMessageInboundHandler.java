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

package top.continew.starter.messaging.mqtt.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import top.continew.starter.messaging.mqtt.annotation.MqttListener;
import top.continew.starter.messaging.mqtt.constant.MqttConstant;
import top.continew.starter.messaging.mqtt.enums.TopicFilterType;
import top.continew.starter.messaging.mqtt.exception.MqttException;
import top.continew.starter.messaging.mqtt.model.MqttMessage;
import top.continew.starter.messaging.mqtt.msg.MqttMessageConsumer;
import top.continew.starter.messaging.mqtt.strategy.MqttOptions;
import top.continew.starter.messaging.mqtt.util.TopicUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息调度器 路由分发中心
 *
 * @author echo
 * @since 2.15.0
 */
public class MqttMessageInboundHandler
    implements MessageHandler, InitializingBean, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageInboundHandler.class);

    // 精确匹配的topic -> 监听器映射（用于@MqttListener注解的监听器）
    private final Map<String, MqttMessageConsumer> annotatedListenerMap = new ConcurrentHashMap<>();

    // 所有实现了MqttMessageListener的Bean列表（用于动态订阅）
    private final List<MqttMessageConsumer> allListeners;

    private final MqttOptions mqttOptions;

    private final Environment environment;

    @Nullable
    private ApplicationContext applicationContext;

    public MqttMessageInboundHandler(List<MqttMessageConsumer> mqttMessageConsumerList,
        MqttOptions mqttOptions,
        Environment environment) {
        this.allListeners = mqttMessageConsumerList;
        this.mqttOptions = mqttOptions;
        this.environment = environment;
    }

    @Override
    @ServiceActivator(inputChannel = MqttConstant.MQTT_INPUT_CHANNEL_NAME)
    public void handleMessage(Message<?> message) throws MessagingException {
        MqttMessage mqttMessage = MqttMessage.of(message);
        String topic = mqttMessage.getTopic();
        // 1. 优先处理通过@MqttListener注解订阅的精确匹配
        boolean handled = handleAnnotatedListeners(topic, mqttMessage);
        // 2. 如果没有精确匹配的注解监听器，则广播给所有监听器
        if (!handled) {
            broadcastToAllListeners(mqttMessage);
        }
    }

    /**
     * 处理通过@MqttListener注解订阅的监听器
     *
     * @return true if message was handled by annotated listener
     */
    private boolean handleAnnotatedListeners(String topic, MqttMessage mqttMessage) {
        boolean handled = false;

        for (Map.Entry<String, MqttMessageConsumer> entry : annotatedListenerMap.entrySet()) {
            String topicFilter = entry.getKey();
            try {
                if (isTopicMatch(topicFilter, topic)) {
                    entry.getValue().onMessage(mqttMessage);
                    handled = true;
                }
            } catch (Exception e) {
                log.error("注解监听器处理消息时发生错误: {}", entry.getValue().getClass().getSimpleName(), e);
            }
        }

        return handled;
    }

    /**
     * 广播消息给所有监听器（用于动态订阅的场景）
     */
    private void broadcastToAllListeners(MqttMessage mqttMessage) {
        // 获取当前订阅的所有topics
        List<String> subscribedTopics = mqttOptions.listTopics();

        // 检查消息的topic是否在订阅列表中
        boolean isSubscribed = subscribedTopics.stream()
            .anyMatch(subscribedTopic -> isTopicMatch(subscribedTopic, mqttMessage.getTopic()));

        if (!isSubscribed) {
            return;
        }

        // 广播给所有没有@MqttListener注解的监听器
        for (MqttMessageConsumer listener : allListeners) {
            // 跳过已经通过注解处理的监听器
            if (isAnnotatedListener(listener)) {
                continue;
            }
            try {
                listener.onMessage(mqttMessage);
            } catch (Exception e) {
                log.error("监听器处理消息时发生错误: {}", listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * 检查是否是注解监听器
     */
    private boolean isAnnotatedListener(MqttMessageConsumer listener) {
        return annotatedListenerMap.containsValue(listener);
    }

    /**
     * 判断topic是否匹配
     */
    private boolean isTopicMatch(String topicFilter, String actualTopic) {
        // 精确匹配
        if (topicFilter.equals(actualTopic)) {
            return true;
        }

        // 通配符匹配
        if (TopicUtils.isTopicFilter(topicFilter) && TopicUtils.match(topicFilter, actualTopic)) {
            return true;
        }

        // 共享订阅匹配
        return TopicFilterType.SHARE.equals(TopicFilterType.getType(topicFilter))
            && TopicFilterType.SHARE
                .match(topicFilter, actualTopic);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(applicationContext, "applicationContext不能为null");
        if (CollectionUtils.isEmpty(allListeners)) {
            return;
        }
        // 处理带有@MqttListener注解的监听器
        for (MqttMessageConsumer mqttMessageConsumer : allListeners) {
            Class<?> clazz = ClassUtils.getUserClass(mqttMessageConsumer);
            MqttListener mqttListener = AnnotationUtils.findAnnotation(clazz, MqttListener.class);

            if (Objects.nonNull(mqttListener)) {
                String topic = resolvePlaceholder(mqttListener.topic());
                int qos = resolveQos(mqttListener.qos());
                mqttOptions.addTopic(topic, qos);
                annotatedListenerMap.put(topic, mqttMessageConsumer);
            } else {
                log.info("发现无注解监听器: {}，将接收所有动态订阅的消息", clazz.getSimpleName());
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 解析占位符，支持 ${} 和直接字符串
     */
    private String resolvePlaceholder(String value) {
        if (StringUtils.hasText(value)) {
            return environment.resolvePlaceholders(value);
        }
        return value;
    }

    /**
     * 解析 QoS 配置,支持占位符和直接数字
     *
     * @param qosValue QoS 字符串值或占位符
     * @return QoS 等级 (0, 1, 或 2)
     * @throws MqttException 如果 QoS 值无效
     */
    private int resolveQos(String qosValue) {
        try {
            String resolved = resolvePlaceholder(qosValue);
            int qos = Integer.parseInt(resolved);

            // ✅ 验证 QoS 值是否合法
            if (qos < 0 || qos > 2) {
                throw new MqttException("QoS 值必须在 0-2 之间, 当前值: " + qos);
            }

            return qos;
        } catch (NumberFormatException e) {
            throw new MqttException("无法解析 QoS 值: " + qosValue + ", 必须是 0, 1 或 2", e);
        }
    }
}
