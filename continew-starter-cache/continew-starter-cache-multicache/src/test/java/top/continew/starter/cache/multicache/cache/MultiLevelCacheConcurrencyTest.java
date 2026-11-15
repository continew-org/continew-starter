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

package top.continew.starter.cache.multicache.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import top.continew.starter.cache.multicache.autoconfigure.MultiLevelCacheProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多级缓存并发窗口测试：并发回源、并发 put/evict 与本地容量淘汰边界。
 *
 * <p>断言只锁「不丢不重」与容量上限两类硬契约：并发回源共享同一加载结果、
 * 并发写读后两级视图最终一致、Caffeine 容量上限永不突破。不锁任何时序。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("多级缓存并发窗口")
class MultiLevelCacheConcurrencyTest {

    private static final String CACHE_NAME = "TEST:";
    private static final int THREADS = 12;

    private FakeRedisCacheWriter newCacheWriter() {
        return new FakeRedisCacheWriter();
    }

    private MultiLevelCacheManager newManager(FakeRedisCacheWriter cacheWriter, long localLimit) {
        RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .entryTtl(Duration.ofHours(2));
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        MultiLevelCacheProperties.Local local = new MultiLevelCacheProperties.Local();
        local.setLimit(localLimit);
        properties.getLocal().setLimit(localLimit);
        MultiLevelCacheManager manager = new MultiLevelCacheManager(cacheWriter, redisConfig,
            properties, null, List.of(CACHE_NAME));
        manager.afterPropertiesSet();
        return manager;
    }

    /**
     * 并发 get(loader) 多键回源：每个键恰好加载一次，全部线程拿到各自键的同一结果。
     */
    @RepeatedTest(value = 5, name = "{currentRepetition}/{totalRepetitions}")
    @DisplayName("多键并发回源：每键恰好加载一次且结果一致")
    void shouldLoadEachKeyExactlyOnceUnderConcurrentGet() throws Exception {
        FakeRedisCacheWriter cacheWriter = newCacheWriter();
        Cache cache = newManager(cacheWriter, 100).getCache(CACHE_NAME);
        int keyCount = 6;
        AtomicInteger loadCount = new AtomicInteger();
        java.util.Set<Integer> loadedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

        runConcurrently(() -> {
            int keyIndex = (int) (Thread.currentThread().getId() % keyCount);
            String key = "hot-" + keyIndex;
            String value = (String) cache.get(key, () -> {
                loadCount.incrementAndGet();
                loadedKeys.add(keyIndex);
                return "loaded-" + keyIndex;
            });
            assertEquals("loaded-" + keyIndex, value, "并发回源应拿到本键的加载结果");
        });

        // 硬契约：每个键的加载器恰好执行一次（12 线程抢 6 键，共 6 次加载）
        assertEquals(keyCount, loadCount.get(), "多键并发回源的加载总次数");
        assertEquals(keyCount, loadedKeys.size(), "被加载的键种类数");
    }

    /**
     * 并发 put/evict/get 交错后视图最终一致：两级缓存不丢已确认写入的键。
     */
    @RepeatedTest(value = 5, name = "{currentRepetition}/{totalRepetitions}")
    @DisplayName("并发写读交错：已确认写入的键两级可见")
    void shouldKeepConfirmedEntriesVisibleAfterConcurrentReadWrite() throws Exception {
        FakeRedisCacheWriter cacheWriter = newCacheWriter();
        Cache cache = newManager(cacheWriter, 100).getCache(CACHE_NAME);
        // 先由写线程确认写入的键集合，读线程只验证这些键最终可读
        java.util.Set<String> confirmedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger done = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(THREADS);
        try {
            for (int i = 0; i < THREADS; i++) {
                int workerId = i;
                futures.add(executor.submit(() -> {
                    try {
                        assertTrue(startLatch.await(5, TimeUnit.SECONDS), "启动信号未就位");
                        if (workerId % 3 == 0) {
                            // 写线程：写入并立即确认（put 返回即两级写入完成）
                            for (int round = 0; round < 20; round++) {
                                String key = "w" + workerId + "-" + round;
                                cache.put(key, "v" + round);
                                confirmedKeys.add(key);
                            }
                        } else if (workerId % 3 == 1) {
                            // 读线程：随机读，只要求不抛异常
                            for (int round = 0; round < 50; round++) {
                                cache.get("w" + (round % THREADS) + "-" + (round % 20));
                            }
                        } else {
                            // 淘汰线程：只淘汰未确认的探针键，不得动写线程的确认键
                            for (int round = 0; round < 10; round++) {
                                cache.evict("probe-" + round);
                            }
                        }
                        return null;
                    } finally {
                        done.incrementAndGet();
                    }
                }));
            }
            startLatch.countDown();
            awaitAll(executor, futures, done, THREADS);
        } finally {
            executor.shutdownNow();
        }

        // 硬契约：全部确认键在两级缓存可读（无静默丢失）
        for (String key : confirmedKeys) {
            Cache.ValueWrapper wrapper = cache.get(key);
            assertNotNull(wrapper, "确认写入的键 " + key + " 不应丢失");
        }
    }

