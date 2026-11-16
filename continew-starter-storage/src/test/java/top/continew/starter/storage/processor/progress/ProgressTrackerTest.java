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

package top.continew.starter.storage.processor.progress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * 进度跟踪器测试
 *
 * <p>
 * 通知阈值为百分比变化达到 1% 或字节数变化达到 1MB；达到 100% 时自动回调
 * {@code onComplete}，此后进度上报被忽略；{@code start} 与 {@code complete} 均幂等。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ProgressTrackerTest {

    @Test
    void shouldNotifyProgressOnlyWhenPercentageAdvances() {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        ProgressTracker tracker = new ProgressTracker(10000, listener);
        tracker.updateProgress(50);
        tracker.updateProgress(49);
        tracker.updateProgress(1);
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(50L, 10000L, 0);
        inOrder.verify(listener).onProgress(100L, 10000L, 1);
        verify(listener, times(2)).onProgress(anyLong(), anyLong(), anyInt());
    }

    @Test
    void shouldNotifyProgressWhenBytesReachMegabyteThreshold() {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        // 总量为 0 时百分比恒为 -1，通知完全由字节数阈值驱动
        ProgressTracker tracker = new ProgressTracker(0, listener);
        tracker.updateProgress(1024L * 1024L);
        tracker.updateProgress(1024L * 1024L);
        verify(listener).onProgress(1024L * 1024L, 0L, -1);
        verify(listener).onProgress(2L * 1024L * 1024L, 0L, -1);
        verify(listener, times(2)).onProgress(anyLong(), anyLong(), anyInt());
    }

    @Test
    void shouldAutoCompleteAtHundredPercent() {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        ProgressTracker tracker = new ProgressTracker(100, listener);
        tracker.start();
        tracker.start();
        tracker.updateProgress(50);
        tracker.updateProgress(50);
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onStart();
        inOrder.verify(listener).onProgress(50L, 100L, 50);
        inOrder.verify(listener).onProgress(100L, 100L, 100);
        inOrder.verify(listener).onComplete();

        tracker.updateProgress(1);
        tracker.complete();
        verify(listener, times(1)).onStart();
        verify(listener, times(1)).onComplete();
        verify(listener, times(2)).onProgress(anyLong(), anyLong(), anyInt());
    }

    @Test
    void shouldIgnoreProgressAfterCompleteButReportError() {
        // CALLS_REAL_METHODS 使接口 default 回调真实执行，与既有 FileStorageServiceTest 用法一致
        UploadProgressListener listener = mock(UploadProgressListener.class,
            CALLS_REAL_METHODS);
        ProgressTracker tracker = new ProgressTracker(100, listener);
        tracker.start();
        tracker.complete();
        tracker.updateProgress(100);
        verify(listener, never()).onProgress(anyLong(), anyLong(), anyInt());
        verify(listener, times(1)).onStart();
        verify(listener, times(1)).onComplete();

        IllegalStateException failure = new IllegalStateException("io");
        tracker.error(failure);
        verify(listener).onError(same(failure));
    }

    @Test
    void shouldNotifyNothingWithoutListener() {
        ProgressTracker tracker = new ProgressTracker(1000, null);
        assertDoesNotThrow(() -> {
            tracker.start();
            tracker.updateProgress(500);
            tracker.complete();
        });
    }
}
