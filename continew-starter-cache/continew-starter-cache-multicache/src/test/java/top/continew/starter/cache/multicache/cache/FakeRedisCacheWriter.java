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

import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存版 RedisCacheWriter（可模拟宕机，记录最近一次写入 TTL），供多级缓存相关测试共用
 *
 * @author Charles7c
 */
public class FakeRedisCacheWriter implements RedisCacheWriter {

    final Map<String, byte[]> store = new ConcurrentHashMap<>();
    final AtomicReference<Duration> lastTtl = new AtomicReference<>();
    private volatile boolean available = true;

    void setAvailable(boolean available) {
        this.available = available;
    }

    private void checkAvailable() {
        if (!available) {
            throw new RuntimeException("Redis is down");
        }
    }

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        checkAvailable();
        lastTtl.set(ttl);
        store.put(new String(key), value);
    }

    @Override
    public byte[] get(String name, byte[] key) {
        checkAvailable();
        return store.get(new String(key));
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        checkAvailable();
        lastTtl.set(ttl);
        return store.putIfAbsent(new String(key), value);
    }

    @Override
    public void remove(String name, byte[] key) {
        checkAvailable();
        store.remove(new String(key));
    }

    @Override
    public void evict(String name, byte[] key) {
        remove(name, key);
    }

    @Override
    public void clean(String name, byte[] pattern) {
        checkAvailable();
        store.clear();
    }

    @Override
    public void clear(String name, byte[] pattern) {
        clean(name, pattern);
    }

    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        return CompletableFuture.completedFuture(get(name, key));
    }

    @Override
    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, Duration ttl) {
        put(name, key, value, ttl);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void clearStatistics(String name) {
    }

    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return new CacheStatistics() {

            @Override
            public String getCacheName() {
                return cacheName;
            }

            @Override
            public long getPuts() {
                return 0;
            }

            @Override
            public long getGets() {
                return 0;
            }

            @Override
            public long getHits() {
                return 0;
            }

            @Override
            public long getMisses() {
                return 0;
            }

            @Override
            public long getDeletes() {
                return 0;
            }

            @Override
            public long getLockWaitDuration(TimeUnit unit) {
                return 0;
            }

            @Override
            public Instant getSince() {
                return Instant.EPOCH;
            }

            @Override
            public Instant getLastReset() {
                return Instant.EPOCH;
            }
        };
    }

    @Override
    public RedisCacheWriter withStatisticsCollector(
        CacheStatisticsCollector cacheStatisticsCollector) {
        return this;
    }
}
