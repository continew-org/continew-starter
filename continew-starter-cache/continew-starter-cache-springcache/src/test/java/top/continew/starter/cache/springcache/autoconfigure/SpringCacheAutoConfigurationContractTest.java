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

package top.continew.starter.cache.springcache.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import java.time.Duration;

/**
 * Spring Cache 自动配置契约测试。
 *
 * <p>固化装配契约：该模块无开关属性，默认即注册 Redis 缓存配置与 key 生成策略；并验证
 * default-cache-springcache.yml 提供的 TTL 默认值可被用户配置覆盖。ObjectMapper 为构造
 * 硬前置，以 Jackson 3 JsonMapper 提供。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SpringCacheAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SpringCacheAutoConfiguration.class))
        .withBean(ObjectMapper.class, () -> JsonMapper.builder().build());

    /**
     * imports 注册完整性：包内全部自动配置类均已登记进 AutoConfiguration.imports。
     */
    @Test
    void shouldRegisterAllAutoConfigurationClasses() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.cache.springcache")).isEmpty();
    }

    /**
     * 无开关默认注册：Redis 缓存配置、key 生成策略与 CachingConfigurer 实现均应装配。
     */
    @Test
    void shouldRegisterCacheInfrastructureByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RedisCacheConfiguration.class);
            assertThat(context).hasSingleBean(KeyGenerator.class);
            assertThat(context).hasSingleBean(CachingConfigurer.class);
        });
    }

    /**
     * 属性绑定：缓存 TTL 默认 2 小时（default-cache-springcache.yml 提供），覆盖值生效。
     */
    @Test
    void shouldBindTimeToLiveProperty() {
        this.contextRunner.run(context -> assertThat(context.getBean(RedisCacheConfiguration.class)
            .getTtlFunction()
            .getTimeToLive("anyKey", "anyValue")).isEqualTo(Duration.ofHours(2)));
        this.contextRunner.withPropertyValues("spring.cache.redis.time-to-live=PT10S")
            .run(context -> assertThat(context.getBean(RedisCacheConfiguration.class)
                .getTtlFunction()
                .getTimeToLive("anyKey", "anyValue")).isEqualTo(Duration.ofSeconds(10)));
    }
}
