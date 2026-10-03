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

package top.continew.starter.auth.refresh.token.support;

import org.redisson.api.RLock;
import top.continew.starter.auth.refresh.token.exception.RefreshTokenException;

import java.util.concurrent.TimeUnit;

/**
 * 认证策略锁。
 *
 * <p>支持 Redisson 普通锁和读写锁，按固定顺序成组获取与逆序释放，避免认证并发策略
 * 之间的死锁。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public final class AuthPolicyLock implements AutoCloseable {

    private final RLock lock;
    private final boolean acquired;

    private AuthPolicyLock(RLock lock, boolean acquired) {
        this.lock = lock;
        this.acquired = acquired;
    }

    /**
     * 在指定时间内获取锁，并使用 Redisson watchdog 自动续期。
     *
     * <p>锁的所有权随返回值转移给调用方，必须配合 try-with-resources 使用；
     * {@link #close()} 只在 {@code acquired} 为 true 且当前线程持锁时释放。</p>
     *
     * @param lock       Redisson 锁实例
     * @param waitMillis 最长等待时间（毫秒）
     * @return 已获取的锁
     */
    public static AuthPolicyLock acquire(RLock lock, long waitMillis) {
        boolean acquired;
        try {
            acquired = lock.tryLock(waitMillis, -1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RefreshTokenException.internalServerError("认证请求已中断");
        }
        if (!acquired) {
            throw RefreshTokenException.tooManyRequests("认证请求正在处理中，请稍后重试");
        }
        return new AuthPolicyLock(lock, true);
    }

    @Override
    public void close() {
        if (acquired && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