    /**
     * 本地容量上限永不突破：并发写入远超 limit 的键后，Caffeine 窗口内条目数不超过上限。
     */
    @Test
    @DisplayName("本地容量淘汰：并发写超限后条目数收敛到上限内")
    void shouldCapLocalEntriesUnderConcurrentOverflowWrites() throws Exception {
        FakeRedisCacheWriter cacheWriter = newCacheWriter();
        long limit = 10;
        MultiLevelCache cache = (MultiLevelCache) newManager(cacheWriter, limit)
            .getCache(CACHE_NAME);
        assertTrue(cache.isLocalEnabled(), "容量上限配置下 L1 应启用");

        AtomicInteger workerSeq = new AtomicInteger();
        runConcurrently(() -> {
            int workerId = workerSeq.incrementAndGet();
            for (int round = 0; round < 20; round++) {
                cache.put("overflow-" + workerId + "-" + round, "v" + round);
            }
        });

        // 硬契约：触发 Caffeine 维护窗口后，条目数收敛到上限（cleanUp 强制执行待决淘汰）
        long estimated = cache.drainLocalAndGetEstimatedSize();
        assertTrue(estimated <= limit, "并发超限写入且维护后 L1 条目数应不超过上限，实际 "
            + estimated);
        // L2 是全量事实源：任意键仍可经 L2 读回
        cache.get("overflow-1-0");
        assertEquals("v0", cache.get("overflow-1-0").get(), "被 L1 淘汰的键应能从 L2 读回");
    }

    /**
     * L1 关闭时容量视图恒为 0：不触碰任何本地缓存操作。
     */
    @Test
    @DisplayName("L1 关闭时容量视图返回 0")
    void shouldReturnZeroSizeWhenLocalDisabled() {
        FakeRedisCacheWriter cacheWriter = newCacheWriter();
        MultiLevelCache cache = (MultiLevelCache) newManager(cacheWriter, 0)
            .getCache(CACHE_NAME);
        assertEquals(0, cache.drainLocalAndGetEstimatedSize(), "L1 未启用时容量视图应恒为 0");
    }

    /**
     * 并发执行骨架：全员闸门同步后各自执行动作并统一回收异常。
     *
     * @param action 并发动作
     */
    private void runConcurrently(Runnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger done = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(THREADS);
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        assertTrue(startLatch.await(5, TimeUnit.SECONDS), "启动信号未就位");
                        action.run();
                        return null;
                    } finally {
                        done.incrementAndGet();
                    }
                }));
            }
            startLatch.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            assertEquals(THREADS, done.get(), "并发任务未全部完成");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "执行器未正常关闭");
        }
    }

    /**
     * 等待全部并发任务完成：断言失败经 Future 传播到主线程，超时兜底避免测试挂死。
     *
     * @param executor 执行器
     * @param futures  任务列表
     * @param done     完成计数
     * @param threads  线程总数
     */
    private void awaitAll(ExecutorService executor, List<Future<?>> futures, AtomicInteger done,
        int threads) throws Exception {
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        assertEquals(threads, done.get(), "并发任务未全部完成");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "执行器未正常关闭");
    }
}
