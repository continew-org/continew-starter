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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 默认重放防护测试：原子占位、有效期、释放后可重占位、并发仅一次成功
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DefaultReplayGuardTest {

    /**
     * 首次占位成功，有效期内再次占位失败
     */
    @Test
    void tryAcquireShouldAllowOnlyOnceWithinTtl() {
        DefaultReplayGuard guard = new DefaultReplayGuard();
        assertThat(guard.tryAcquire("nonce-1", Duration.ofSeconds(60))).isTrue();
        assertThat(guard.tryAcquire("nonce-1", Duration.ofSeconds(60))).isFalse();
        assertThat(guard.tryAcquire("nonce-2", Duration.ofSeconds(60))).isTrue();
    }

    /**
     * 释放占位后同键可重新占位（验签后续失败时释放名额）
     */
    @Test
    void releaseShouldFreeTheKeyForRetry() {
        DefaultReplayGuard guard = new DefaultReplayGuard();
        assertThat(guard.tryAcquire("nonce-1", Duration.ofSeconds(60))).isTrue();
        guard.release("nonce-1");
        assertThat(guard.tryAcquire("nonce-1", Duration.ofSeconds(60))).isTrue();
    }

    /**
     * 占位过期后同键可重新占位
     */
    @Test
    void expiredEntryShouldBeReplaceable() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        DefaultReplayGuard guard = new DefaultReplayGuard(clock);
        assertThat(guard.tryAcquire("nonce-1", Duration.ofMillis(50))).isTrue();
        clock.advance(Duration.ofMillis(80));
        assertThat(guard.tryAcquire("nonce-1", Duration.ofSeconds(60))).isTrue();
    }

    /**
     * 并发抢占同一键时仅一个线程成功（原子占位）
     */
    @Test
    void concurrentAcquireShouldSucceedExactlyOnce() throws Exception {
        DefaultReplayGuard guard = new DefaultReplayGuard();
        int threadCount = 32;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return guard.tryAcquire("same-nonce", Duration.ofSeconds(60));
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            long successCount = futures.stream().filter(future -> {
                try {
                    return future.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return false;
                }
            }).count();
            assertThat(successCount).isEqualTo(1);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 成功占位同样推进清理节奏：从不 release 的全成功流量下，达到清理间隔时过期条目被清除
     */
    @Test
    void acquireShouldTriggerPurgeOfExpiredEntries() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        DefaultReplayGuard guard = new DefaultReplayGuard(clock);
        assertThat(guard.tryAcquire("expired-nonce", Duration.ofMillis(1))).isTrue();
        // 推进时钟使首个条目过期，随后仅通过 tryAcquire（无任何 release）累计超过清理间隔
        clock.advance(Duration.ofMillis(20));
        for (int i = 0; i < 69; i++) {
            assertThat(guard.tryAcquire("nonce-" + i, Duration.ofSeconds(60))).isTrue();
        }
        // 第 64 次成功占位触发过期清理，expired-nonce 已被移除，仅剩 69 个有效占位
        assertThat(guard.size()).isEqualTo(69);
        assertThat(guard.tryAcquire("expired-nonce", Duration.ofSeconds(60))).isTrue();
    }

    /**
     * 可推进时钟：用 advance 模拟时间流逝驱动 TTL 过期，替代真实等待
     */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }
}
