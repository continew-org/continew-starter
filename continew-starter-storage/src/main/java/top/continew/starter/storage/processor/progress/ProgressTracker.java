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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进度跟踪器
 *
 * @author echo
 * @since 2.14.0
 */
public class ProgressTracker {

    private final long totalBytes;
    private final UploadProgressListener listener;
    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong lastNotifiedBytes = new AtomicLong(0);
    private final AtomicInteger lastPercentage = new AtomicInteger(-1);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);

    // 通知阈值：至少变化1%或者达到 1MB 阈值
    private static final int PERCENTAGE_THRESHOLD = 1;
    private static final long BYTES_THRESHOLD = 1024 * 1024;

    public ProgressTracker(long totalBytes, UploadProgressListener listener) {
        this.totalBytes = totalBytes;
        this.listener = listener;
    }

    public void start() {
        if (started.compareAndSet(false, true) && listener != null) {
            listener.onStart();
        }
    }

    public void updateProgress(long bytes) {
        if (completed.get() || listener == null) {
            return;
        }

        long currentBytes = bytesRead.addAndGet(bytes);
        int currentPercentage = totalBytes > 0 ? (int) ((currentBytes * 100L) / totalBytes) : -1;

        // 检查是否需要通知
        boolean shouldNotify = false;
        int lastPct = lastPercentage.get();

        if (currentPercentage >= 0) {
            // 百分比变化达到阈值
            if (currentPercentage - lastPct >= PERCENTAGE_THRESHOLD) {
                shouldNotify = true;
            }
            // 达到100%必须通知
            if (currentPercentage == 100 && lastPct != 100) {
                shouldNotify = true;
            }
        }

        // 字节数变化达到阈值
        if (currentBytes - lastNotifiedBytes.get() >= BYTES_THRESHOLD) {
            shouldNotify = true;
        }

        if (shouldNotify) {
            // 使用CAS更新，避免并发问题
            if (lastPercentage.compareAndSet(lastPct, currentPercentage)) {
                lastNotifiedBytes.set(currentBytes);
                listener.onProgress(currentBytes, totalBytes, currentPercentage);

                // 如果达到100%，标记完成
                if (currentPercentage == 100) {
                    complete();
                }
            }
        }
    }

    public void complete() {
        if (completed.compareAndSet(false, true) && listener != null) {
            listener.onComplete();
        }
    }

    public void error(Exception e) {
        if (listener != null) {
            listener.onError(e);
        }
    }
}
