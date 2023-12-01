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

package top.charles7c.continew.starter.cache.redisson.util;

import cn.hutool.extra.spring.SpringUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.api.*;
import org.redisson.config.Config;
import top.charles7c.continew.starter.core.constant.StringConstants;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Redis 工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisUtils {

    private static final RedissonClient CLIENT = SpringUtil.getBean(RedissonClient.class);

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    public static <T> void set(final String key, final T value) {
        CLIENT.getBucket(key).set(value);
    }

    /**
     * 设置缓存
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     */
    public static <T> void set(final String key, final T value, final Duration duration) {
        RBatch batch = CLIENT.createBatch();
        RBucketAsync<T> bucket = batch.getBucket(key);
        bucket.setAsync(value);
        bucket.expireAsync(duration);
        batch.execute();
    }

    /**
     * 查询指定缓存
     *
     * @param key 键
     * @return 值
     */
    public static <T> T get(final String key) {
        RBucket<T> bucket = CLIENT.getBucket(key);
        return bucket.get();
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return true 设置成功；false 设置失败
     */
    public static boolean delete(final String key) {
        return CLIENT.getBucket(key).delete();
    }

    /**
     * 设置缓存过期时间
     *
     * @param key     键
     * @param timeout 过期时间（单位：秒）
     * @return true 设置成功；false 设置失败
     */
    public static boolean expire(final String key, final long timeout) {
        return expire(key, Duration.ofSeconds(timeout));
    }

    /**
     * 设置缓存过期时间
     *
     * @param key      键
     * @param duration 过期时间
     * @return true 设置成功；false 设置失败
     */
    public static boolean expire(final String key, final Duration duration) {
        return CLIENT.getBucket(key).expire(duration);
    }

    /**
     * 查询缓存剩余过期时间
     *
     * @param key 键
     * @return 缓存剩余过期时间（单位：毫秒）
     */
    public static long getTimeToLive(final String key) {
        return CLIENT.getBucket(key).remainTimeToLive();
    }

    /**
     * 是否存在指定缓存
     *
     * @param key 键
     * @return true 存在；false 不存在
     */
    public static boolean hasKey(String key) {
        RKeys keys = CLIENT.getKeys();
        return keys.countExists(getNameMapper().map(key)) > 0;
    }

    /**
     * 查询缓存列表
     *
     * @param keyPattern 键表达式
     * @return 缓存列表
     */
    public static Collection<String> keys(final String keyPattern) {
        Stream<String> stream = CLIENT.getKeys().getKeysStreamByPattern(getNameMapper().map(keyPattern));
        return stream.map(key -> getNameMapper().unmap(key)).collect(Collectors.toList());
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

    /**
     * 根据 Redisson 配置，获取名称映射器
     *
     * @return 名称映射器
     */
    private static NameMapper getNameMapper() {
        Config config = CLIENT.getConfig();
        if (config.isClusterConfig()) {
            return config.useClusterServers().getNameMapper();
        }
        if (config.isSentinelConfig()) {
            return config.useSentinelServers().getNameMapper();
        }
        return config.useSingleServer().getNameMapper();
    }
}
