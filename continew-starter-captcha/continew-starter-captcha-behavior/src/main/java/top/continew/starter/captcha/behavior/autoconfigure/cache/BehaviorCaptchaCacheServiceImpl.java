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

package top.continew.starter.captcha.behavior.autoconfigure.cache;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.NumberUtil;
import com.anji.captcha.service.CaptchaCacheService;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.captcha.behavior.enums.StorageType;

import java.time.Duration;

/**
 * 行为验证码 Redis 缓存实现
 *
 * @author Bull-BCLS
 * @since 1.1.0
 */
public class BehaviorCaptchaCacheServiceImpl implements CaptchaCacheService {
    @Override
    public void set(String key, String value, long expiresInSeconds) {
        if (NumberUtil.isNumber(value)) {
            RedisUtils.set(key, Convert.toInt(value), Duration.ofSeconds(expiresInSeconds));
        } else {
            RedisUtils.set(key, value, Duration.ofSeconds(expiresInSeconds));
        }
    }

    @Override
    public boolean exists(String key) {
        return RedisUtils.exists(key);
    }

    @Override
    public void delete(String key) {
        RedisUtils.delete(key);
    }

    @Override
    public String get(String key) {
        return Convert.toStr(RedisUtils.get(key));
    }

    @Override
    public String type() {
        return StorageType.REDIS.name().toLowerCase();
    }

    @Override
    public Long increment(String key, long val) {
        return RedisUtils.incr(key);
    }
}
