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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * {@link InputStreamMultipartFile} 单次读流语义单元测试。
 *
 * <p>不可复读、不支持 getBytes/transferTo、实际读取字节数回填与幂等关闭，
 * 是单次读契约的核心行为。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("InputStreamMultipartFile 单次读流语义单元测试")
class InputStreamMultipartFileTest {

    @Test
    @DisplayName("getInputStream 仅可取一次，二次获取抛错")
    void shouldAllowSingleGetInputStream() throws IOException {
        byte[] content = "one-shot".getBytes(StandardCharsets.UTF_8);
        InputStreamMultipartFile file = new InputStreamMultipartFile("f", "a.bin",
            "application/octet-stream", content.length,
            new ByteArrayInputStream(content));
        try (InputStream in = file.getInputStream()) {
            assertArrayEquals(content, in.readAllBytes());
        }
        assertThrows(IOException.class, file::getInputStream);
    }

    @Test
    @DisplayName("getBytes 与 transferTo 不支持，直接抛 IOException")
    void shouldRejectBulkAccessors() {
        InputStreamMultipartFile file = new InputStreamMultipartFile("f", "a.bin",
            "application/octet-stream", 3, new ByteArrayInputStream(new byte[3]));
        assertThrows(IOException.class, file::getBytes);
        assertThrows(IOException.class, () -> file.transferTo(new java.io.File("x")));
    }

    @Test
    @DisplayName("closeStream 幂等，重复调用不抛错且源流只关一次")
    void shouldCloseStreamIdempotently() throws IOException {
        TrackingStream tracking = new TrackingStream();
        InputStreamMultipartFile file = new InputStreamMultipartFile("f", "a.bin",
            "application/octet-stream", -1, tracking);
        file.closeStream();
        file.closeStream();
        assertEquals(1, tracking.closeCount, "幂等关闭底层流只应关闭一次");
    }

    @Test
    @DisplayName("actualSize 回填与 isEmpty 语义")
    void shouldBackfillActualSizeAndEmptiness() {
        InputStreamMultipartFile file = new InputStreamMultipartFile("f", "a.bin",
            "application/octet-stream", 0, new ByteArrayInputStream(new byte[0]));
        assertTrue(file.isEmpty());
        assertEquals(-1, file.getActualSize());
        file.setActualSize(7);
        assertEquals(7, file.getActualSize());
    }

    /**
     * 记录关闭次数的输入流
     */
    private static class TrackingStream extends InputStream {

        private int closeCount;

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() {
            closeCount++;
        }
    }
}
