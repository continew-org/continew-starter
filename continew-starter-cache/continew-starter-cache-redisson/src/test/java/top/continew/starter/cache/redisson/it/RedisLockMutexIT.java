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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.cache.redisson.util.RedisLockUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RedisLockUtils 分布式锁真实互斥语义集成测试。
 *
 * <p>同键争用、持锁排他与释放后可重获由 Redis 服务端裁决，单测 mock 无法证明。
 * RLock 为可重入锁，同线程重复加锁会直接成功，因此争用必须由独立线程发起。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("RedisLockUtils 分布式锁真实互斥语义集成测试")
class RedisLockMutexIT extends RedisContainerSupport {

    @Test
    @DisplayName("同键的跨线程争用在持锁期间失败，释放后可重新获取")
    void shouldSerializeCompetingLockHoldersOnSameKey() throws InterruptedException {
        RedisLockUtils first = RedisLockUtils.tryLockWithWatchdog("itest:lock:mutex", 1000);
        assertTrue(first.isLocked());

        // 持锁期间由独立线程发起同键争用：等待 200ms 仍拿不到，互斥由真实 Redis 判定
        AtomicBoolean contested = new AtomicBoolean(false);
        Thread contender = new Thread(() -> {
            RedisLockUtils second = RedisLockUtils.tryLockWithWatchdog("itest:lock:mutex", 200);
            contested.set(second.isLocked());
            if (second.isLocked()) {
                second.close();
            }
        });
        contender.start();
        contender.join(5000);
        assertFalse(contested.get());

        first.close();
        RedisLockUtils third = RedisLockUtils.tryLockWithWatchdog("itest:lock:mutex", 1000);
        assertTrue(third.isLocked());
        third.close();
    }
}
