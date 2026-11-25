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
    // 服务内部显式成功/失败判定模式：100% 不自动判定完成
    private final boolean manualCompletion;

    // 通知阈值：至少变化1%或者达到 1MB 阈值
    private static final int PERCENTAGE_THRESHOLD = 1;
    private static final long BYTES_THRESHOLD = 1024L * 1024L;

    public ProgressTracker(long totalBytes, UploadProgressListener listener) {
        this(totalBytes, listener, false);
    }

    /**
     * 构造进度跟踪器
     *
     * @param totalBytes       上传总字节数（未知传 0 或负值）
     * @param listener         进度监听器
     * @param manualCompletion true 时 100% 仅上报进度，完成由调用方显式
     *                         {@link #complete()} 判定（服务内部上传链路用）
     */
    public ProgressTracker(long totalBytes, UploadProgressListener listener,
        boolean manualCompletion) {
        this.totalBytes = totalBytes;
        this.listener = listener;
        this.manualCompletion = manualCompletion;
    }

    /**
     * 标记上传开始并回调监听器，重复调用不会重复回调
     */
    public void start() {
        if (started.compareAndSet(false, true) && listener != null) {
            listener.onStart();
        }
    }

    /**
     * 更新上传进度
     * <p>
     * 累计已上传字节数，当百分比变化达到 1% 或字节数变化达到 1MB 阈值时回调监听器；
     * 默认模式达到 100% 自动判定完成，manualCompletion 模式仅上报进度、由调用方显式判定
     *
     * @param bytes 本次新增的已上传字节数
     */
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

        // 使用CAS更新，避免并发问题
        if (shouldNotify && lastPercentage.compareAndSet(lastPct, currentPercentage)) {
            lastNotifiedBytes.set(currentBytes);
            listener.onProgress(currentBytes, totalBytes, currentPercentage);

            // 默认模式 100% 判定完成；manualCompletion 模式由调用方显式判定
            if (!manualCompletion && currentPercentage == 100) {
                complete();
            }
        }
    }

    /**
     * 是否为手动完成判定模式（进度包装流据此决定是否关闭即完成）
     *
     * @return true 表示完成由调用方显式判定
     */
    boolean isManualCompletion() {
        return manualCompletion;
    }

    /**
     * 标记上传完成并回调监听器，仅首次调用生效
     */
    public void complete() {
        if (completed.compareAndSet(false, true) && listener != null) {
            listener.onComplete();
        }
    }

    /**
     * 上报上传异常并回调监听器
     *
     * @param e 上传过程中发生的异常
     */
    public void error(Exception e) {
        if (listener != null) {
            listener.onError(e);
        }
    }
}
