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

package top.continew.starter.cache.redisson.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import top.continew.starter.core.util.SpringUtils;

import java.util.concurrent.TimeUnit;

/**
 * RedisLockUtils 纯逻辑分支单测。
 *
 * <p>
 * RedisLockUtils 的 client 为 {@code static volatile}，经 {@link SpringUtils#getBean(Class, boolean)}
 * 双检锁惰性初始化。本测试在首次引用 RedisLockUtils 前以静态模拟让
 * {@code getBean(RedissonClient.class, false)} 返回受控的 {@link RedissonClient}，并借一次
 * tryLock 预热触发双检锁缓存客户端替身，此后全部获取锁路径不再触达 Spring 上下文。
 * 断言统一校验转发到 RLock 的方法、参数与中断状态恢复，不启动真实 Redis。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("RedisLockUtils 纯逻辑分支单测")
class RedisLockUtilsTest {

    private static RedissonClient client;

    private RLock lock;

    @BeforeAll
    static void injectClient() {
        client = mock(RedissonClient.class);
        RLock warmupLock = mock(RLock.class);
        try (MockedStatic<SpringUtils> springUtils = mockStatic(SpringUtils.class)) {
            springUtils.when(() -> SpringUtils.getBean(RedissonClient.class, false))
                .thenReturn(client);
            doReturn(warmupLock).when(client).getLock("warmup:key");
            RedisLockUtils.tryLock("warmup:key", 0L, 0L, TimeUnit.MILLISECONDS);
            springUtils.verify(() -> SpringUtils.getBean(RedissonClient.class, false));
        }
    }

    @BeforeEach
    void resetInvocations() {
        clearInvocations(client);
        lock = mock(RLock.class);
        doReturn(lock).when(client).getLock(anyString());
    }

    @SuppressWarnings("unchecked")
    private RFuture<Void> stubHeldLock() throws Exception {
        when(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        RFuture<Void> future = mock(RFuture.class);
        doReturn(future).when(lock).unlockAsync();
        return future;
    }

    @Nested
    @DisplayName("获取锁委托链")
    class TryLockDelegation {

        @Test
        @DisplayName("连续获取锁复用双检锁缓存的客户端替身")
        void shouldReuseInjectedClientAcrossAcquisitions() {
            RedisLockUtils.tryLock("lock:a");
            RedisLockUtils.tryLockWithWatchdog("lock:b");
            verify(client).getLock("lock:a");
            verify(client).getLock("lock:b");
        }

        @Test
        @DisplayName("tryLock 默认重载以默认过期 10000 与超时 5000 毫秒透传")
        void shouldTryLockWithDefaultConstants() throws Exception {
            when(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).thenReturn(true);
            RedisLockUtils instance = RedisLockUtils.tryLock("lock:key");
            verify(client).getLock("lock:key");
            verify(lock).tryLock(5000L, 10000L, TimeUnit.MILLISECONDS);
            assertThat(instance.isLocked()).isTrue();
        }

        @Test
        @DisplayName("tryLock 毫秒重载透传过期与超时值")
        void shouldTryLockWithMillisecondOverload() throws Exception {
            RedisLockUtils.tryLock("lock:key", 15000L, 8000L);
            verify(client).getLock("lock:key");
            verify(lock).tryLock(8000L, 15000L, TimeUnit.MILLISECONDS);
        }

        @Test
        @DisplayName("tryLock 全参重载透传时间单位")
        void shouldTryLockWithFullArguments() throws Exception {
            RedisLockUtils.tryLock("lock:key", 20L, 10L, TimeUnit.SECONDS);
            verify(client).getLock("lock:key");
            verify(lock).tryLock(10L, 20L, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("isLocked 反映构造期获取结果真假两分支")
        void shouldReportLockedStateBothBranches() throws Exception {
            when(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).thenReturn(true, false);
            assertThat(RedisLockUtils.tryLock("lock:key").isLocked()).isTrue();
            assertThat(RedisLockUtils.tryLock("lock:key").isLocked()).isFalse();
            verify(lock, times(2)).tryLock(5000L, 10000L, TimeUnit.MILLISECONDS);
        }

        @Test
        @DisplayName("tryLock 被中断时返回未获锁实例并恢复线程中断标记")
        void shouldRestoreInterruptFlagWhenTryLockInterrupted() throws Exception {
            when(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS))
                .thenThrow(new InterruptedException("interrupted"));
            RedisLockUtils instance = RedisLockUtils.tryLock("lock:key", 10000L, 5000L);
            assertThat(instance.isLocked()).isFalse();
            assertThat(Thread.interrupted()).isTrue();
        }
    }

    @Nested
    @DisplayName("看门狗获取")
    class WatchdogTryLock {

        @Test
        @DisplayName("tryLockWithWatchdog 默认重载以默认超时 5000 毫秒与租期 -1 透传")
        void shouldTryLockWithWatchdogDefaultTimeout() throws Exception {
            when(lock.tryLock(5000L, -1L, TimeUnit.MILLISECONDS)).thenReturn(true);
            RedisLockUtils.tryLockWithWatchdog("lock:key");
            verify(client).getLock("lock:key");
            verify(lock).tryLock(5000L, -1L, TimeUnit.MILLISECONDS);
        }

        @Test
        @DisplayName("tryLockWithWatchdog 毫秒重载透传超时并保持租期 -1")
        void shouldTryLockWithWatchdogMillisecondOverload() throws Exception {
            RedisLockUtils.tryLockWithWatchdog("lock:key", 3000L);
            verify(lock).tryLock(3000L, -1L, TimeUnit.MILLISECONDS);
        }

        @Test
        @DisplayName("tryLockWithWatchdog 全参重载透传时间单位并保持租期 -1")
        void shouldTryLockWithWatchdogExplicitUnit() throws Exception {
            RedisLockUtils.tryLockWithWatchdog("lock:key", 2L, TimeUnit.SECONDS);
            verify(lock).tryLock(2L, -1L, TimeUnit.SECONDS);
        }
    }

    @Nested
    @DisplayName("释放锁")
    class CloseBehavior {

        @Test
        @DisplayName("未获锁实例 close 不触发释放")
        void shouldNotUnlockWhenNotLocked() throws Exception {
            when(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).thenReturn(false);
            RedisLockUtils instance = RedisLockUtils.tryLock("lock:key");
            assertThat(instance.isLocked()).isFalse();
            instance.close();
            verify(lock, never()).unlockAsync();
        }

        @Test
        @DisplayName("持锁标记为真但锁被其他线程持有时 close 不触发释放")
        void shouldNotUnlockWhenHeldByOtherThread() throws Exception {
            when(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(false);
            RedisLockUtils.tryLock("lock:key").close();
            verify(lock, never()).unlockAsync();
        }

        @Test
        @DisplayName("当前线程持有时 close 异步释放并等待完成")
        void shouldUnlockAsyncWhenHeldByCurrentThread() throws Exception {
            RFuture<Void> future = stubHeldLock();
            RedisLockUtils.tryLock("lock:key").close();
            verify(lock).unlockAsync();
            verify(future).get();
        }

        @Test
        @DisplayName("释放等待被中断时恢复线程中断标记且不向上传播")
        void shouldRestoreInterruptFlagWhenUnlockInterrupted() throws Exception {
            RFuture<Void> future = stubHeldLock();
            doThrow(new InterruptedException("interrupted")).when(future).get();
            RedisLockUtils.tryLock("lock:key").close();
            assertThat(Thread.interrupted()).isTrue();
            verify(lock).unlockAsync();
        }

        @Test
        @DisplayName("释放失败被捕获且不向上传播")
        void shouldSwallowUnlockFailure() throws Exception {
            RFuture<Void> future = stubHeldLock();
            doThrow(new IllegalStateException("redis down")).when(future).get();
            RedisLockUtils instance = RedisLockUtils.tryLock("lock:key");
            assertThatCode(instance::close).doesNotThrowAnyException();
            verify(lock).unlockAsync();
        }
    }
}
