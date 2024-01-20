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

package top.charles7c.continew.starter.auth.justauth.core;

import me.zhyd.oauth.cache.AuthStateCache;
import top.charles7c.continew.starter.cache.redisson.util.RedisUtils;

import java.time.Duration;

/**
 * JustAuth State 缓存 Redis 实现
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class JustAuthStateCacheRedisImpl implements AuthStateCache {

    private static final String KEY_PREFIX = "SOCIAL_AUTH_STATE";

    /**
     * 存入缓存
     *
     * @param key   key
     * @param value 内容
     */
    @Override
    public void cache(String key, String value) {
        // 参考：在 JustAuth 中，内置了一个基于 map 的 state 缓存器，默认缓存有效期为 3 分钟
        RedisUtils.set(RedisUtils.formatKey(KEY_PREFIX, key), value, Duration.ofMinutes(3));
    }

    /**
     * 存入缓存
     *
     * @param key     key
     * @param value   内容
     * @param timeout 缓存过期时间（毫秒）
     */
    @Override
    public void cache(String key, String value, long timeout) {
        RedisUtils.set(RedisUtils.formatKey(KEY_PREFIX, key), value, Duration.ofMillis(timeout));
    }

    /**
     * 获取缓存内容
     *
     * @param key key
     * @return 内容
     */
    @Override
    public String get(String key) {
        return RedisUtils.get(RedisUtils.formatKey(KEY_PREFIX, key));
    }

    /**
     * 是否存在 key，如果对应 key 的 value 值已过期，也返回 false
     *
     * @param key key
     * @return true：存在 key，并且 value 没过期；false：key 不存在或者已过期
     */
    @Override
    public boolean containsKey(String key) {
        return RedisUtils.hasKey(RedisUtils.formatKey(KEY_PREFIX, key));
    }
}