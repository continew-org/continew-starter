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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.redisson.api.*;
import org.redisson.api.options.KeysScanOptions;
import top.continew.starter.core.constant.StringConstants;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class RedisUtils {

    private static final RedissonClient CLIENT = SpringUtil.getBean(RedissonClient.class);

    private RedisUtils() {
    }

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    public static <T> void set(String key, T value) {
        CLIENT.getBucket(key).set(value);
    }

    /**
     * 设置缓存
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     */
    public static <T> void set(String key, T value, Duration duration) {
        CLIENT.getBucket(key).set(value, duration);
    }

    /**
     * 设置缓存
     *
     * <p>如果键已存在，则不设置</p>
     *
     * @param key   键
     * @param value 值
     * @return true：设置成功；false：设置失败
     * @since 2.10.0
     */
    public static <T> boolean setIfAbsent(String key, T value) {
        return CLIENT.getBucket(key).setIfAbsent(value);
    }

    /**
     * 设置缓存
     *
     * <p>如果键已存在，则不设置</p>
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     * @return true：设置成功；false：设置失败
     * @since 2.10.0
     */
    public static <T> boolean setIfAbsent(String key, T value, Duration duration) {
        return CLIENT.getBucket(key).setIfAbsent(value, duration);
    }

    /**
     * 设置缓存
     * <p>如果键不存在，则不设置</p>
     * 
     * @param key   键
     * @param value 值
     * @return true：设置成功；false：设置失败
     * @since 2.10.0
     */
    public static <T> boolean setIfExists(String key, T value) {
        return CLIENT.getBucket(key).setIfExists(value);
    }

    /**
     * 设置缓存
     * <p>如果键不存在，则不设置</p>
     * 
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     * @return true：设置成功；false：设置失败
     * @since 2.10.0
     */
    public static <T> boolean setIfExists(String key, T value, Duration duration) {
        return CLIENT.getBucket(key).setIfExists(value, duration);
    }

    /**
     * 查询指定缓存
     *
     * @param key 键
     * @return 值
     */
    public static <T> T get(String key) {
        RBucket<T> bucket = CLIENT.getBucket(key);
        return bucket.get();
    }

    /**
     * 设置缓存（List 集合）
     *
     * @param key   键
     * @param value 值
     * @since 2.1.1
     */
    public static <T> void setList(String key, List<T> value) {
        RList<T> list = CLIENT.getList(key);
        list.addAll(value);
    }

    /**
     * 设置缓存（List 集合）
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     * @since 2.1.1
     */
    public static <T> void setList(String key, List<T> value, Duration duration) {
        RBatch batch = CLIENT.createBatch();
        RListAsync<T> list = batch.getList(key);
        list.addAllAsync(value);
        list.expireAsync(duration);
        batch.execute();
    }

    /**
     * 查询指定缓存（List 集合）
     *
     * @param key 键
     * @return 值
     * @since 2.1.1
     */
    public static <T> List<T> getList(String key) {
        RList<T> list = CLIENT.getList(key);
        return list.readAll();
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return true：设置成功；false：设置失败
     */
    public static boolean delete(String key) {
        return CLIENT.getBucket(key).delete();
    }

    /**
     * 删除缓存
     *
     * @param pattern 键模式
     */
    public static void deleteByPattern(String pattern) {
        CLIENT.getKeys().deleteByPattern(pattern);
    }

    /**
     * 递增 1
     *
     * @param key 键
     * @return 当前值
     * @since 2.0.1
     */
    public static long incr(String key) {
        return CLIENT.getAtomicLong(key).incrementAndGet();
    }

    /**
     * 递减 1
     *
     * @param key 键
     * @return 当前值
     * @since 2.0.1
     */
    public static long decr(String key) {
        return CLIENT.getAtomicLong(key).decrementAndGet();
    }

    /**
     * 设置缓存过期时间
     *
     * @param key      键
     * @param duration 过期时间
     * @return true：设置成功；false：设置失败
     */
    public static boolean expire(String key, Duration duration) {
        return CLIENT.getBucket(key).expire(duration);
    }

    /**
     * 查询缓存剩余过期时间
     *
     * @param key 键
     * @return 缓存剩余过期时间（单位：毫秒）
     */
    public static long getTimeToLive(String key) {
        return CLIENT.getBucket(key).remainTimeToLive();
    }

    /**
     * 是否存在指定缓存
     *
     * @param key 键
     * @return true：存在；false：不存在
     */
    public static boolean exists(String key) {
        return CLIENT.getKeys().countExists(key) > 0;
    }

    /**
     * 查询缓存列表
     *
     * @param pattern 键模式
     * @return 缓存列表
     */
    public static Collection<String> keys(String pattern) {
        KeysScanOptions options = KeysScanOptions.defaults();
        options.pattern(pattern);
        return CLIENT.getKeys().getKeysStream(options).toList();
    }

    /**
     * 添加元素到 ZSet 中
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return true：添加成功；false：添加失败
     * @since 2.7.3
     */
    public static <T> boolean zAdd(String key, T value, double score) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.add(score, value);
    }

    /**
     * 查询 ZSet 中指定元素的分数
     *
     * @param key   键
     * @param value 值
     * @return 分数（null 表示元素不存在）
     * @since 2.7.3
     */
    public static <T> Double zScore(String key, T value) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.getScore(value);
    }

    /**
     * 查询 ZSet 中指定元素的排名
     *
     * @param key   键
     * @param value 值
     * @return 排名（从 0 开始，null 表示元素不存在）
     * @since 2.7.3
     */
    public static <T> Integer zRank(String key, T value) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.rank(value);
    }

    /**
     * 查询 ZSet 中的元素个数
     *
     * @param key 键
     * @return 元素个数
     * @since 2.7.3
     */
    public static <T> int zSize(String key) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.size();
    }

    /**
     * 从 ZSet 中删除指定元素
     *
     * @param key   键
     * @param value 值
     * @return true：删除成功；false：删除失败
     * @since 2.7.3
     */
    public static <T> boolean zRemove(String key, T value) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.remove(value);
    }

    /**
     * 删除 ZSet 中指定分数范围内的元素
     *
     * @param key 键
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 删除的元素个数
     * @since 2.7.3
     */
    public static <T> int zRemoveRangeByScore(String key, double min, double max) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.removeRangeByScore(min, true, max, true);
    }

    /**
     * 删除 ZSet 中指定排名范围内的元素
     *
     * <p>
     * 索引从 0 开始。<code>-1<code> 表示最高分，<code>-2<code> 表示第二高分。
     * </p>
     *
     * @param key        键
     * @param startIndex 起始索引
     * @param endIndex   结束索引
     * @return 删除的元素个数
     * @since 2.7.3
     */
    public static <T> int zRemoveRangeByRank(String key, int startIndex, int endIndex) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.removeRangeByRank(startIndex, endIndex);
    }

    /**
     * 根据分数范围查询 ZSet 中的元素列表
     *
     * @param key 键
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 元素列表
     * @since 2.7.3
     */
    public static <T> Collection<T> zRangeByScore(String key, double min, double max) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.valueRange(min, true, max, true);
    }

    /**
     * 根据分数范围查询 ZSet 中的元素列表
     *
     * @param key    键
     * @param min    最小分数（包含）
     * @param max    最大分数（包含）
     * @param offset 偏移量
     * @param count  数量
     * @return 元素列表
     * @since 2.7.3
     */
    public static <T> Collection<T> zRangeByScore(String key, double min, double max, int offset, int count) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.valueRange(min, true, max, true, offset, count);
    }

    /**
     * 根据分数范围查询 ZSet 中的元素个数
     *
     * @param key 键
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 元素个数
     * @since 2.7.3
     */
    public static <T> int zCountRangeByScore(String key, double min, double max) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        return zSet.count(min, true, max, true);
    }

    /**
     * 计算 ZSet 中多个元素的分数之和
     *
     * @param key    键
     * @param values 值列表
     * @return 分数之和
     * @since 2.7.3
     */
    public static <T> double zSum(String key, Collection<T> values) {
        RScoredSortedSet<T> zSet = CLIENT.getScoredSortedSet(key);
        double sum = 0;
        for (T value : values) {
            Double score = zSet.getScore(value);
            if (score != null) {
                sum += score;
            }
        }
        return sum;
    }

    /**
     * 限流
     *
     * @param key          键
     * @param rateType     限流类型（OVERALL：全局限流；PER_CLIENT：单机限流）
     * @param rate         速率（指定时间间隔产生的令牌数）
     * @param rateInterval 速率间隔（时间间隔，单位：秒）
     * @return true：成功；false：失败
     */
    public static boolean rateLimit(String key, RateType rateType, int rate, int rateInterval) {
        return rateLimit(key, rateType, rate, Duration.ofSeconds(rateInterval));
    }

    /**
     * 限流
     *
     * @param key          键
     * @param rateType     限流类型（OVERALL：全局限流；PER_CLIENT：单机限流）
     * @param rate         速率（指定时间间隔产生的令牌数）
     * @param rateInterval 速率间隔（时间间隔）
     * @return true：成功；false：失败
     */
    public static boolean rateLimit(String key, RateType rateType, int rate, Duration rateInterval) {
        RRateLimiter rateLimiter = CLIENT.getRateLimiter(key);
        rateLimiter.trySetRate(rateType, rate, rateInterval);
        return rateLimiter.tryAcquire(1);
    }

    /**
     * 尝试获取锁
     *
     * @param key        键
     * @param expireTime 锁过期时间（单位：毫秒）
     * @param timeout    获取锁超时时间（单位：毫秒）
     * @return true：成功；false：失败
     * @since 2.7.2
     */
    public static boolean tryLock(String key, long expireTime, long timeout) {
        return tryLock(key, expireTime, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 释放锁
     *
     * @param key 键
     * @return true：释放成功；false：释放失败
     * @since 2.7.2
     */
    public static boolean unlock(String key) {
        RLock lock = getLock(key);
        return unlock(lock);
    }

    /**
     * 尝试获取锁
     *
     * @param key        键
     * @param expireTime 锁过期时间
     * @param timeout    获取锁超时时间
     * @param unit       时间单位
     * @return true：成功；false：失败
     * @since 2.7.2
     */
    public static boolean tryLock(String key, long expireTime, long timeout, TimeUnit unit) {
        RLock lock = getLock(key);
        try {
            return lock.tryLock(timeout, expireTime, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lock 锁实例
     * @return true：释放成功；false：释放失败
     * @since 2.7.2
     */
    public static boolean unlock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlockAsync().get();
                return true;
            } catch (ExecutionException | InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 获取锁实例
     *
     * @param key 键
     * @return 锁实例
     * @since 2.7.2
     */
    public static RLock getLock(String key) {
        return CLIENT.getLock(key);
    }

    /**
     * 格式化键，将各子键用 : 拼接起来
     *
     * @param subKeys 子键列表
     * @return 键
     */
    public static String formatKey(String... subKeys) {
        return String.join(StringConstants.COLON, ArrayUtil.removeBlank(subKeys));
    }
}
