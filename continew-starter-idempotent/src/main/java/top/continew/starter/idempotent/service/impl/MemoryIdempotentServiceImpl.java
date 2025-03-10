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

import top.continew.starter.idempotent.service.IdempotentService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 内存实现幂等
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 19:49
 * @Package top.continew.starter.idempotent.service.impl.MemoryIdempotentServiceImpl
 */
public class MemoryIdempotentServiceImpl implements IdempotentService {

    private final ConcurrentHashMap<String, Long> lockMap = new ConcurrentHashMap<>();
    private final long cleanInterval;

    public MemoryIdempotentServiceImpl(long cleanInterval) {
        this.cleanInterval = cleanInterval;
    }

    @Override
    public boolean checkAndLock(String key, long timeout) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        long currentTime = System.currentTimeMillis();
        Long existingExpireTime = lockMap.get(key);

        // 如果 key 已存在且未过期，返回 false
        if (existingExpireTime != null && currentTime < existingExpireTime) {
            return false;
        }

        // 设置新的过期时间并放入 map
        long effectiveTimeout = timeout > 0 ? timeout : cleanInterval;
        long newExpireTime = currentTime + TimeUnit.SECONDS.toMillis(effectiveTimeout);
        lockMap.put(key, newExpireTime);
        return true;
    }

    @Override
    public void unlock(String key) {
        if (key != null && !key.isEmpty()) {
            lockMap.remove(key);
        }
    }

    // 清理过期 key 的方法
    public void cleanExpiredKeys() {
        long currentTime = System.currentTimeMillis();
        lockMap.entrySet().removeIf(entry -> currentTime >= entry.getValue());
    }
}
