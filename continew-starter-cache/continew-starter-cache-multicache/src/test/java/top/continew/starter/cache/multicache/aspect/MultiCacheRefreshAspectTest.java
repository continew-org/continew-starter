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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
            // 等待后台刷新：先失效该键再经代理重放，方法应重新执行并写回新值
            Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(50))
                .until(() -> !"v1".equals(service.load()));
            String value = service.load();
            assertTrue(Integer.parseInt(value.substring(1)) >= 2, "方法应被重新执行");
            assertEquals(1, context.getBean(MultiCacheRefreshAspect.class).getRefreshTaskCount());
        }
    }

    @Test
    @DisplayName("单方法任务数达上限时淘汰最久未用任务，其他方法仍能注册并刷新")
    void testPerMethodTaskCapKeepsOtherMethodsWorking() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            PerMethodCappedConfig.class)) {
            MultiCacheRefreshAspect aspect = context.getBean(MultiCacheRefreshAspect.class);
            RefreshService service = context.getBean(RefreshService.class);
            for (int i = 0; i < 5; i++) {
                service.loadByKey("k" + i);
            }
            assertEquals(2, aspect.getRefreshTaskCount(), "动态参数方法应按方法上限封顶");
            String cached = service.load();
            assertEquals(3, aspect.getRefreshTaskCount(), "其他方法不应因上限被静默丢弃");
            awaitRefreshed(service, cached);
            assertEquals(3, aspect.getRefreshTaskCount());
        }
    }

    @Test
    @DisplayName("全局任务数达上限时淘汰最久未用任务，新方法仍能注册并刷新")
    void testGlobalTaskCapEvictsLeastRecentlyUsed() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            GlobalCappedConfig.class)) {
            MultiCacheRefreshAspect aspect = context.getBean(MultiCacheRefreshAspect.class);
            RefreshService service = context.getBean(RefreshService.class);
            service.loadByKey("g0");
            service.loadByKey("g1");
            service.loadByKey("g2");
            assertEquals(2, aspect.getRefreshTaskCount());
            String cached = service.load();
            assertEquals(2, aspect.getRefreshTaskCount(), "全局上限应淘汰旧任务而不是丢弃新任务");
            awaitRefreshed(service, cached);
        }
    }

    /**
     * 等待固定参数方法的刷新任务生效（缓存值被后台刷新改写）
     *
     * <p>
     * 后台刷新周期为 100ms，轮询交给 Awaitility 调度，失败时直接抛 ConditionTimeoutException，
     * 无需手写 sleep 循环。
     * </p>
     *
     * @param service 被刷新的服务
     * @param cached  注册任务时的缓存值
     */
    private static void awaitRefreshed(RefreshService service, String cached) {
        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(50))
            .until(() -> !cached.equals(service.load()));
    }

    private static MultiLevelCacheManager newCacheManager() {
        MultiLevelCacheManager manager = new MultiLevelCacheManager(new FakeRedisCacheWriter(),
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)),
            new MultiLevelCacheProperties(), null, List.of(CACHE_NAME));
        manager.afterPropertiesSet();
        return manager;
    }

    @Test
    @DisplayName("淘汰竞态：自续期新 future 提交后才被取消时，新 future 立即取消（不复活调度）")
    void testSelfRenewalRaceCancelsFreshFuture() {
        List<RecordingFuture> futures = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        ScheduledExecutorService mockScheduler = Mockito.mock(ScheduledExecutorService.class);
        try (
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("mockScheduler", mockScheduler);
            context.register(RaceConfig.class);
            context.refresh();
            RefreshService service = context.getBean(RefreshService.class);
            MultiCacheRefreshAspect aspect = context.getBean(MultiCacheRefreshAspect.class);
            // mock 不执行任务，只记录每次提交的 future；第 3 次提交（k1 的自续期）的回答内
            // 注册 k3 → 按方法上限淘汰 k1，复现「cancelTask 读到旧 future、新 future 随后提交」的窗口
            Mockito.when(mockScheduler.schedule(Mockito.any(Runnable.class), Mockito.anyLong(),
                Mockito.any(TimeUnit.class))).thenAnswer(invocation -> {
                    RecordingFuture future = new RecordingFuture();
                    futures.add(future);
                    if (calls.incrementAndGet() == 3) {
                        service.loadByKey("k3");
                    }
                    return future;
                });
            service.loadByKey("k1");
            service.loadByKey("k2");
            assertEquals(2, aspect.getRefreshTaskCount());
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            Mockito.verify(mockScheduler, Mockito.times(2)).schedule(captor.capture(),
                Mockito.anyLong(), Mockito.any(TimeUnit.class));
            // 手动执行 k1 的自续期 runnable：schedule 提交新 future 后，k1 才被淘汰取消
            captor.getAllValues().get(0).run();
            assertEquals(4, calls.get(), "自续期在 mock 内触发第 3 次提交，k3 注册触发第 4 次");
            assertTrue(futures.get(0).isCancelled(), "被淘汰任务 k1 的旧 future 应已取消");
            assertTrue(futures.get(2).isCancelled(), "竞态窗口内 k1 的新 future 必须由复查取消");
            assertFalse(futures.get(3).isCancelled(), "新任务 k3 的 future 不应被误取消");
            assertEquals(2, aspect.getRefreshTaskCount(), "k1 淘汰、k3 注册，总数维持在上限");
        }
    }

    /**
     * 只记取消状态的 ScheduledFuture 桩（mock 调度器不真正执行任务）
     */
    static class RecordingFuture implements ScheduledFuture<Object> {

        private volatile boolean cancelled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed o) {
            return 0;
        }
    }

    /**
     * 竞态测试配置：注入 mock 调度器，按方法与全局上限均为 2
     */
    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class RaceConfig {

        @Bean
        MultiLevelCacheManager cacheManager() {
            return newCacheManager();
        }

        @Bean
        RefreshService refreshService() {
            return new RefreshService();
        }

        @Bean
        MultiCacheRefreshAspect multiCacheRefreshAspect(CacheManager cacheManager,
            ScheduledExecutorService scheduler) {
            return new MultiCacheRefreshAspect(cacheManager, scheduler, 2, 2);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        MultiLevelCacheManager cacheManager() {
            return newCacheManager();
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

    /**
     * 按方法上限 2、全局上限 4 的切面配置
     */
    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    @EnableAspectJAutoProxy
    static class PerMethodCappedConfig {

        @Bean
        MultiLevelCacheManager cacheManager() {
            return newCacheManager();
        }

        @Bean
        MultiCacheRefreshAspect multiCacheRefreshAspect(CacheManager cacheManager) {
            return new MultiCacheRefreshAspect(cacheManager, 4, 2);
        }

        @Bean
        RefreshService refreshService() {
            return new RefreshService();
        }
    }

    /**
     * 全局上限 2、按方法上限 2 的切面配置
     */
    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    @EnableAspectJAutoProxy
    static class GlobalCappedConfig {

        @Bean
        MultiLevelCacheManager cacheManager() {
            return newCacheManager();
        }

        @Bean
        MultiCacheRefreshAspect multiCacheRefreshAspect(CacheManager cacheManager) {
            return new MultiCacheRefreshAspect(cacheManager, 2, 2);
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

        /**
         * 动态参数方法：每个参数组合注册一个刷新任务（周期很长，避免与上位测试的 100ms 刷新互相干扰）
         *
         * @param key 缓存键
         * @return 由调用序号生成的缓存值
         */
        @Cacheable(cacheNames = CACHE_NAME)
        @MultiCacheRefresh(refresh = 1, timeUnit = TimeUnit.HOURS)
        public String loadByKey(String key) {
            return key + "-v" + counter.incrementAndGet();
        }
    }
}
