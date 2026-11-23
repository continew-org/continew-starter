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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 进度监听输入流测试
 *
 * <p>
 * 流内容必须与底层流一致；构造即触发 {@code onStart}，按实际读取字节数累计进度并按百分比阈值
 * 回调，{@code close} 关闭底层流且 {@code onComplete} 幂等，读取失败不产生进度回调。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ProgressInputStreamTest {

    @Test
    void shouldStartTrackerOnConstructionAndForwardSingleByteRead() throws IOException {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        ProgressTracker tracker = new ProgressTracker(3, listener);
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        ProgressInputStream in = new ProgressInputStream(new ByteArrayInputStream(data), tracker);
        verify(listener).onStart();
        assertEquals('a', in.read());
        assertEquals('b', in.read());
        assertEquals('c', in.read());
        assertEquals(-1, in.read());
        in.close();
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onStart();
        inOrder.verify(listener).onProgress(1L, 3L, 33);
        inOrder.verify(listener).onProgress(2L, 3L, 66);
        inOrder.verify(listener).onProgress(3L, 3L, 100);
        // 关闭不再自动判定成功，完成由调用方显式 complete
        verify(listener, never()).onComplete();
    }

    @Test
    void shouldForwardBlockReadWithContentConsistency() throws IOException {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        ProgressTracker tracker = new ProgressTracker(11, listener);
        byte[] data = "hello-world".getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[64];
        try (ProgressInputStream in = new ProgressInputStream(new ByteArrayInputStream(data),
            tracker)) {
            int bytesRead = in.read(buffer, 0, buffer.length);
            assertEquals(data.length, bytesRead);
            assertArrayEquals(data, Arrays.copyOf(buffer, bytesRead));
            assertEquals(-1, in.read(buffer, 0, buffer.length));
        }
        verify(listener).onProgress((long) data.length, 11L, 100);
        verify(listener, never()).onComplete();
    }

    @Test
    void shouldCloseUnderlyingStreamExactlyOncePerClose() throws IOException {
        InputStream underlying = mock(InputStream.class);
        UploadProgressListener listener = mock(UploadProgressListener.class);
        ProgressInputStream in = new ProgressInputStream(underlying, new ProgressTracker(1,
            listener));
        in.close();
        verify(underlying, times(1)).close();
        // 不随关闭上报成功，避免流中断被误报为完成
        verify(listener, never()).onComplete();
    }

    @Test
    void shouldPropagateReadFailureWithoutProgressCallback() {
        UploadProgressListener listener = mock(UploadProgressListener.class);
        ProgressTracker tracker = new ProgressTracker(1, listener);
        InputStream failing = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };
        ProgressInputStream in = new ProgressInputStream(failing, tracker);
        assertThrows(IOException.class, in::read);
        verify(listener, never()).onProgress(anyLong(), anyLong(), anyInt());
    }
}
