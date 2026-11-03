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

package top.continew.starter.cache.redisson.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import top.continew.starter.cache.redisson.handler.NameMapperHandler;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * Redisson 自动配置契约测试。
 *
 * <p>固化装配契约：模块开关（spring.data.redisson.enabled）装配条件、key-prefix 到内置
 * NameMapper 实现的回退、代表性配置属性绑定。DataRedisProperties 与 ObjectMapper 为装配
 * 硬前置，分别以默认实例与 Jackson 3 JsonMapper 提供；不创建真实 RedissonClient，仅验证
 * 配置器生成阶段的条件行为。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RedissonAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RedissonAutoConfiguration.class))
        .withBean(DataRedisProperties.class, DataRedisProperties::new)
        .withBean(ObjectMapper.class, () -> JsonMapper.builder().build());

    /**
     * imports 注册完整性：包内全部自动配置类均已登记进 AutoConfiguration.imports。
     */
    @Test
    void shouldRegisterAllAutoConfigurationClasses() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.cache.redisson")).isEmpty();
    }

    /**
     * 开关默认开启（matchIfMissing）：Redisson 配置定制器应注册。
     */
    @Test
    void shouldRegisterCustomizerByDefault() {
        this.contextRunner.run(context -> assertThat(context)
            .hasSingleBean(RedissonAutoConfigurationCustomizer.class));
    }

    /**
     * 开关关闭时跳过，不注册配置定制器且不影响应用启动。
     */
    @Test
    void shouldSkipSilentlyWhenDisabled() {
        this.contextRunner.withPropertyValues("spring.data.redisson.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(RedissonAutoConfigurationCustomizer.class);
            });
    }

    /**
     * 配置 key-prefix 后，单机模式应回退到内置 NameMapper 实现，保证缓存键带前缀。
     *
     * <p>产品侧回退路径基于 Redisson 已过时的 BaseConfig#setNameMapper（见
     * RedissonAutoConfiguration 构建配置），本测试有意沿用同一 API 固化其行为，
     * 待产品侧迁移 Config#setNameMapper 后同步调整。</p>
     */
    @SuppressWarnings("deprecation")
    @Test
    void shouldApplyNameMapperFallbackWhenKeyPrefixConfigured() {
        this.contextRunner
            .withPropertyValues("spring.data.redisson.key-prefix=continew:cache:pre:")
            .run(context -> {
                RedissonAutoConfigurationCustomizer customizer = context
                    .getBean(RedissonAutoConfigurationCustomizer.class);
                Config config = new Config();
                customizer.customize(config);
                assertThat(config.useSingleServer().getNameMapper())
                    .isInstanceOf(NameMapperHandler.class);
            });
    }

    /**
     * 属性绑定：key-prefix 默认未设置，覆盖值生效。
     */
    @Test
    void shouldBindKeyPrefixProperty() {
        this.contextRunner.run(context -> assertThat(context.getBean(RedissonProperties.class)
            .getKeyPrefix()).isNull());
        this.contextRunner
            .withPropertyValues("spring.data.redisson.key-prefix=continew:cache:pre:")
            .run(context -> assertThat(context.getBean(RedissonProperties.class).getKeyPrefix())
                .isEqualTo("continew:cache:pre:"));
    }
}
