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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BusinessException;

import java.util.concurrent.FutureTask;
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
        assertThatThrownBy(() -> ExceptionUtils.exToThrow(failingSupplier(cause),
            BusinessException::new)).isInstanceOf(BusinessException.class)
            .hasCause(cause);
    }

    @Test
    @DisplayName("exToThrow 透传正常结果")
    void shouldReturnValueWhenExToThrowSucceeds() {
        assertThat(ExceptionUtils.exToThrow(succeedingSupplier("ok"), BusinessException::new))
            .isEqualTo("ok");
    }

    @Test
    @DisplayName("printException 对 null 异常与非 Future 任务不执行任何动作")
    void shouldDoNothingForPlainRunnable() {
        ExceptionUtils.printException(() -> {
        }, null);
    }

    @Test
    @DisplayName("printException 对已取消的 FutureTask 记录 CancellationException 而不抛出")
    void shouldLogCancelledFutureSilently() {
        FutureTask<String> cancelled = new FutureTask<>(() -> "x");
        cancelled.cancel(true);
        ExceptionUtils.printException(cancelled, null);
    }

    @Test
    @DisplayName("printException 对执行失败的 FutureTask 提取包装异常的 cause 记录")
    void shouldLogExecutionExceptionCauseSilently() {
        FutureTask<String> failed = new FutureTask<>(() -> {
            throw new IllegalStateException("task failed");
        });
        failed.run();
        ExceptionUtils.printException(failed, null);
    }

    @Test
    @DisplayName("printException 对未执行的 FutureTask 跳过 get 阻塞")
    void shouldSkipUnfinishedFuture() {
        FutureTask<String> running = new FutureTask<>(() -> "x");
        ExceptionUtils.printException(running, null);
        assertThat(running.isDone()).isFalse();
    }

    @Test
    @DisplayName("printException 直接传入异常时原样记录")
    void shouldLogDirectThrowable() {
        ExceptionUtils.printException(() -> {
        }, new IllegalStateException("direct"));
    }
}
