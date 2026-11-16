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

package top.continew.starter.cache.redisson.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RListAsync;
import org.redisson.api.RMap;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RTopic;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.options.KeysScanOptions;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * RedisUtils 纯逻辑分支单测。
 *
 * <p>
 * RedisUtils 的 CLIENT 为 {@code static final}，在类初始化时经 {@link SpringUtil#getBean(Class)}
 * 取值。本测试在首次引用 RedisUtils 前通过静态模拟注入受控的 {@link RedissonClient}，
 * 触发初始化后立即关闭静态模拟（final 字段已固化为 mock 引用，不受后续关闭影响）。
 * 断言统一校验转发到 Redisson 代理的方法、参数与返回值透传，不启动真实 Redis。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("RedisUtils 纯逻辑分支单测")
class RedisUtilsTest {

    private static RedissonClient client;
    private static RBucket<String> bucket;
    private static RList<String> list;
    private static RMap<String, String> map;
    private static RScoredSortedSet<String> zSet;
    private static RAtomicLong atomicLong;
    private static RRateLimiter rateLimiter;
    private static RTopic topic;
    private static RKeys keys;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void injectClient() {
        client = mock(RedissonClient.class);
        bucket = mock(RBucket.class);
        list = mock(RList.class);
        map = mock(RMap.class);
        zSet = mock(RScoredSortedSet.class);
        atomicLong = mock(RAtomicLong.class);
        rateLimiter = mock(RRateLimiter.class);
        topic = mock(RTopic.class);
        keys = mock(RKeys.class);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(client);
            RedisUtils.getClient();
        }
        doReturn(bucket).when(client).getBucket(anyString());
        doReturn(list).when(client).getList(anyString());
        doReturn(map).when(client).getMap(anyString());
        doReturn(zSet).when(client).getScoredSortedSet(anyString());
        doReturn(atomicLong).when(client).getAtomicLong(anyString());
        doReturn(rateLimiter).when(client).getRateLimiter(anyString());
        doReturn(topic).when(client).getTopic(anyString());
        when(client.getKeys()).thenReturn(keys);
    }

    @BeforeEach
    void resetInvocations() {
        clearInvocations(client, bucket, list, map, zSet, atomicLong, rateLimiter, topic, keys);
    }

    @Nested
    @DisplayName("客户端与键格式化")
    class ClientAndKeyFormat {

        @Test
        @DisplayName("getClient 返回注入的 RedissonClient")
        void shouldReturnInjectedClient() {
            assertThat(RedisUtils.getClient()).isSameAs(client);
        }

        @Test
        @DisplayName("formatKey 以冒号拼接多个非空子键，过滤 null 与空白，空数组返回空串")
        void shouldFormatKeyFilteringBlanks() {
            assertThat(RedisUtils.formatKey("a", "b", "c")).isEqualTo("a:b:c");
            assertThat(RedisUtils.formatKey("a", null, "", "  ", "b")).isEqualTo("a:b");
            assertThat(RedisUtils.formatKey()).isEmpty();
            assertThat(RedisUtils.formatKey("only")).isEqualTo("only");
        }
    }

    @Nested
    @DisplayName("缓存读写与过期边界")
    class CacheOperations {

        @Test
        @DisplayName("set 两个重载转发值，Duration 原样透传")
        void shouldSetWithOptionalDuration() {
            RedisUtils.set("k", "v");
            verify(bucket).set("v");
            RedisUtils.set("k", "v", Duration.ofSeconds(30));
            verify(bucket).set("v", Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("set 对负数与零 Duration 边界原样透传")
        void shouldSetWithBoundaryDurations() {
            RedisUtils.set("k", "v", Duration.ofSeconds(-1));
            verify(bucket).set("v", Duration.ofSeconds(-1));
            RedisUtils.set("k", "v", Duration.ZERO);
            verify(bucket).set("v", Duration.ZERO);
        }

        @Test
        @DisplayName("get 返回 bucket.get 的值")
        void shouldGetValue() {
            when(bucket.get()).thenReturn("cached");
            String value = RedisUtils.get("k");
            assertThat(value).isEqualTo("cached");
            verify(client).getBucket("k");
        }

        @Test
        @DisplayName("getOrDefault 在值缺失时返回默认值，存在时返回原值")
        void shouldGetOrDefaultBothBranches() {
            when(bucket.get()).thenReturn(null, "real");
            String fallback = RedisUtils.getOrDefault("k", "fallback");
            String present = RedisUtils.getOrDefault("k", "fallback");
            assertThat(fallback).isEqualTo("fallback");
            assertThat(present).isEqualTo("real");
        }

        @Test
        @DisplayName("expire 对正数、零、负数 Duration 透传并透传返回布尔值")
        void shouldExpireWithBoundaryDurations() {
            when(bucket.expire(Duration.ofMinutes(1))).thenReturn(true);
            when(bucket.expire(Duration.ZERO)).thenReturn(false);
            when(bucket.expire(Duration.ofSeconds(-1))).thenReturn(true);
            assertThat(RedisUtils.expire("k", Duration.ofMinutes(1))).isTrue();
            assertThat(RedisUtils.expire("k", Duration.ZERO)).isFalse();
            assertThat(RedisUtils.expire("k", Duration.ofSeconds(-1))).isTrue();
            verify(bucket).expire(Duration.ofMinutes(1));
            verify(bucket).expire(Duration.ZERO);
            verify(bucket).expire(Duration.ofSeconds(-1));
        }

        @Test
        @DisplayName("getTimeToLive 返回 bucket 剩余 TTL")
        void shouldReturnTimeToLive() {
            when(bucket.remainTimeToLive()).thenReturn(1234L);
            assertThat(RedisUtils.getTimeToLive("k")).isEqualTo(1234L);
            verify(bucket).remainTimeToLive();
        }
    }

    @Nested
    @DisplayName("互斥写入与计数")
    class MutexAndCounter {

        @Test
        @DisplayName("setIfAbsent 两个重载透传值及 Duration，覆盖真假分支")
        void shouldSetIfAbsentBothBranches() {
            Duration ttl = Duration.ofSeconds(5);
            when(bucket.setIfAbsent("v")).thenReturn(true);
            when(bucket.setIfAbsent("v", ttl)).thenReturn(false);
            assertThat(RedisUtils.setIfAbsent("k", "v")).isTrue();
            assertThat(RedisUtils.setIfAbsent("k", "v", ttl)).isFalse();
            verify(bucket).setIfAbsent("v");
            verify(bucket).setIfAbsent("v", ttl);
        }

        @Test
        @DisplayName("setIfExists 两个重载透传值及 Duration，覆盖真假分支")
        void shouldSetIfExistsBothBranches() {
            Duration ttl = Duration.ofSeconds(5);
            when(bucket.setIfExists("v")).thenReturn(false);
            when(bucket.setIfExists("v", ttl)).thenReturn(true);
            assertThat(RedisUtils.setIfExists("k", "v")).isFalse();
            assertThat(RedisUtils.setIfExists("k", "v", ttl)).isTrue();
            verify(bucket).setIfExists("v");
            verify(bucket).setIfExists("v", ttl);
        }

        @Test
        @DisplayName("incr 与 decr 返回原子运算结果")
        void shouldIncrementAndDecrement() {
            when(atomicLong.incrementAndGet()).thenReturn(7L);
            when(atomicLong.decrementAndGet()).thenReturn(3L);
            assertThat(RedisUtils.incr("k")).isEqualTo(7L);
            assertThat(RedisUtils.decr("k")).isEqualTo(3L);
            verify(atomicLong).incrementAndGet();
            verify(atomicLong).decrementAndGet();
        }
    }

    @Nested
    @DisplayName("删除与键扫描")
    class DeleteAndScan {

        @Test
        @DisplayName("delete 与 deleteByPattern 透传键和模式")
        void shouldDeleteAndDeleteByPattern() {
            when(bucket.delete()).thenReturn(true);
            when(keys.deleteByPattern("pat:*")).thenReturn(2L);
            assertThat(RedisUtils.delete("k")).isTrue();
            RedisUtils.deleteByPattern("pat:*");
            verify(bucket).delete();
            verify(keys).deleteByPattern("pat:*");
        }

        @Test
        @DisplayName("exists 按 countExists 是否大于零返回真假")
        void shouldCheckExistsBothBranches() {
            when(keys.countExists("k")).thenReturn(1L, 0L);
            assertThat(RedisUtils.exists("k")).isTrue();
            assertThat(RedisUtils.exists("k")).isFalse();
            verify(keys, times(2)).countExists("k");
        }

        @Test
        @DisplayName("keys 将模式设置进 KeysScanOptions 并返回扫描结果")
        void shouldScanKeysByPattern() {
            KeysScanOptions options = mock(KeysScanOptions.class);
            when(options.pattern("pat:*")).thenReturn(options);
            when(keys.getKeysStream(options)).thenReturn(Stream.of("pat:a", "pat:b"));
            try (MockedStatic<KeysScanOptions> staticOptions = mockStatic(KeysScanOptions.class)) {
                staticOptions.when(KeysScanOptions::defaults).thenReturn(options);
                assertThat(RedisUtils.keys("pat:*")).containsExactly("pat:a", "pat:b");
                verify(options).pattern("pat:*");
                staticOptions.verify(KeysScanOptions::defaults);
            }
        }
    }

    @Nested
    @DisplayName("List 缓存")
    class ListCache {

        @Test
        @DisplayName("setList 转发全部元素到 getList(key).addAll")
        void shouldSetList() {
            List<String> values = List.of("a", "b");
            RedisUtils.setList("k", values);
            verify(client).getList("k");
            verify(list).addAll(values);
        }

        @Test
        @DisplayName("setList 带 Duration 时经批处理异步写入并透传过期时间")
        @SuppressWarnings("unchecked")
        void shouldSetListWithDurationViaBatch() {
            RBatch batch = mock(RBatch.class);
            RListAsync<String> asyncList = mock(RListAsync.class);
            when(client.createBatch()).thenReturn(batch);
            doReturn(asyncList).when(batch).getList("k");
            List<String> values = List.of("a", "b");
            Duration ttl = Duration.ofMinutes(2);
            RedisUtils.setList("k", values, ttl);
            verify(asyncList).addAllAsync(values);
            verify(asyncList).expireAsync(ttl);
            verify(batch).execute();
        }

        @Test
        @DisplayName("getList 返回 readAll 的结果")
        void shouldGetList() {
            when(list.readAll()).thenReturn(List.of("a", "b"));
            assertThat(RedisUtils.getList("k")).containsExactly("a", "b");
            verify(client).getList("k");
        }
    }

    @Nested
    @DisplayName("Hash 缓存")
    class HashCache {

        @Test
        @DisplayName("hSet 与 hGet 透传字段和值")
        void shouldHSetAndHGet() {
            when(map.get("f")).thenReturn("v");
            RedisUtils.hSet("k", "f", "v");
            String value = RedisUtils.hGet("k", "f");
            verify(client, times(2)).getMap("k");
            verify(map).put("f", "v");
            verify(map).get("f");
            assertThat(value).isEqualTo("v");
        }

        @Test
        @DisplayName("hGetAll 返回整个 Map")
        void shouldHGetAll() {
            Map<String, String> all = Map.of("f1", "v1", "f2", "v2");
            when(map.readAllMap()).thenReturn(all);
            assertThat(RedisUtils.hGetAll("k")).isEqualTo(all);
            verify(map).readAllMap();
        }

        @Test
        @DisplayName("hExists 与 hDel 透传字段并返回结果")
        void shouldHExistsAndHDel() {
            when(map.containsKey("f")).thenReturn(true);
            when(map.fastRemove("f1", "f2")).thenReturn(2L);
            assertThat(RedisUtils.hExists("k", "f")).isTrue();
            assertThat(RedisUtils.hDel("k", "f1", "f2")).isEqualTo(2L);
            verify(map).containsKey("f");
            verify(map).fastRemove("f1", "f2");
        }
    }

    @Nested
    @DisplayName("ZSet 缓存")
    class ZSetCache {

        @Test
        @DisplayName("zAdd、zScore、zRank、zSize 透传参数并返回结果")
        void shouldAddScoreRankAndSize() {
            when(zSet.add(5.0, "v")).thenReturn(true);
            when(zSet.getScore("v")).thenReturn(9.0);
            when(zSet.rank("v")).thenReturn(2);
            when(zSet.size()).thenReturn(4);
            assertThat(RedisUtils.zAdd("k", "v", 5.0)).isTrue();
            assertThat(RedisUtils.zScore("k", "v")).isEqualTo(9.0);
            assertThat(RedisUtils.zRank("k", "v")).isEqualTo(2);
            assertThat(RedisUtils.zSize("k")).isEqualTo(4);
            verify(zSet).add(5.0, "v");
            verify(zSet).getScore("v");
            verify(zSet).rank("v");
            verify(zSet).size();
        }

        @Test
        @DisplayName("zRemove、zRemoveRangeByScore、zRemoveRangeByRank 透传参数并返回删除数")
        void shouldRemoveByValueScoreAndRank() {
            when(zSet.remove("v")).thenReturn(true);
            when(zSet.removeRangeByScore(1.0, true, 3.0, true)).thenReturn(2);
            when(zSet.removeRangeByRank(0, 2)).thenReturn(3);
            assertThat(RedisUtils.zRemove("k", "v")).isTrue();
            assertThat(RedisUtils.zRemoveRangeByScore("k", 1.0, 3.0)).isEqualTo(2);
            assertThat(RedisUtils.zRemoveRangeByRank("k", 0, 2)).isEqualTo(3);
            verify(zSet).remove("v");
            verify(zSet).removeRangeByScore(1.0, true, 3.0, true);
            verify(zSet).removeRangeByRank(0, 2);
        }

        @Test
        @DisplayName("zRangeByScore 两个重载以双端包含边界透传参数并返回成员")
        void shouldRangeByScoreBothOverloads() {
            Collection<String> all = List.of("a", "b");
            Collection<String> paged = List.of("b");
            when(zSet.valueRange(1.0, true, 3.0, true)).thenReturn(all);
            when(zSet.valueRange(1.0, true, 3.0, true, 1, 1)).thenReturn(paged);
            assertThat(RedisUtils.zRangeByScore("k", 1.0, 3.0)).containsExactly("a", "b");
            assertThat(RedisUtils.zRangeByScore("k", 1.0, 3.0, 1, 1)).containsExactly("b");
            verify(zSet).valueRange(1.0, true, 3.0, true);
            verify(zSet).valueRange(1.0, true, 3.0, true, 1, 1);
        }

        @Test
        @DisplayName("zCountRangeByScore 以双端包含边界返回区间计数")
        void shouldCountRangeByScore() {
            when(zSet.count(1.0, true, 3.0, true)).thenReturn(5);
            assertThat(RedisUtils.zCountRangeByScore("k", 1.0, 3.0)).isEqualTo(5);
            verify(zSet).count(1.0, true, 3.0, true);
        }

        @Test
        @DisplayName("zSum 累加各元素分数并跳过不存在元素")
        void shouldZSumSkippingNullScore() {
            when(zSet.getScore("a")).thenReturn(2.0);
            when(zSet.getScore("b")).thenReturn(null);
            when(zSet.getScore("c")).thenReturn(5.5);
            assertThat(RedisUtils.zSum("k", List.of("a", "b", "c"))).isEqualTo(7.5);
            verify(zSet).getScore("a");
            verify(zSet).getScore("b");
            verify(zSet).getScore("c");
        }
    }

    @Nested
    @DisplayName("限流与消息")
    class RateLimitAndMessaging {

        @Test
        @DisplayName("rateLimit 秒数重载委托为 Duration.ofSeconds，覆盖 tryAcquire 真假分支")
        void shouldRateLimitBothBranches() {
            when(rateLimiter.tryAcquire(1)).thenReturn(true, false);
            assertThat(RedisUtils.rateLimit("k", RateType.OVERALL, 10, 2)).isTrue();
            assertThat(RedisUtils.rateLimit("k", RateType.PER_CLIENT, 1, Duration.ofSeconds(1)))
                .isFalse();
            verify(rateLimiter).trySetRate(RateType.OVERALL, 10, Duration.ofSeconds(2));
            verify(rateLimiter).trySetRate(RateType.PER_CLIENT, 1, Duration.ofSeconds(1));
            verify(rateLimiter, times(2)).tryAcquire(1);
        }

        @Test
        @DisplayName("publish 两个重载发布消息，带 consumer 的重载额外回调消费者")
        @SuppressWarnings("unchecked")
        void shouldPublishBothOverloads() {
            Consumer<String> consumer = mock(Consumer.class);
            RedisUtils.publish("topic", "msg", consumer);
            RedisUtils.publish("topic", "msg");
            verify(topic, times(2)).publish("msg");
            verify(consumer).accept("msg");
        }

        @Test
        @DisplayName("subscribe 按消息类型注册监听器并转发消息给消费者")
        @SuppressWarnings("unchecked")
        void shouldSubscribe() {
            Consumer<String> consumer = mock(Consumer.class);
            RedisUtils.subscribe("topic", String.class, consumer);
            ArgumentCaptor<MessageListener<String>> captor = ArgumentCaptor.forClass(
                (Class<MessageListener<String>>) (Class<?>) MessageListener.class);
            verify(topic).addListener(eq(String.class), captor.capture());
            captor.getValue().onMessage("topic", "msg");
            verify(consumer).accept("msg");
        }
    }
}
