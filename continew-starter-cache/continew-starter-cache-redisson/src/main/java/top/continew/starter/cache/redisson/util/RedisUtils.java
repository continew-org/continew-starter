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

import cn.hutool.extra.spring.SpringUtil;
import org.redisson.api.*;
import top.continew.starter.core.constant.StringConstants;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

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
        return CLIENT.getKeys().getKeysStreamByPattern(pattern).toList();
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
        RRateLimiter rateLimiter = CLIENT.getRateLimiter(key);
        rateLimiter.trySetRate(rateType, rate, rateInterval, RateIntervalUnit.SECONDS);
        return rateLimiter.tryAcquire(1);
    }

    /**
     * 格式化键，将各子键用 : 拼接起来
     *
     * @param subKeys 子键列表
     * @return 键
     */
    public static String formatKey(String... subKeys) {
        return String.join(StringConstants.COLON, subKeys);
    }
}
