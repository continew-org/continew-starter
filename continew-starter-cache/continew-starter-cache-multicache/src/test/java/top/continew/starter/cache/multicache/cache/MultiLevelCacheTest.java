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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * 多级缓存单元测试（内存版 L2，验证 L1/L2 回填、evict 广播、空值缓存、击穿保护、降级）
 *
 * @author KAI
 * @since 3.0.0
 */
class MultiLevelCacheTest {

    private static final String CACHE_NAME = "TEST:";

    private FakeRedisCacheWriter cacheWriter;
    private RedisCacheConfiguration redisCacheConfiguration;
    private MultiLevelCacheProperties properties;
    private RecordingPublisher publisher;
    private MultiLevelCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheWriter = new FakeRedisCacheWriter();
        redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .entryTtl(Duration.ofHours(2));
        properties = new MultiLevelCacheProperties();
        publisher = new RecordingPublisher();
        cacheManager = newManager(publisher);
        publisher.setCacheManager(cacheManager);
    }

    private MultiLevelCacheManager newManager(CacheInvalidationPublisher invalidationPublisher) {
        MultiLevelCacheManager manager = new MultiLevelCacheManager(cacheWriter,
            redisCacheConfiguration, properties, invalidationPublisher, List.of(CACHE_NAME));
        manager.afterPropertiesSet();
        return manager;
    }

    @Test
    @DisplayName("L1 miss → L2 命中 → 回填 L1")
    void testL2HitBackfillL1() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        // 由另一节点写入（只落 L2），本节点 L1 未命中
        RecordingPublisher otherPublisher = new RecordingPublisher();
        MultiLevelCacheManager otherManager = newManager(otherPublisher);
        otherManager.getCache(CACHE_NAME).put("k1", "v1");
        assertEquals("v1", cache.get("k1").get());
        // 第二次读取应命中 L1（L2 清空后仍能读到）
        otherManager.getCache(CACHE_NAME).evict("k1");
        assertEquals("v1", cache.get("k1").get());
    }

    @Test
    @DisplayName("put 同时写 L1 与 L2")
    void testPutWritesBothLevels() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("k1", "v1");
        assertEquals("v1", cache.get("k1").get());
        // 另一节点（L1 为空）也能读到，说明 L2 已写入
        MultiLevelCacheManager otherManager = newManager(new RecordingPublisher());
        assertEquals("v1", otherManager.getCache(CACHE_NAME).get("k1").get());
    }

    @Test
    @DisplayName("evict 清除两级并发布广播")
    void testEvictClearsBothAndPublishes() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("k1", "v1");
        cache.evict("k1");
        assertNull(cache.get("k1"));
        // L2 也已清除（另一节点读不到）
        MultiLevelCacheManager otherManager = newManager(new RecordingPublisher());
        assertNull(otherManager.getCache(CACHE_NAME).get("k1"));
        // put 与 evict 各广播一次
        assertEquals(2, publisher.messages.size());
        assertTrue(publisher.messages.get(1).contains(CACHE_NAME));
    }

    @Test
    @DisplayName("收到其他节点广播时仅清本节点 L1")
    void testBroadcastClearsLocalOnly() {
        // 模拟另一节点
        RecordingPublisher otherPublisher = new RecordingPublisher();
        MultiLevelCacheManager otherManager = newManager(otherPublisher);
        otherPublisher.setCacheManager(otherManager);
        // 本节点与另一节点均缓存
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("k1", "v1");
        otherManager.getCache(CACHE_NAME).get("k1");
        // 本节点 evict → 广播消息由"另一节点"的 publisher 处理（替换 nodeId 模拟非本节点）
        cache.evict("k1");
        String message = publisher.messages.get(publisher.messages.size() - 1)
            .replaceFirst("^[^|]+", "other-node");
        otherPublisher.onMessage(new FakeMessage(message), null);
        // 另一节点 L1 已清，重新 get 回源 L2（L2 也已清，应为 miss）
        assertNull(otherManager.getCache(CACHE_NAME).get("k1"));
    }

    @Test
    @DisplayName("缓存空值（防穿透）")
    void testCacheNullValues() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("nullKey", null);
        Cache.ValueWrapper wrapper = cache.get("nullKey");
        assertNotNull(wrapper);
        assertNull(wrapper.get());
    }

    @Test
    @DisplayName("另一节点写入的空值在本节点命中（L2 空值不穿透）")
    void testL2NullValueHitCrossNode() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        // 另一节点只落 L2（本节点 L1 为空）
        MultiLevelCacheManager otherManager = newManager(new RecordingPublisher());
        otherManager.getCache(CACHE_NAME).put("nullKey", null);
        Cache.ValueWrapper wrapper = cache.get("nullKey");
        assertNotNull(wrapper, "L2 中的空值应视为命中而不是未命中");
        assertNull(wrapper.get());
    }

    @Test
    @DisplayName("非 String 键与 L2 键空间对齐，广播失效可命中 L1")
    void testNonStringKeyBroadcastAlignment() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put(123L, "v1");
        // 另一节点更新 L2，本节点 L1 仍是旧值
        MultiLevelCacheManager otherManager = newManager(new RecordingPublisher());
        otherManager.getCache(CACHE_NAME).put(123L, "v2");
        assertEquals("v1", cache.get(123L).get());
        // 广播消息中的键为字符串形式，应能清掉本地 L1 中归一化键的条目
        publisher.onMessage(new FakeMessage("other-node|false|" + CACHE_NAME + "|123"), null);
        assertEquals("v2", cache.get(123L).get());
    }

    @Test
    @DisplayName("putIfAbsent 委托 L2 原子操作，已存在时返回现值并回填 L1")
    void testPutIfAbsent() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertNull(cache.putIfAbsent("k1", "v1"));
        Cache.ValueWrapper existing = cache.putIfAbsent("k1", "v2");
        assertNotNull(existing);
        assertEquals("v1", existing.get());
        assertEquals("v1", cache.get("k1").get());
        // 另一节点（L1 为空）读到的也是首写值，说明 L2 语义正确
        MultiLevelCacheManager otherManager = newManager(new RecordingPublisher());
        assertEquals("v1", otherManager.getCache(CACHE_NAME).get("k1").get());
    }

    @Test
    @DisplayName("put 写入成功后广播 L1 失效")
    void testPutBroadcastsInvalidation() {
        cacheManager.getCache(CACHE_NAME).put("k1", "v1");
        assertEquals(List.of("node|false|" + CACHE_NAME + "|k1"), publisher.messages);
    }

    @Test
    @DisplayName("另一节点 put 的广播使本节点 L1 失效，读到新值而非脏值")
    void testPutBroadcastClearsOtherNodeL1() {
        RecordingPublisher otherPublisher = new RecordingPublisher();
        MultiLevelCacheManager otherManager = newManager(otherPublisher);
        otherPublisher.setCacheManager(otherManager);
        Cache other = otherManager.getCache(CACHE_NAME);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        other.put("k1", "v1");
        assertEquals("v1", cache.get("k1").get());
        // 另一节点更新 L2 并广播，本节点回填的 L1 必须失效
        other.put("k1", "v2");
        String message = otherPublisher.messages.get(1).replaceFirst("^[^|]+", "other-node");
        publisher.onMessage(new FakeMessage(message), null);
        assertEquals("v2", cache.get("k1").get());
    }

    @Test
    @DisplayName("putIfAbsent 仅在实际写入 L2 时广播")
    void testPutIfAbsentBroadcastsOnlyWhenWritten() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertNull(cache.putIfAbsent("k1", "v1"));
        assertEquals(1, publisher.messages.size());
        assertNotNull(cache.putIfAbsent("k1", "v2"));
        assertEquals(1, publisher.messages.size(), "L2 未变更时无需广播");
    }

    @Test
    @DisplayName("降级模式下 putIfAbsent 明确失败，不伪装成写入成功")
    void testPutIfAbsentFailsWhenDegraded() {
        cacheWriter.setAvailable(false);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cache
            .putIfAbsent("k1", "v1"));
        assertTrue(exception.getMessage().contains("mutual exclusion"));
        assertNull(cache.get("k1"), "失败时不应写入本地 L1");
    }

    @Test
    @DisplayName("关闭降级时 putIfAbsent 透传 L2 异常")
    void testPutIfAbsentPropagatesRemoteFailure() {
        properties.setDegradeOnRedisFailure(false);
        cacheManager = newManager(publisher);
        cacheWriter.setAvailable(false);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> cache
            .putIfAbsent("k1", "v1"));
        assertEquals("Redis is down", exception.getMessage());
    }

    @Test
    @DisplayName("get(loader) 并发回源只执行一次（击穿保护）")
    void testPenetrationProtection() throws Exception {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        AtomicInteger loadCount = new AtomicInteger();
        int threads = 16;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch loaderStarted = new CountDownLatch(1);
        CountDownLatch loaderGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS), "回源启动信号未就位");
                    return cache.get("hotKey", () -> {
                        // 用闸门同步代替 sleep：首个回源线程挂起时，其余线程已被分段锁挡住
                        loadCount.incrementAndGet();
                        loaderStarted.countDown();
                        loaderGate.await(5, TimeUnit.SECONDS);
                        return "loaded";
                    });
                } finally {
                    doneLatch.countDown();
                }
            }));
        }
        startLatch.countDown();
        // 等首个线程真正进入回源后再放行，避免时序抖动导致击穿保护未被触发
        assertTrue(loaderStarted.await(5, TimeUnit.SECONDS), "回源加载器未被调用，击穿保护或加载路径存在缺陷");
        loaderGate.countDown();
        // 统一回收 worker 结果：断言失败经 Future 传播到主线程，超时兜底避免测试挂死
        for (Future<Object> future : futures) {
            assertEquals("loaded", future.get(5, TimeUnit.SECONDS));
        }
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "并发线程未全部完成");
        executor.shutdown();
        assertEquals(1, loadCount.get(), "回源加载应只执行一次");
    }

    @Test
    @DisplayName("L2 宕机时降级为仅 L1")
    void testDegradeOnRedisFailure() {
        cacheWriter.setAvailable(false);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("k1", "v1"); // 不抛异常
        assertEquals("v1", cache.get("k1").get()); // 走 L1
        cache.evict("k1"); // 不抛异常
        assertNull(cache.get("k1"));
    }

    @Test
    @DisplayName("specs 关闭 L1 后退化为纯 Redis 缓存")
    void testDisableLocalCacheBySpec() {
        MultiLevelCacheProperties.CacheSpec spec = new MultiLevelCacheProperties.CacheSpec();
        MultiLevelCacheProperties.Local local = new MultiLevelCacheProperties.Local();
        local.setLimit(0);
        spec.setLocal(local);
        properties.getSpecs().put(CACHE_NAME, spec);
        cacheManager = newManager(publisher);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("k1", "v1");
        // L1 关闭时，get 仍需返回 L2 中的值
        assertEquals("v1", cache.get("k1").get());
    }

    @Test
    @DisplayName("全局 remote TTL/空值策略应用到 L2")
    void testGlobalRemoteConfigApplied() {
        properties.getRemote().setExpireAfterWrite(Duration.ofMinutes(10));
        properties.getRemote().setCacheNullValues(false);
        cacheManager = newManager(publisher);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("k1", "v1");
        assertEquals(Duration.ofMinutes(10), cacheWriter.lastTtl.get());
        // 关闭空值缓存后写入 null 应拒绝（与 RedisCache 语义一致）
        assertThrows(IllegalArgumentException.class, () -> cache.put("nullKey", null));
    }

    /**
     * 广播消息记录器
     */
    private static class RecordingPublisher extends CacheInvalidationPublisher {

        private final List<String> messages = new ArrayList<>();

        RecordingPublisher() {
            super(null, "test-topic");
        }

        @Override
        public void publish(String cacheName, Object key, boolean allEntries) {
            messages.add("node|" + allEntries + "|" + cacheName + "|" + key);
        }
    }
}
