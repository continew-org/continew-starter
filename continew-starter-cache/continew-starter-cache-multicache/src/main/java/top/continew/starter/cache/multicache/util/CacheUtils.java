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

package top.continew.starter.cache.multicache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import top.continew.starter.cache.multicache.cache.MultiLevelCacheManager;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 多级缓存工具类（命令式缓存统一入口）
 *
 * <p>
 * 与 {@code @Cacheable} 等注解共用同一个 {@link MultiLevelCacheManager}，
 * 相同 cacheName + key 的数据互通，享受同一份 L1（Caffeine）+ L2（Redis）缓存。
 * </p>
 * <p>
 * 注意：分布式锁、计数器、限流等非缓存语义的 Redis 操作请使用
 * {@code RedisUtils} / {@code RedisLockUtils}，本工具类不提供此类原语。
 * </p>
 *
 * @author KAI
 * @since 3.0.0
 */
public class CacheUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheUtils.class);

    private static final AtomicReference<MultiLevelCacheManager> CACHE_MANAGER =
        new AtomicReference<>();

    private CacheUtils() {
    }

    /**
     * 初始化（由自动配置类注入，业务代码请勿调用）
     *
     * @param cacheManager 多级缓存管理器
     */
    public static void setCacheManager(MultiLevelCacheManager cacheManager) {
        CACHE_MANAGER.set(cacheManager);
    }

    /**
     * 获取缓存值
     *
     * @param <T>       值类型
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @return 缓存值（未命中返回 null）
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String cacheName, Object key) {
        Cache.ValueWrapper wrapper = getCache(cacheName).get(key);
        return wrapper == null ? null : (T) wrapper.get();
    }

    /**
     * 获取缓存值，未命中时回源加载并回填（带本地锁，防缓存击穿）
     *
     * @param <T>         值类型
     * @param cacheName  缓存名称
     * @param key         缓存键
     * @param valueLoader 回源加载器
     * @return 缓存值
     */
    public static <T> T get(String cacheName, Object key, Callable<T> valueLoader) {
        return getCache(cacheName).get(key, valueLoader);
    }

    /**
     * 写入缓存（同时写 L1 与 L2，并向集群广播 L1 失效）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     */
    public static void put(String cacheName, Object key, Object value) {
        getCache(cacheName).put(key, value);
    }

    /**
     * 移除缓存（同时清 L1 与 L2，并向集群广播 L1 失效）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    public static void evict(String cacheName, Object key) {
        getCache(cacheName).evict(key);
    }

    /**
     * 清空整个 cacheName 的缓存（同时清 L1 与 L2，并向集群广播）
     *
     * <p>
     * cacheName 未预注册且管理器未开启动态创建时，无缓存可清，此时输出 WARN 提示配置漂移
     * （如 cacheName 拼写错误），既有失败行为保持不变。
     * </p>
     *
     * @param cacheName 缓存名称
     */
    public static void evictAll(String cacheName) {
        Cache cache = requireCacheManager().getCache(cacheName);
        if (cache == null) {
            LOGGER.warn("[ContiNew Starter] - evictAll skipped: cache '{}' is not registered, "
                + "please check the cacheNames configuration.", cacheName);
            throw notRegisteredException(cacheName);
        }
        cache.clear();
    }

    private static Cache getCache(String cacheName) {
        Cache cache = requireCacheManager().getCache(cacheName);
        if (cache == null) {
            throw notRegisteredException(cacheName);
        }
        return cache;
    }

    /**
     * 构造 cacheName 未注册异常（evictAll 与其余操作共用同一失败消息）
     *
     * @param cacheName 缓存名称
     * @return 未注册异常
     */
    private static IllegalArgumentException notRegisteredException(String cacheName) {
        return new IllegalArgumentException("Cache '%s' is not registered.".formatted(cacheName));
    }

    /**
     * 获取已初始化的多级缓存管理器
     *
     * @return 多级缓存管理器
     */
    private static MultiLevelCacheManager requireCacheManager() {
        MultiLevelCacheManager manager = CACHE_MANAGER.get();
        if (manager == null) {
            throw new IllegalStateException(
                "CacheUtils is not initialized: no MultiLevelCacheManager "
                    + "bean is available, please check the multi-level cache configuration.");
        }
        return manager;
    }
}
