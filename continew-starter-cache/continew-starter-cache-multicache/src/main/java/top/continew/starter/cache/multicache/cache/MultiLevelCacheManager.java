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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import top.continew.starter.cache.multicache.autoconfigure.MultiLevelCacheProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * 多级缓存管理器
 *
 * <p>
 * 按 cacheName 创建 {@link MultiLevelCache}（Caffeine L1 + Redis L2）。
 * 支持预声明 cacheNames（启动即建好缓存）和按 cacheName 覆盖配置（specs）。
 * </p>
 *
 * @author KAI
 * @since 3.0.0
 */
public class MultiLevelCacheManager extends AbstractCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiLevelCacheManager.class);

    private final MultiLevelCacheProperties properties;
    private final CacheInvalidationPublisher invalidationPublisher;
    private final boolean dynamic;
    private final Collection<String> initialCacheNames;

    /**
     * L2 工厂（复用 RedisCacheManager 的创建逻辑，RedisCache 构造器为 protected）
     */
    private final RedisCacheFactory redisCacheFactory;

    public MultiLevelCacheManager(RedisCacheWriter cacheWriter,
        RedisCacheConfiguration defaultRedisCacheConfiguration,
        MultiLevelCacheProperties properties,
        CacheInvalidationPublisher invalidationPublisher,
        Collection<String> initialCacheNames) {
        this.properties = properties;
        this.invalidationPublisher = invalidationPublisher;
        this.redisCacheFactory = new RedisCacheFactory(cacheWriter, defaultRedisCacheConfiguration);
        this.dynamic = initialCacheNames == null;
        this.initialCacheNames = initialCacheNames == null
            ? Collections.emptyList()
            : new ArrayList<>(initialCacheNames);
        if (initialCacheNames != null) {
            // 校验 specs 中配置的 cacheName 是否已注册
            for (String specName : properties.getSpecs().keySet()) {
                if (!initialCacheNames.contains(specName)) {
                    LOGGER.warn("[ContiNew Starter] - Cache spec '{}' is not in registered cache "
                        + "names, please check your configuration.", specName);
                }
            }
        }
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        Collection<Cache> caches = new ArrayList<>(initialCacheNames.size());
        for (String cacheName : initialCacheNames) {
            caches.add(createMultiLevelCache(cacheName));
        }
        return caches;
    }

    @Override
    protected Cache getMissingCache(String name) {
        return dynamic ? createMultiLevelCache(name) : null;
    }

    private MultiLevelCache createMultiLevelCache(String cacheName) {
        MultiLevelCacheProperties.CacheSpec spec = properties.getSpecs().get(cacheName);
        MultiLevelCacheProperties.Local localConfig = spec != null && spec.getLocal() != null
            ? spec.getLocal()
            : properties.getLocal();
        // 构建 L2（RedisCache）：先应用全局 remote 配置，再按 cacheName 覆盖
        RedisCacheConfiguration redisConfig = redisCacheFactory.getDefaultCacheConfiguration();
        MultiLevelCacheProperties.Remote globalRemote = properties.getRemote();
        if (globalRemote.getExpireAfterWrite() != null) {
            redisConfig = redisConfig.entryTtl(globalRemote.getExpireAfterWrite());
        }
        if (!globalRemote.isCacheNullValues()) {
            redisConfig = redisConfig.disableCachingNullValues();
        }
        if (spec != null && spec.getRemote() != null) {
            MultiLevelCacheProperties.Remote remote = spec.getRemote();
            if (remote.getExpireAfterWrite() != null) {
                redisConfig = redisConfig.entryTtl(remote.getExpireAfterWrite());
            }
            if (!remote.isCacheNullValues()) {
                redisConfig = redisConfig.disableCachingNullValues();
            }
        }
        RedisCache redisCache = redisCacheFactory.createRedisCache(cacheName, redisConfig);
        LOGGER.debug("[ContiNew Starter] - Created multi-level cache '{}'.", cacheName);
        return new MultiLevelCache(cacheName, localConfig, redisCache, invalidationPublisher,
            properties.isDegradeOnRedisFailure());
    }

    /**
     * L2（RedisCache）工厂：继承 RedisCacheManager 以暴露 protected 的 createRedisCache
     *
     * <p>
     * 为什么用继承而不是组合：{@link RedisCacheManager#createRedisCache(String, RedisCacheConfiguration)}
     * 与 {@link RedisCacheManager#getDefaultCacheConfiguration()} 均为 protected，Spring 未提供
     * 公开的工厂入口。继承是访问这两个方法的最直接方式，语义上等价于「工厂方法适配器」，
     * 而非完整的 CacheManager 使用。若未来 Spring 开放公开工厂 API，应改为组合。
     * </p>
     */
    private static class RedisCacheFactory extends RedisCacheManager {

        RedisCacheFactory(RedisCacheWriter cacheWriter,
            RedisCacheConfiguration defaultCacheConfiguration) {
            super(cacheWriter, defaultCacheConfiguration);
        }

        // 两个 override 仅转发 super 调用：受 JLS 6.6.2 限制，外层类无法直接访问嵌套类
        // 从 RedisCacheManager 继承的 protected 方法，必须以桥接方法形式暴露给本类使用
        @SuppressWarnings("java:S1185")
        @Override
        protected RedisCache createRedisCache(String name,
            RedisCacheConfiguration cacheConfiguration) {
            return super.createRedisCache(name, cacheConfiguration);
        }

        @SuppressWarnings("java:S1185")
        @Override
        protected RedisCacheConfiguration getDefaultCacheConfiguration() {
            return super.getDefaultCacheConfiguration();
        }
    }
}
