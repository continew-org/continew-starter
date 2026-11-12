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

package top.continew.starter.cache.redisson.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.cache.redisson.util.RedisUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * RedisUtils 真实 Redis 行为集成测试。
 *
 * <p>TTL 过期、原子写入与计数语义由 Redis 服务端决定，单测中的 mock 无法证明；
 * 此处对 Testcontainers 拉起的真实 Redis 逐项验证。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("RedisUtils 真实 Redis 行为集成测试")
class RedisUtilsCacheOperationIT extends RedisContainerSupport {

    private static final String KEY_PREFIX = "itest:op:";

    @Test
    @DisplayName("带 TTL 写入的值按真实过期时间失效，无 TTL 写入长期保留")
    void shouldExpireWrittenValueByRealTtl() {
        RedisUtils.set(KEY_PREFIX + "ttl", "v1", Duration.ofMillis(600));
        assertEquals("v1", RedisUtils.get(KEY_PREFIX + "ttl"));
        long remaining = RedisUtils.getTimeToLive(KEY_PREFIX + "ttl");
        assertTrue(remaining > 0 && remaining <= 600, "剩余 TTL 应在写入窗口内：" + remaining);

        RedisUtils.set(KEY_PREFIX + "no-ttl", "keep");
        assertEquals(-1L, RedisUtils.getTimeToLive(KEY_PREFIX + "no-ttl"));

        awaitKeyGone(KEY_PREFIX + "ttl");
        assertFalse(RedisUtils.exists(KEY_PREFIX + "ttl"));
        assertNull(RedisUtils.get(KEY_PREFIX + "ttl"));
        assertTrue(RedisUtils.exists(KEY_PREFIX + "no-ttl"));
    }

    /**
     * 轮询等待键按 TTL 真实过期：每轮循环一次真实 EXISTS 往返构成间隔，超时后
     * 由调用方断言兜底失败。
     *
     * @param key 目标键名
     */
    private static void awaitKeyGone(String key) {
        long deadline = System.currentTimeMillis() + 5000;
        boolean exists = RedisUtils.exists(key);
        while (exists && System.currentTimeMillis() < deadline) {
            exists = RedisUtils.exists(key);
        }
    }

    @Test
    @DisplayName("setIfAbsent 保持 Redis 原子语义：已存在的键不被覆盖")
    void shouldNotOverwriteExistingKeyWithSetIfAbsent() {
        assertTrue(
            RedisUtils.setIfAbsent(KEY_PREFIX + "ifabsent", "first", Duration.ofSeconds(30)));
        assertFalse(RedisUtils.setIfAbsent(KEY_PREFIX + "ifabsent", "second", Duration
            .ofSeconds(30)));
        assertEquals("first", RedisUtils.get(KEY_PREFIX + "ifabsent"));

        assertTrue(RedisUtils.delete(KEY_PREFIX + "ifabsent"));
        assertTrue(
            RedisUtils.setIfAbsent(KEY_PREFIX + "ifabsent", "third", Duration.ofSeconds(30)));
        assertEquals("third", RedisUtils.get(KEY_PREFIX + "ifabsent"));
    }

    @Test
    @DisplayName("计数器按真实 Redis INCR/DECR 语义返回运算后结果")
    void shouldIncrementAndDecrementCounterInRealRedis() {
        assertEquals(1L, RedisUtils.incr(KEY_PREFIX + "counter"));
        assertEquals(2L, RedisUtils.incr(KEY_PREFIX + "counter"));
        assertEquals(1L, RedisUtils.decr(KEY_PREFIX + "counter"));
        assertEquals("1", RedisUtils.get(KEY_PREFIX + "counter"));
    }

    @Test
    @DisplayName("keys 按模式扫描真实键，deleteByPattern 清理后不再命中")
    void shouldScanAndDeleteKeysByPattern() {
        RedisUtils.set(KEY_PREFIX + "scan:a", "1");
        RedisUtils.set(KEY_PREFIX + "scan:b", "2");
        Collection<String> keys = RedisUtils.keys(KEY_PREFIX + "scan:*");
        assertTrue(keys.containsAll(List.of(KEY_PREFIX + "scan:a", KEY_PREFIX + "scan:b")));

        RedisUtils.deleteByPattern(KEY_PREFIX + "scan:*");
        assertTrue(RedisUtils.keys(KEY_PREFIX + "scan:*").isEmpty());
    }

