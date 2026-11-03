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

package top.continew.starter.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import top.continew.starter.core.autoconfigure.application.ApplicationAutoConfiguration;
import top.continew.starter.core.autoconfigure.application.ApplicationProperties;
import top.continew.starter.core.autoconfigure.threadpool.ThreadPoolAutoConfiguration;
import top.continew.starter.core.autoconfigure.threadpool.ThreadPoolExecutorRejectedPolicy;
import top.continew.starter.core.autoconfigure.threadpool.ThreadPoolExtensionProperties;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * Core 模块自动配置契约测试
 *
 * <p>固化 ApplicationAutoConfiguration 与 ThreadPoolAutoConfiguration 两个装配入口的契约：
 * imports 登记完整性、默认装配、开关装配、属性绑定与 {@code @ConditionalOnMissingBean}
 * 扩展点回退。core 为全部模块的公共依赖，本测试不依赖任何外部服务。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CoreAutoConfigurationContractTest {

    private final ApplicationContextRunner applicationRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ApplicationAutoConfiguration.class));

    private final ApplicationContextRunner threadPoolRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ThreadPoolAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert.findUnregistered("top.continew.starter.core")
            .isEmpty());
    }

    /**
     * ApplicationAutoConfiguration 无装配开关，默认应注册配置属性并引入 Hutool SpringUtil。
     */
    @Test
    void applicationShouldRegisterByDefault() {
        this.applicationRunner.run(context -> {
            assertThat(context).hasSingleBean(ApplicationProperties.class);
            // SpringUtil 由 @Import 与 @ComponentScan 双通道引入，只断言存在，不锁定注册数量
            assertThat(context).getBeans(SpringUtil.class).isNotEmpty();
        });
    }

    /**
     * application 前缀属性应绑定到 ApplicationProperties：production 默认 false，覆盖后生效。
     */
    @Test
    void applicationPropertiesShouldBindWithDefaultAndOverride() {
        this.applicationRunner
            .run(context -> assertFalse(context.getBean(ApplicationProperties.class)
                .isProduction()));
        this.applicationRunner
            .withPropertyValues("application.name=contract-core", "application.production=true")
            .run(context -> {
                ApplicationProperties properties = context.getBean(ApplicationProperties.class);
                assertEquals("contract-core", properties.getName());
                assertTrue(properties.isProduction());
            });
    }

    /**
     * ThreadPoolAutoConfiguration 默认应注册扩展属性，且异步与调度两个扩展定制器均生效
     * （两个扩展配置均 matchIfMissing，默认开启）。
     */
    @Test
    void threadPoolShouldRegisterCustomizersByDefault() {
        this.threadPoolRunner.run(context -> {
            assertThat(context).hasSingleBean(ThreadPoolExtensionProperties.class);
            assertThat(context).hasSingleBean(ThreadPoolTaskExecutorCustomizer.class);
            assertThat(context).hasSingleBean(ThreadPoolTaskSchedulerCustomizer.class);
            assertThat(context).hasSingleBean(AsyncConfigurer.class);
        });
    }

    /**
     * spring.task.execution.extension.enabled=false 时应跳过异步任务扩展装配，且不得影响上下文启动。
     */
    @Test
    void threadPoolShouldSkipWhenExecutionExtensionDisabled() {
        this.threadPoolRunner.withPropertyValues("spring.task.execution.extension.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ThreadPoolTaskExecutorCustomizer.class);
                assertThat(context).doesNotHaveBean(AsyncConfigurer.class);
            });
    }

    /**
     * 用户提供 AsyncConfigurer 时应整体回退，容器中保持用户实例（@ConditionalOnMissingBean 语义）。
     */
    @Test
    void asyncConfigurerShouldFallbackToUserProvidedBean() {
        AsyncConfigurer custom = mock(AsyncConfigurer.class);
        this.threadPoolRunner.withBean(AsyncConfigurer.class, () -> custom).run(context -> {
            assertThat(context).hasSingleBean(AsyncConfigurer.class);
            assertSame(custom, context.getBean(AsyncConfigurer.class));
        });
    }

    /**
     * 拒绝策略默认 CALLER_RUNS，覆盖配置后应绑定到异步任务扩展属性。
     */
    @Test
    void rejectedPolicyShouldBindWithDefaultAndOverride() {
        this.threadPoolRunner.run(context -> {
            ThreadPoolExtensionProperties properties =
                context.getBean(ThreadPoolExtensionProperties.class);
            assertEquals(ThreadPoolExecutorRejectedPolicy.CALLER_RUNS, properties.getExecution()
                .getRejectedPolicy());
        });
        this.threadPoolRunner.withPropertyValues("spring.task.execution.rejected-policy=ABORT")
            .run(context -> {
                ThreadPoolExtensionProperties properties =
                    context.getBean(ThreadPoolExtensionProperties.class);
                assertEquals(ThreadPoolExecutorRejectedPolicy.ABORT, properties.getExecution()
                    .getRejectedPolicy());
            });
    }
}
