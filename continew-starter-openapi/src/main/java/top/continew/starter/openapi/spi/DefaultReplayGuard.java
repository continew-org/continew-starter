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

package top.continew.starter.openapi.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认重放防护实现（基于 JVM 内存）
 *
 * <p>用 {@link ConcurrentHashMap#compute} 保证「过期检查 + 占位」的原子性，等效于
 * setIfAbsent 语义。仅适用于单实例部署：多实例部署时各实例 nonce 互不可见，无法全局去重，
 * 使用方必须定义 Bean 覆盖为集中式存储（如 Redis）实现。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class DefaultReplayGuard implements ReplayGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReplayGuard.class);

    /**
     * 每隔多少次占位/释放操作触发一次全量过期清理
     */
    private static final long CLEAN_INTERVAL = 64;

    private final Map<String, Long> store = new ConcurrentHashMap<>();

    private final AtomicLong operationCount = new AtomicLong();

    private final Clock clock;

    public DefaultReplayGuard() {
        this(Clock.systemDefaultZone());
    }

    /**
     * 指定时钟构造：过期判定统一取自注入时钟，测试注入可推进时钟替代真实等待
     *
     * @param clock 过期判定基准时钟
     */
    public DefaultReplayGuard(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String nonceKey, Duration ttl) {
        long now = clock.millis();
        long expireAt = now + ttl.toMillis();
        boolean[] acquired = {false};
        // compute 对同一键串行执行，「过期即重占位」判定不会与并发请求交错
        store.compute(nonceKey, (key, oldExpireAt) -> {
            if (oldExpireAt == null || oldExpireAt <= now) {
                acquired[0] = true;
                return expireAt;
            }
            return oldExpireAt;
        });
        // 成功占位同样计入清理节奏，全成功流量（从不 release）下过期条目也能被清理
        if (acquired[0] && operationCount.incrementAndGet() % CLEAN_INTERVAL == 0) {
            purgeExpired();
        }
        return acquired[0];
    }

    @Override
    public void release(String nonceKey) {
        store.remove(nonceKey);
        if (operationCount.incrementAndGet() % CLEAN_INTERVAL == 0) {
            purgeExpired();
        }
    }

    /**
     * 清理已过期的占位条目，防止长期运行后内存缓慢增长
     */
    private void purgeExpired() {
        long now = clock.millis();
        store.entrySet().removeIf(entry -> entry.getValue() <= now);
        LOGGER.debug(
            "[ContiNew Starter] - Default replay guard purged expired nonce entries, remain: {}",
            store.size());
    }

    /**
     * 当前占位条目数（包内可见，仅供测试观察清理效果）
     *
     * @return 占位条目数
     */
    int size() {
        return store.size();
    }
}
