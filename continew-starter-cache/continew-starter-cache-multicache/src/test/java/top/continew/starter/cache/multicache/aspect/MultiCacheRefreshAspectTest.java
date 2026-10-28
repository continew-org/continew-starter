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

package top.continew.starter.cache.multicache.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import top.continew.starter.cache.multicache.annotation.MultiCacheRefresh;
import top.continew.starter.cache.multicache.autoconfigure.MultiLevelCacheProperties;
import top.continew.starter.cache.multicache.cache.FakeRedisCacheWriter;
import top.continew.starter.cache.multicache.cache.MultiLevelCacheManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多级缓存定时刷新切面测试（验证刷新真实回源并更新缓存，而不是被 @Cacheable 命中短路）
 *
 * @author KAI
 * @since 3.0.0
 */
class MultiCacheRefreshAspectTest {

    private static final String CACHE_NAME = "REFRESH:";

    @Test
    @DisplayName("缓存命中期间后台刷新真实回源并更新缓存")
    void testRefreshUpdatesCachedValue() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            TestConfig.class)) {
            RefreshService service = context.getBean(RefreshService.class);
            assertEquals("v1", service.load());
            assertEquals("v1", service.load());
            // 轮询等待后台刷新：先失效该键再经代理重放，方法应重新执行并写回新值
            String value = "v1";
            for (int i = 0; i < 100 && "v1".equals(value); i++) {
                Thread.sleep(50);
                value = service.load();
            }
            assertNotEquals("v1", value, "后台刷新应已回源更新缓存");
            assertTrue(Integer.parseInt(value.substring(1)) >= 2, "方法应被重新执行");
            assertEquals(1, context.getBean(MultiCacheRefreshAspect.class).getRefreshTaskCount());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        MultiLevelCacheManager cacheManager() {
            MultiLevelCacheManager manager = new MultiLevelCacheManager(new FakeRedisCacheWriter(),
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)),
                new MultiLevelCacheProperties(), null, List.of(CACHE_NAME));
            manager.afterPropertiesSet();
            return manager;
        }

        @Bean
        MultiCacheRefreshAspect multiCacheRefreshAspect(CacheManager cacheManager) {
            return new MultiCacheRefreshAspect(cacheManager);
        }

        @Bean
        RefreshService refreshService() {
            return new RefreshService();
        }
    }

    static class RefreshService {

        private final AtomicInteger counter = new AtomicInteger();

        @Cacheable(cacheNames = CACHE_NAME)
        @MultiCacheRefresh(refresh = 100, timeUnit = TimeUnit.MILLISECONDS, retryAfterFailure = 50)
        public String load() {
            return "v" + counter.incrementAndGet();
        }
    }
}
