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

package top.continew.starter.cache.multicache.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.continew.starter.cache.multicache.cache.CacheInvalidationPublisher;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 多级缓存自动配置契约测试。
 *
 * <p>固化装配契约：模块开关的默认开启与关闭跳过、L1 失效广播开关对广播 Bean 的装配控制、
 * CacheManager 的用户实现 fallback、代表性配置属性绑定。Redis 前置以 Mockito mock 提供；
 * 广播开启时失效监听容器作为 SmartLifecycle 需真实 Redis 完成订阅注册，其链路验证属集成
 * 测试（T3）范围，本测试统一在广播关闭形态下运行。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MultiCacheAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MultiCacheAutoConfiguration.class))
        .withPropertyValues("continew-starter.cache.multi.broadcast-enabled=false")
        .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
        .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    /**
     * imports 注册完整性：包内全部自动配置类均已登记进 AutoConfiguration.imports。
     */
    @Test
    void shouldRegisterAllAutoConfigurationClasses() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.cache.multicache")).isEmpty();
    }

    /**
     * 模块开关默认开启（matchIfMissing）：缓存管理器应注册，且广播关闭形态下不注册广播器。
     */
    @Test
    void shouldRegisterCacheManagerByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context).doesNotHaveBean(CacheInvalidationPublisher.class);
        });
    }

    /**
     * 模块开关关闭时整体跳过，不注册任何缓存 Bean 且不影响应用启动。
     */
    @Test
    void shouldSkipSilentlyWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.cache.multi.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(CacheManager.class);
                assertThat(context).doesNotHaveBean(CacheInvalidationPublisher.class);
            });
    }

    /**
     * 缓存管理器以 Redis 连接工厂为硬前置：缺失时应启动失败暴露缺 Bean，而非静默注册。
     */
    @Test
    void shouldFailFastWhenConnectionFactoryAbsent() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MultiCacheAutoConfiguration.class))
            .withPropertyValues("continew-starter.cache.multi.broadcast-enabled=false")
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(UnsatisfiedDependencyException.class);
            });
    }

    /**
     * 用户已提供 CacheManager 时模块不接管，容器保留用户实例（同一性）。
     */
    @Test
    void shouldKeepUserCacheManagerWhenPresent() {
        NoOpCacheManager userCacheManager = new NoOpCacheManager();
        this.contextRunner.withBean(CacheManager.class, () -> userCacheManager)
            .run(context -> assertThat(context.getBean(CacheManager.class))
                .isSameAs(userCacheManager));
    }

    /**
     * 属性绑定：Redis 失效降级开关默认开启，覆盖值生效。
     */
    @Test
    void shouldBindDegradeOnRedisFailureProperty() {
        this.contextRunner.run(context -> assertThat(context
            .getBean(MultiLevelCacheProperties.class).isDegradeOnRedisFailure()).isTrue());
        this.contextRunner
            .withPropertyValues("continew-starter.cache.multi.degrade-on-redis-failure=false")
            .run(context -> assertThat(context.getBean(MultiLevelCacheProperties.class)
                .isDegradeOnRedisFailure()).isFalse());
    }
}
