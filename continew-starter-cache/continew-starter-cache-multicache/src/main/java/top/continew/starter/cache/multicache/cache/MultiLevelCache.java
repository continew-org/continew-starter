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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.cache.RedisCache;
import top.continew.starter.cache.multicache.autoconfigure.MultiLevelCacheProperties;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多级缓存（Caffeine L1 + Redis L2）
 *
 * <p>
 * 读取：L1 命中直接返回；L1 未命中查询 L2，命中则回填 L1。
 * 写入：先写 L2 再写 L1（L1 过期时间不超过 L2），写成后广播 L1 失效消息。
 * 失效：清除 L1、L2 并向集群广播 L1 失效消息。
 * 降级：L2 故障时读、写、失效降级为仅本地缓存；但 putIfAbsent 的跨节点互斥无法在本地实现，
 * 降级期间一律抛出异常（见 {@link #putIfAbsent(Object, Object)}）。
 * </p>
 *
 * @author KAI
 * @since 3.0.0
 */
public class MultiLevelCache extends AbstractValueAdaptingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiLevelCache.class);

    /**
     * 缓存名称
     */
    private final String name;

    /**
     * 本地缓存（L1），limit <= 0 时为 null（该 cacheName 退化为纯 Redis 缓存）
     */
    private final Cache<Object, Object> localCache;

    /**
     * 远程缓存（L2）
     */
    private final RedisCache redisCache;

    /**
     * 失效广播器（null 表示不广播）
     */
    private final CacheInvalidationPublisher invalidationPublisher;

    /**
     * Redis 不可用时是否降级为仅本地缓存
     */
    private final boolean degradeOnRedisFailure;

    /**
     * 回源加载锁（分段锁，防缓存击穿）
     */
    private final Lock[] loadLocks = new Lock[32];

    public MultiLevelCache(String name,
        MultiLevelCacheProperties.Local localConfig,
        RedisCache redisCache,
        CacheInvalidationPublisher invalidationPublisher,
        boolean degradeOnRedisFailure) {
        super(redisCache.getCacheConfiguration().getAllowCacheNullValues());
        this.name = name;
        this.redisCache = redisCache;
        this.invalidationPublisher = invalidationPublisher;
        this.degradeOnRedisFailure = degradeOnRedisFailure;
        this.localCache =
            localConfig.getLimit() > 0 ? createLocalCache(localConfig, redisCache) : null;
        for (int i = 0; i < loadLocks.length; i++) {
            loadLocks[i] = new ReentrantLock();
        }
    }

    /**
     * 构建本地缓存（L1 过期时间取 local/remote 两者较小值，避免 L1 比 L2 活得久）
     *
     * @param localConfig 本地缓存配置
     * @param redisCache  远程缓存（用于求取 L2 TTL）
     * @return 本地缓存实例
     */
    private static Cache<Object, Object> createLocalCache(
        MultiLevelCacheProperties.Local localConfig,
        RedisCache redisCache) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
            .maximumSize(localConfig.getLimit())
            .recordStats();
        Duration localTtl = localConfig.getExpireAfterWrite();
        Duration ttl = minTtl(localTtl, resolveRemoteTtl(redisCache));
        if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            builder.expireAfterWrite(ttl);
        }
        return builder.build();
    }

    /**
     * 求取 L2 的 TTL；自定义 TTL 函数无法用哑键值求值时返回 null（回退为仅按本地配置）
     *
     * @param redisCache 远程缓存
     * @return L2 的 TTL，无法求值时返回 null
     */
    private static Duration resolveRemoteTtl(RedisCache redisCache) {
        try {
            return redisCache.getCacheConfiguration().getTtlFunction()
                .getTimeToLive(new Object(), new Object());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * L1 键与 L2 键空间对齐：L2 键经 StringRedisSerializer 按 {@code toString()} 序列化，
     * L1 同样按字符串形式存取，使 {@code Long 123} 与 {@code "123"} 在两级命中同一条目，
     * 广播失效消息（键为字符串）也能精确匹配本地条目
     *
     * @param key 原始缓存键
     * @return 归一化后的 L1 键
     */
    private static Object normalizeKey(Object key) {
        return String.valueOf(key);
    }

    private static Duration minTtl(Duration localTtl, Duration remoteTtl) {
        if (localTtl == null) {
            return remoteTtl;
        }
        if (remoteTtl == null || remoteTtl.isNegative() || remoteTtl.isZero()) {
            return localTtl;
        }
        return localTtl.compareTo(remoteTtl) <= 0 ? localTtl : remoteTtl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RedisCache getNativeCache() {
        return redisCache;
    }

    @Override
    protected Object lookup(Object key) {
        // 1. 查询 L1
        if (localCache != null) {
            Object value = localCache.getIfPresent(normalizeKey(key));
            if (value != null) {
                return value;
            }
        }
        // 2. 查询 L2，命中则回填 L1
        Object value = lookupRemote(key);
        if (value != null) {
            putLocal(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            @SuppressWarnings("unchecked")
            T value = (T) wrapper.get();
            return value;
        }
        // 分段锁回源，防缓存击穿
        Lock lock = loadLocks[(normalizeKey(key).hashCode() & 0x7fffffff) % loadLocks.length];
        lock.lock();
        try {
            wrapper = get(key);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                T value = (T) wrapper.get();
                return value;
            }
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Object key, Object value) {
        // 值转换在写 L2 之前完成：空值策略不满足时直接抛出，不当作 L2 故障降级
        Object storeValue = toStoreValue(value);
        boolean remoteWritten = false;
        try {
            redisCache.put(key, storeValue);
            remoteWritten = true;
        } catch (RuntimeException e) {
            handleRemoteFailure("put", key, e);
        }
        putLocal(key, value);
        if (remoteWritten) {
            // L2 已变更，其他节点 L1 立即失效，避免最长 min(L1 TTL, L2 TTL) 的脏读窗口
            publishInvalidation(key, false);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Object storeValue = toStoreValue(value);
        ValueWrapper existing;
        try {
            // 委托 L2 的原子 SETNX，保证跨节点语义
            existing = redisCache.putIfAbsent(key, storeValue);
        } catch (RuntimeException e) {
            throw putIfAbsentFailure(key, e);
        }
        if (existing == null) {
            putLocal(key, value);
            publishInvalidation(key, false);
            return null;
        }
        // 已有值时回填 L1，避免后续读全部落到 L2；L2 未变更，无需广播
        putLocal(key, existing.get());
        return existing;
    }

    @Override
    public void evict(Object key) {
        try {
            redisCache.evict(key);
        } catch (RuntimeException e) {
            handleRemoteFailure("evict", key, e);
        }
        evictLocal(key);
        publishInvalidation(key, false);
    }

    @Override
    public void clear() {
        try {
            redisCache.clear();
        } catch (RuntimeException e) {
            handleRemoteFailure("clear", null, e);
        }
        clearLocal();
        publishInvalidation(null, true);
    }

    /**
     * 仅清除本地 L1（收到其他节点广播时调用，不级联再广播）
     *
     * @param key 缓存键
     */
    public void evictLocal(Object key) {
        if (localCache != null) {
            localCache.invalidate(normalizeKey(key));
        }
    }

    /**
     * 仅清空本地 L1
     */
    public void clearLocal() {
        if (localCache != null) {
            localCache.invalidateAll();
        }
    }

    /**
     * 是否启用了本地 L1
     *
     * @return 是否启用了本地 L1
     */
    public boolean isLocalEnabled() {
        return localCache != null;
    }

    private Object lookupRemote(Object key) {
        try {
            ValueWrapper wrapper = redisCache.get(key);
            if (wrapper == null) {
                return null;
            }
            // 空值命中保持“已缓存”语义（包装为 NullValue），避免多节点场景下空值反复穿透
            Object value = wrapper.get();
            return value != null ? value : NullValue.INSTANCE;
        } catch (RuntimeException e) {
            handleRemoteFailure("get", key, e);
            return null;
        }
    }

    private void putLocal(Object key, Object value) {
        if (localCache != null) {
            localCache.put(normalizeKey(key), toStoreValue(value));
        }
    }

    private void publishInvalidation(Object key, boolean allEntries) {
        if (invalidationPublisher != null) {
            invalidationPublisher.publish(name, key, allEntries);
        }
    }

    private void handleRemoteFailure(String operation, Object key, RuntimeException e) {
        if (degradeOnRedisFailure) {
            LOGGER.warn("[ContiNew Starter] - Multi-level cache L2 (Redis) '{}' failed, key: {}, "
                + "degraded to local cache only.", operation, key, e);
        } else {
            throw e;
        }
    }

    /**
     * putIfAbsent 的 L2 故障处理：降级只能保证本地语义，无法保证跨节点互斥，因此一律失败
     *
     * <p>
     * 降级期间若返回「写入成功」，多个节点会各自认为自己抢到了键，分布式互斥静默丢失。
     * 这里明确抛出异常，由调用方决定是否中止业务。
     * </p>
     *
     * @param key   缓存键
     * @param cause L2 抛出的异常
     * @return 待调用方抛出的异常（降级关闭时原样透传 L2 异常）
     */
    private RuntimeException putIfAbsentFailure(Object key, RuntimeException cause) {
        if (!degradeOnRedisFailure) {
            return cause;
        }
        return new IllegalStateException(("Multi-level cache putIfAbsent requires the atomic "
            + "set-if-absent of L2 (Redis), degraded mode cannot guarantee cluster-wide mutual "
            + "exclusion. cacheName: %s, key: %s").formatted(name, key), cause);
    }
}
