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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 进度监听输入流包装器
 *
 * @author echo
 * @since 2.14.0
 */
public class ProgressInputStream extends FilterInputStream {

    private final ProgressTracker tracker;

    public ProgressInputStream(InputStream in, ProgressTracker tracker) {
        super(in);
        this.tracker = tracker;
        tracker.start();
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            tracker.updateProgress(1);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead > 0) {
            tracker.updateProgress(bytesRead);
        }
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    /**
     * 进度追踪器（供调用方在上传成功或失败时显式 complete/error）
     *
     * @return 进度追踪器
     */
    public ProgressTracker getTracker() {
        return tracker;
    }
}
