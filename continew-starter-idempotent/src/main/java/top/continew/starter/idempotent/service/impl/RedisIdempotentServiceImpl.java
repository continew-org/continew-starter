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

package top.continew.starter.idempotent.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import top.continew.starter.idempotent.service.IdempotentService;

import java.util.concurrent.TimeUnit;

/**
 * redis实现幂等
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 19:49
 * @Package top.continew.starter.idempotent.service.impl.RedisIdempotentServiceImpl
 */

public class RedisIdempotentServiceImpl implements IdempotentService {

    private final StringRedisTemplate redisTemplate;
    private final long redisTimeout;

    public RedisIdempotentServiceImpl(StringRedisTemplate redisTemplate, long redisTimeout) {
        this.redisTemplate = redisTemplate;
        this.redisTimeout = redisTimeout;
    }

    @Override
    public boolean checkAndLock(String key, long timeout) {
        long effectiveTimeout = timeout > 0 ? timeout : redisTimeout;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", effectiveTimeout, TimeUnit.SECONDS);
        return success != null && success;
    }

    @Override
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}