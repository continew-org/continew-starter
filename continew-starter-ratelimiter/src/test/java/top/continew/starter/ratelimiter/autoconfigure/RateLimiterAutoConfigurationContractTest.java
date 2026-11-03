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

package top.continew.starter.ratelimiter.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.ratelimiter.aop.RateLimiterAspect;
import top.continew.starter.ratelimiter.generator.RateLimiterNameGenerator;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 限流器自动配置装配契约测试。
 *
 * <p>固化两条装配契约：切面构造直接注入 {@code RedissonClient} 且源码未提供
 * {@code @ConditionalOnBean} 保护，前置缺失时以快速失败暴露（与 Refresh Token 模块的
 * 静默跳过是两种不同契约，此处按真实行为固化）；名称生成器按
 * {@code @ConditionalOnMissingBean} 约定允许使用方整体覆盖。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RateLimiterAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RateLimiterAutoConfiguration.class));

    /**
     * imports 资源必须登记本模块包内全部自动配置类，防止新增配置类漏注册后静默失效。
     */
    @Test
    void importsShouldBeComplete() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.ratelimiter")).isEmpty();
    }

    /**
     * RedissonClient 就位时装配限流切面，切面是限流能力的唯一入口 Bean。
     */
    @Test
    void shouldRegisterAspectWhenRedissonClientPresent() {
        this.contextRunner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .run(context -> assertThat(context).hasSingleBean(RateLimiterAspect.class));
    }

    /**
     * 缺少 RedissonClient 时启动快速失败，缺失前置不以静默失效方式被掩盖。
     */
    @Test
    void shouldFailFastWhenRedissonClientAbsent() {
        this.contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .isInstanceOf(UnsatisfiedDependencyException.class);
        });
    }

    /**
     * 显式关闭开关后即使缺少 RedissonClient 也整体跳过，不影响应用启动。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.rate-limiter.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(RateLimiterAspect.class);
            });
    }

    /**
     * 使用方注册名称生成器后默认实现退避，容器保留用户实例。
     */
    @Test
    void shouldUseCustomNameGeneratorWhenPresent() {
        RateLimiterNameGenerator customGenerator = mock(RateLimiterNameGenerator.class);
        this.contextRunner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .withBean(RateLimiterNameGenerator.class, () -> customGenerator)
            .run(context -> assertThat(context.getBean(RateLimiterNameGenerator.class))
                .isSameAs(customGenerator));
    }

    /**
     * Key 前缀默认 RateLimiter，配置覆盖后生效。
     */
    @Test
    void shouldBindKeyPrefixProperty() {
        this.contextRunner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .run(context -> assertThat(context.getBean(RateLimiterProperties.class)
                .getKeyPrefix()).isEqualTo("RateLimiter"));
        this.contextRunner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .withPropertyValues("continew-starter.rate-limiter.key-prefix=RL")
            .run(context -> assertThat(context.getBean(RateLimiterProperties.class)
                .getKeyPrefix()).isEqualTo("RL"));
    }
}
