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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.processor.progress.UploadProgressListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 进度监听 MultipartFile 包装器测试
 *
 * <p>
 * 只有进入 {@code ReadPhase.UPLOAD} 且存在监听器时，输入流与 {@code transferTo} 才经进度包装并按
 * 「开始 → 进度 → 完成」回调；其余阶段（含监听器为空、清理缓存后）均为无进度直通。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ProgressAwareMultipartFileTest {

    private static final byte[] CONTENT = "progress-content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    File tempDir;

    @Test
    void shouldPassThroughOutsideUploadPhase() throws IOException {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        MultipartFile original = this.mockOriginal();
        ProgressAwareMultipartFile file = new ProgressAwareMultipartFile(original, false, listener);
        file.setReadPhase(ProgressAwareMultipartFile.ReadPhase.VALIDATION);
        try (InputStream in = file.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
        file.setReadPhase(ProgressAwareMultipartFile.ReadPhase.THUMBNAIL);
        File dest = this.tempDir.toPath().resolve("thumbnail.bin").toFile();
        file.transferTo(dest);
        assertArrayEquals(CONTENT, Files.readAllBytes(dest.toPath()));
        verifyNoInteractions(listener);
    }

    @Test
    void shouldNotWrapStreamWithoutListener() throws IOException {
        MultipartFile original = this.mockOriginal();
        ProgressAwareMultipartFile file = new ProgressAwareMultipartFile(original, false, null);
        file.setReadPhase(ProgressAwareMultipartFile.ReadPhase.UPLOAD);
        try (InputStream in = file.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
        File dest = this.tempDir.toPath().resolve("no-listener.bin").toFile();
        file.transferTo(dest);
        assertArrayEquals(CONTENT, Files.readAllBytes(dest.toPath()));
    }

    @Test
    void shouldTrackProgressWhileReadingUploadStream() throws IOException {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        MultipartFile original = this.mockOriginal();
        ProgressAwareMultipartFile file = new ProgressAwareMultipartFile(original, false, listener);
        file.setReadPhase(ProgressAwareMultipartFile.ReadPhase.UPLOAD);
        try (InputStream in = file.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onStart();
        inOrder.verify(listener).onProgress((long) CONTENT.length, (long) CONTENT.length, 100);
        // 仅读取不判定完成，完成由上传方显式触发
        verify(listener, never()).onComplete();
    }

    @Test
    void shouldTrackProgressOnTransferToInUploadPhase() throws IOException {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        MultipartFile original = this.mockOriginal();
        ProgressAwareMultipartFile file = new ProgressAwareMultipartFile(original, false, listener);
        file.setReadPhase(ProgressAwareMultipartFile.ReadPhase.UPLOAD);
        File dest = this.tempDir.toPath().resolve("transfer.bin").toFile();
        file.transferTo(dest);
        assertArrayEquals(CONTENT, Files.readAllBytes(dest.toPath()));
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onStart();
        inOrder.verify(listener).onProgress((long) CONTENT.length, (long) CONTENT.length, 100);
        inOrder.verify(listener).onComplete();
    }

    @Test
    void shouldBypassProgressForRawStreamAndAfterClearCache() throws IOException {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        MultipartFile original = this.mockOriginal();
        ProgressAwareMultipartFile file = new ProgressAwareMultipartFile(original, true, listener);
        file.setReadPhase(ProgressAwareMultipartFile.ReadPhase.UPLOAD);
        try (InputStream raw = file.getInputStreamWithoutProgress()) {
            assertArrayEquals(CONTENT, raw.readAllBytes());
        }
        verifyNoInteractions(listener);
        file.clearCache();
        try (InputStream in = file.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
        verifyNoInteractions(listener);
    }

    private MultipartFile mockOriginal() throws IOException {
        MultipartFile original = mock(MultipartFile.class);
        when(original.getName()).thenReturn("file");
        when(original.getOriginalFilename()).thenReturn("a.txt");
        when(original.getContentType()).thenReturn("text/plain");
        when(original.isEmpty()).thenReturn(false);
        when(original.getSize()).thenReturn((long) CONTENT.length);
        when(original.getBytes()).thenReturn(CONTENT);
        when(original.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(CONTENT));
        doAnswer(inv -> {
            Files.write(((File) inv.getArgument(0)).toPath(), CONTENT);
            return null;
        }).when(original).transferTo(any(File.class));
        return original;
    }
}