    @Test
    @DisplayName("expire 续期真实生效，getOrDefault 仅在键缺失时回落默认值")
    void shouldExpireKeyAndFallBackToDefaultOnMissingKey() {
        RedisUtils.set(KEY_PREFIX + "expire", "v");
        assertTrue(RedisUtils.expire(KEY_PREFIX + "expire", Duration.ofSeconds(60)));
        long remaining = RedisUtils.getTimeToLive(KEY_PREFIX + "expire");
        assertTrue(remaining > 0 && remaining <= 60_000, "续期后 TTL 应在窗口内：" + remaining);
        assertEquals("v", RedisUtils.getOrDefault(KEY_PREFIX + "expire", "fallback"));
        assertNull(RedisUtils.get(KEY_PREFIX + "missing"));
        assertEquals("fallback", RedisUtils.getOrDefault(KEY_PREFIX + "missing", "fallback"));
    }

    @Test
    @DisplayName("setIfExists 仅覆盖已存在的键，缺失键保持不写入")
    void shouldOnlyOverwriteExistingKeyWithSetIfExists() {
        RedisUtils.set(KEY_PREFIX + "ifexists", "origin");
        assertTrue(RedisUtils.setIfExists(KEY_PREFIX + "ifexists", "updated"));
        assertEquals("updated", RedisUtils.get(KEY_PREFIX + "ifexists"));
        assertFalse(RedisUtils.setIfExists(KEY_PREFIX + "ifexists-missing", "ignored", Duration
            .ofSeconds(30)));
        assertFalse(RedisUtils.exists(KEY_PREFIX + "ifexists-missing"));
    }

    @Test
    @DisplayName("hash 字段按真实 Redis Hash 语义读写、判存与删除")
    void shouldOperateHashFieldsInRealRedis() {
        RedisUtils.hSet(KEY_PREFIX + "hash", "field1", "v1");
        RedisUtils.hSet(KEY_PREFIX + "hash", "field2", "v2");
        assertEquals("v1", RedisUtils.hGet(KEY_PREFIX + "hash", "field1"));
        assertTrue(RedisUtils.hExists(KEY_PREFIX + "hash", "field2"));
        Map<String, String> all = RedisUtils.hGetAll(KEY_PREFIX + "hash");
        assertEquals(Map.of("field1", "v1", "field2", "v2"), all);
        assertEquals(1L, RedisUtils.hDel(KEY_PREFIX + "hash", "field1"));
        assertFalse(RedisUtils.hExists(KEY_PREFIX + "hash", "field1"));
    }

    @Test
    @DisplayName("zset 成员按真实分数排序、排名与规模，删除后不再命中")
    void shouldScoreAndRankSortedSetMembersInRealRedis() {
        assertTrue(RedisUtils.zAdd(KEY_PREFIX + "zset", "low", 1.0));
        assertTrue(RedisUtils.zAdd(KEY_PREFIX + "zset", "high", 9.0));
        assertTrue(RedisUtils.zAdd(KEY_PREFIX + "zset", "mid", 5.0));
        assertEquals(3, RedisUtils.zSize(KEY_PREFIX + "zset"));
        assertEquals(9.0, RedisUtils.zScore(KEY_PREFIX + "zset", "high"));
        assertEquals(0, RedisUtils.zRank(KEY_PREFIX + "zset", "low"));
        assertEquals(List.of("low", "mid", "high"), RedisUtils.zRangeByScore(KEY_PREFIX + "zset",
            0.0, 10.0));
        assertTrue(RedisUtils.zRemove(KEY_PREFIX + "zset", "mid"));
        assertEquals(2, RedisUtils.zSize(KEY_PREFIX + "zset"));
    }
}
