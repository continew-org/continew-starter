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
import static org.junit.jupiter.api.Assertions.fail;

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
        assertEquals(1, publisher.messages.size());
        assertTrue(publisher.messages.get(0).contains(CACHE_NAME));
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
        String message = publisher.messages.get(0).replaceFirst("^[^|]+", "other-node");
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
    @DisplayName("get(loader) 并发回源只执行一次（击穿保护）")
    void testPenetrationProtection() throws InterruptedException {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        AtomicInteger loadCount = new AtomicInteger();
        int threads = 16;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Object value = cache.get("hotKey", () -> {
                        loadCount.incrementAndGet();
                        Thread.sleep(50);
                        return "loaded";
                    });
                    assertEquals("loaded", value);
                } catch (Exception e) {
                    fail(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
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
        cacheManager.getCache(CACHE_NAME).put("k1", "v1");
        assertEquals(Duration.ofMinutes(10), cacheWriter.lastTtl.get());
        // 关闭空值缓存后写入 null 应拒绝（与 RedisCache 语义一致）
        assertThrows(IllegalArgumentException.class, () -> cacheManager.getCache(CACHE_NAME)
            .put("nullKey", null));
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
