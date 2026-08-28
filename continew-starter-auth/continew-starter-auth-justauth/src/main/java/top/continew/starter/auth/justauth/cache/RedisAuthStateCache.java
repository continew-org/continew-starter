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

package top.continew.starter.auth.justauth.cache;

import me.zhyd.oauth.cache.AuthStateCache;
import top.continew.starter.auth.justauth.autoconfigure.cache.JustAuthStateCacheProperties;
import top.continew.starter.cache.redisson.util.RedisUtils;

import java.time.Duration;

/**
 * Redis State 缓存实现
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class RedisAuthStateCache implements AuthStateCache {

    public final JustAuthStateCacheProperties cacheProperties;

    public RedisAuthStateCache(JustAuthStateCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 存入缓存
     *
     * @param key   key
     * @param value 内容
     */
    @Override
    public void cache(String key, String value) {
        this.cache(key, value, cacheProperties.getTimeout().toMillis());
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
        RedisUtils.set(RedisUtils.formatKey(cacheProperties.getPrefix(), key), value,
            Duration.ofMillis(timeout));
    }

    /**
     * 获取缓存内容
     *
     * @param key key
     * @return 内容
     */
    @Override
    public String get(String key) {
        return RedisUtils.get(RedisUtils.formatKey(cacheProperties.getPrefix(), key));
    }

    /**
     * 是否存在 key，如果对应 key 的 value 值已过期，也返回 false
     *
     * @param key key
     * @return true：存在 key，并且 value 没过期；false：key 不存在或者已过期
     */
    @Override
    public boolean containsKey(String key) {
        return RedisUtils.exists(RedisUtils.formatKey(cacheProperties.getPrefix(), key));
    }
}
