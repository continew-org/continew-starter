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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import top.continew.starter.cache.multicache.cache.MultiLevelCacheManager;
import top.continew.starter.cache.springcache.autoconfigure.SpringCacheAutoConfiguration;

/**
 * 多级缓存自动配置测试（验证与 Spring Boot 内置缓存自动配置的装配顺序）
 *
 * @author Charles7c
 * @since 3.0.0
 */
class MultiCacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withPropertyValues("continew-starter.cache.multi.broadcast-enabled=false")
        .withConfiguration(AutoConfigurations.of(MultiCacheAutoConfiguration.class,
            SpringCacheAutoConfiguration.class, CacheAutoConfiguration.class))
        .withUserConfiguration(EnableCachingConfiguration.class);

    @Test
    @DisplayName("应用自带 @EnableCaching 时多级缓存管理器仍确定性接管 CacheManager")
    void testMultiLevelCacheManagerTakesOver() {
        // 上下文启动失败时 getBean 会携带启动异常一起抛出
        runner.run(context -> assertInstanceOf(MultiLevelCacheManager.class,
            context.getBean(CacheManager.class)));
    }

    @EnableCaching
    @Configuration(proxyBeanMethods = false)
    static class EnableCachingConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return Mockito.mock(RedisConnectionFactory.class);
        }
    }
}
