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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import top.continew.starter.messaging.mqtt.msg.MqttMessageConsumer;
import top.continew.starter.messaging.mqtt.msg.MqttMessageProducer;
import top.continew.starter.messaging.mqtt.strategy.MqttOptions;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * MQTT 自动配置装配契约测试
 *
 * <p>开关属性为 {@code continew-starter.messaging.mqtt.enabled} 且未声明
 * {@code matchIfMissing}，即默认不启用，必须显式开启后才装配。开启用例固定关闭
 * {@code consumer.auto-start-up}，装配契约不验证与真实 broker 的连接。模块各 Bean 均无
 * {@code @ConditionalOnMissingBean}，扩展点形式为注入 {@link MqttMessageConsumer} 监听器列表。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MqttAutoConfigurationContractTest {

    private static final String BASE_PACKAGE = "top.continew.starter.messaging.mqtt";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MqttAutoConfiguration.class))
        .withUserConfiguration(IntegrationInfrastructureConfiguration.class)
        // 生产者网关（GatewayProxyFactoryBean）依赖容器中的 taskScheduler，
        // 真实应用由 Boot 的 TaskSchedulingAutoConfiguration 提供，契约层按类型补齐
        .withBean(TaskScheduler.class, ThreadPoolTaskScheduler::new);

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 默认（未声明 enabled）应整体跳过装配，不得产生任何 MQTT Bean 或启动错误。
     */
    @Test
    void shouldSkipByDefaultWithoutEnabledProperty() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(MqttOptions.class);
            assertThat(context).doesNotHaveBean(MqttConnectOptions.class);
        });
    }

    /**
     * 显式开启后应装配核心组件，自定义消费者监听器进入装配链。
     */
    @Test
    void shouldRegisterCoreBeansWhenEnabled() {
        this.contextRunner
            .withPropertyValues("continew-starter.messaging.mqtt.enabled=true",
                "continew-starter.messaging.mqtt.host=tcp://localhost:1883",
                "continew-starter.messaging.mqtt.password=test-only",
                "continew-starter.messaging.mqtt.consumer.auto-start-up=false")
            .withBean(MqttMessageConsumer.class, () -> mock(MqttMessageConsumer.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(MqttConnectOptions.class);
                assertThat(context).hasSingleBean(MqttOptions.class);
                assertThat(context).hasSingleBean(MqttMessageProducer.class);
            });
    }

    /**
     * host 配置应传递到连接选项的服务器地址。
     */
    @Test
    void shouldBindHostPropertyToConnectOptions() {
        this.contextRunner
            .withPropertyValues("continew-starter.messaging.mqtt.enabled=true",
                "continew-starter.messaging.mqtt.host=tcp://mqtt.example.com:61883",
                "continew-starter.messaging.mqtt.password=test-only",
                "continew-starter.messaging.mqtt.consumer.auto-start-up=false")
            .withBean(MqttMessageConsumer.class, () -> mock(MqttMessageConsumer.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                MqttConnectOptions options = context.getBean(MqttConnectOptions.class);
                assertThat(options.getServerURIs()).containsExactly("tcp://mqtt.example.com:61883");
            });
    }

    /**
     * Spring Integration 基础设施的最小等价环境：真实应用由 Boot 的 Integration 自动配置触发
     * {@code @EnableIntegration}，契约层直接启用同一开关，使 SpEL 求值上下文等基础 Bean 可用。
     */
    @Configuration(proxyBeanMethods = false)
    @EnableIntegration
    static class IntegrationInfrastructureConfiguration {
    }
}
