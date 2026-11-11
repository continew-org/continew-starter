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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import top.continew.starter.core.exception.BusinessException;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异常工具类测试
 *
 * <p>
 * 锁定 exToNull / exToBlank / exToDefault / exToThrow 的异常转换契约，
 * 以及 printException 对已完成 Future 异常的提取路径。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExceptionUtilsTest {

    /**
     * 构造抛出指定异常的 ExSupplier
     *
     * @param cause 待抛出的异常
     * @return 必然失败的供应器
     */
    private static ExceptionUtils.ExSupplier<String> failingSupplier(Exception cause) {
        return () -> {
            throw cause;
        };
    }

    /**
     * 构造返回指定值的 ExSupplier
     *
     * @param value 返回值
     * @return 必然成功的供应器
     */
    private static ExceptionUtils.ExSupplier<String> succeedingSupplier(String value) {
        return () -> value;
    }

    @Test
    @DisplayName("exToNull 透传正常结果")
    void shouldReturnValueWhenSupplierSucceeds() {
        assertThat(ExceptionUtils.exToNull(succeedingSupplier("ok"))).isEqualTo("ok");
    }

    @Test
    @DisplayName("exToNull 在异常时返回 null")
    void shouldReturnNullWhenSupplierFails() {
        assertThat(ExceptionUtils.exToNull(failingSupplier(new IllegalStateException("boom"))))
            .isNull();
    }

    @Test
    @DisplayName("exToNull 带处理器时将异常交给 Consumer")
    void shouldDeliverExceptionToConsumer() {
        AtomicReference<Exception> captured = new AtomicReference<>();
        Exception cause = new IllegalStateException("boom");
        String result = ExceptionUtils.exToNull(failingSupplier(cause), captured::set);
        assertThat(result).isNull();
        assertThat(captured.get()).isSameAs(cause);
    }

    @Test
    @DisplayName("exToNull 处理器为空时静默返回默认值")
    void shouldReturnNullWithoutConsumer() {
        assertThat(
            ExceptionUtils.exToNull(failingSupplier(new IllegalStateException("boom")), null))
            .isNull();
    }

    @Test
    @DisplayName("exToBlank 在异常时返回空字符串")
    void shouldReturnBlankWhenSupplierFails() {
        assertThat(ExceptionUtils.exToBlank(failingSupplier(new IllegalStateException("boom"))))
            .isEmpty();
    }

    @Test
    @DisplayName("exToBlank 透传正常结果")
    void shouldReturnValueWhenExToBlankSucceeds() {
        assertThat(ExceptionUtils.exToBlank(succeedingSupplier("ok"))).isEqualTo("ok");
    }

    @Test
    @DisplayName("exToDefault 在异常时返回指定默认值")
    void shouldReturnDefaultWhenSupplierFails() {
        assertThat(ExceptionUtils.exToDefault(failingSupplier(new IllegalStateException("boom")),
            "fallback"))
            .isEqualTo("fallback");
    }

    @Test
    @DisplayName("exToThrow 在异常时抛出 mapper 转换后的异常")
    void shouldThrowMappedException() {
        Exception cause = new IllegalStateException("boom");
        ExceptionUtils.ExSupplier<String> failing = failingSupplier(cause);
        assertThatThrownBy(() -> ExceptionUtils.exToThrow(failing, BusinessException::new))
            .isInstanceOf(BusinessException.class)
            .hasCause(cause);
    }

    @Test
    @DisplayName("exToThrow 透传正常结果")
    void shouldReturnValueWhenExToThrowSucceeds() {
        assertThat(ExceptionUtils.exToThrow(succeedingSupplier("ok"), BusinessException::new))
            .isEqualTo("ok");
    }

    @Test
    @DisplayName("printException 对 null 异常与非 Future 任务不记录任何日志")
    void shouldDoNothingForPlainRunnable() {
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            ExceptionUtils.printException(() -> {
            }, null);
            assertThat(appender.list).isEmpty();
        } finally {
            detachLogAppender(appender);
        }
    }

    @Test
    @DisplayName("printException 对已取消的 FutureTask 记录 CancellationException 而不抛出")
    void shouldLogCancelledFutureSilently() {
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            FutureTask<String> cancelled = new FutureTask<>(() -> "x");
            cancelled.cancel(true);
            ExceptionUtils.printException(cancelled, null);
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getThrowableProxy().getClassName())
                .isEqualTo(CancellationException.class.getName());
        } finally {
            detachLogAppender(appender);
        }
    }

    @Test
    @DisplayName("printException 对执行失败的 FutureTask 提取包装异常的 cause 记录")
    void shouldLogExecutionExceptionCauseSilently() {
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            FutureTask<String> failed = new FutureTask<>(() -> {
                throw new IllegalStateException("task failed");
            });
            failed.run();
            ExceptionUtils.printException(failed, null);
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getThrowableProxy().getClassName())
                .isEqualTo(IllegalStateException.class.getName());
            assertThat(appender.list.get(0).getThrowableProxy().getMessage())
                .isEqualTo("task failed");
        } finally {
            detachLogAppender(appender);
        }
    }

    @Test
    @DisplayName("printException 对未执行的 FutureTask 跳过 get 阻塞")
    void shouldSkipUnfinishedFuture() {
        FutureTask<String> running = new FutureTask<>(() -> "x");
        // isDone() 守卫若被移除，get() 会永久阻塞，超时让回归以失败而非挂起暴露
        assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> ExceptionUtils.printException(running, null));
        assertThat(running.isDone()).isFalse();
    }

    @Test
    @DisplayName("printException 直接传入异常时原样记录")
    void shouldLogDirectThrowable() {
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            ExceptionUtils.printException(() -> {
            }, new IllegalStateException("direct"));
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getThrowableProxy().getClassName())
                .isEqualTo(IllegalStateException.class.getName());
            assertThat(appender.list.get(0).getThrowableProxy().getMessage()).isEqualTo("direct");
        } finally {
            detachLogAppender(appender);
        }
    }

    @Test
    @DisplayName("printException 遇到中断异常时恢复中断标志且不记录日志")
    void shouldRestoreInterruptFlagWithoutLogging() {
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            ExceptionUtils.printException(new InterruptingFuture(), null);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(appender.list).isEmpty();
        } finally {
            detachLogAppender(appender);
            // 中断标志会带到后续用例，这里必须清除
            Thread.interrupted();
        }
    }

    /**
     * 挂载日志收集器，用于断言 printException 实际记录的异常类型
     *
     * @return 已启动的日志收集器
     */
    private static ListAppender<ILoggingEvent> attachLogAppender() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger().addAppender(appender);
        return appender;
    }

    /**
     * 卸载日志收集器
     *
     * @param appender 日志收集器
     */
    private static void detachLogAppender(ListAppender<ILoggingEvent> appender) {
        logger().detachAppender(appender);
    }

    /**
     * 获取 ExceptionUtils 使用的 Logger
     *
     * @return logback Logger
     */
    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(ExceptionUtils.class);
    }

    /**
     * isDone() 为 true 但 get() 抛 InterruptedException 的 Future，用于覆盖中断分支
     */
    private static class InterruptingFuture implements Future<String>, Runnable {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public String get() throws InterruptedException {
            throw new InterruptedException("interrupted");
        }

        @Override
        public String get(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("interrupted");
        }

        @Override
        public void run() {
            // 仅作为 Future 与 Runnable 载体，执行路径由用例直接调用 printException 驱动
        }
    }
}
