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

package top.continew.starter.storage.domain.file;

import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.processor.progress.ProgressInputStream;
import top.continew.starter.storage.processor.progress.ProgressTracker;
import top.continew.starter.storage.processor.progress.UploadProgressListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 进度监听 MultipartFile 包装器
 *
 * @author echo
 * @since 2.14.0
 */
public class ProgressAwareMultipartFile extends EnhancedMultipartFile {

    private final UploadProgressListener progressListener;
    private volatile ProgressTracker progressTracker;
    private volatile boolean progressEnabled = false;
    private final Object progressLock = new Object();

    /**
     * 读取阶段（用于区分不同的读取阶段）
     */
    public enum ReadPhase {
        VALIDATION, // 验证阶段
        THUMBNAIL, // 缩略图生成
        UPLOAD // 实际上传
    }

    private volatile ReadPhase currentPhase = ReadPhase.VALIDATION;

    public ProgressAwareMultipartFile(MultipartFile originalFile,
        boolean enableCache,
        UploadProgressListener progressListener) {
        super(originalFile, enableCache);
        this.progressListener = progressListener;
    }

    /**
     * 设置当前读取阶段
     */
    public void setReadPhase(ReadPhase phase) {
        synchronized (progressLock) {
            this.currentPhase = phase;
            // 只在上传阶段启用进度
            this.progressEnabled = (phase == ReadPhase.UPLOAD);

            // 切换到上传阶段时，重置进度追踪器
            if (phase == ReadPhase.UPLOAD && progressListener != null) {
                this.progressTracker = new ProgressTracker(getSize(), progressListener);
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream originalStream = super.getInputStream();

        synchronized (progressLock) {
            // 只有在上传阶段且有监听器时才包装流
            if (progressEnabled && progressTracker != null) {
                return new ProgressInputStream(originalStream, progressTracker);
            }
        }

        return originalStream;
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        if (progressEnabled && progressTracker != null) {
            // 使用带进度的传输
            try (InputStream in = getInputStream(); OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } else {
            super.transferTo(dest);
        }
    }

    /**
     * 获取不带进度监听的输入流（向后兼容）
     */
    public InputStream getInputStreamWithoutProgress() throws IOException {
        return super.getInputStream();
    }

    /**
     * 清理资源
     */
    @Override
    public void clearCache() {
        super.clearCache();
        synchronized (progressLock) {
            progressTracker = null;
        }
    }
}
