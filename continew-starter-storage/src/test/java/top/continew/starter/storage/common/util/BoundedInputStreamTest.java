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

package top.continew.starter.storage.common.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@link BoundedInputStream} 限长语义单元测试。
 *
 * <p>软限/硬限边界、skip 与不可 mark 行为由纯字节流即可裁决。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("BoundedInputStream 限长语义单元测试")
class BoundedInputStreamTest {

    @Test
    @DisplayName("硬限模式底层还有剩余即抛错；恰好达上限时底层读空则返回 EOF")
    void shouldThrowOnlyWhenUnderlyingHasMore() throws IOException {
        byte[] data = "0123456789".getBytes(StandardCharsets.UTF_8);
        try (BoundedInputStream over = new BoundedInputStream(
            new ByteArrayInputStream(data), 4, true)) {
            byte[] buf = new byte[4];
            over.read(buf);
            assertThrows(IOException.class, over::read);
        }
        try (BoundedInputStream exactly = new BoundedInputStream(
            new ByteArrayInputStream("0123".getBytes(StandardCharsets.UTF_8)), 4, true)) {
            byte[] buf = new byte[4];
            assertEquals(4, exactly.read(buf));
            assertEquals(-1, exactly.read(), "底层恰好读空不触发硬限探测失败");
        }
    }

    @Test
    @DisplayName("read() 单字节在软限下截断返回 EOF")
    void shouldCutSingleByteReadAtSoftLimit() throws IOException {
        try (BoundedInputStream in = new BoundedInputStream(
            new ByteArrayInputStream("ab".getBytes(StandardCharsets.UTF_8)), 1)) {
            assertEquals('a', in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    @DisplayName("skip 受剩余额度钳制并累计已计量字节")
    void shouldCapSkipAtRemainingBudget() throws IOException {
        try (BoundedInputStream in = new BoundedInputStream(
            new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.UTF_8)), 5)) {
            assertEquals(3, in.skip(3));
            assertEquals(2, in.skip(10), "skip 不得超过剩余额度");
            assertEquals(0, in.skip(1), "额度耗尽后 skip 返回 0");
            assertEquals(5, in.getCount());
        }
    }

    @Test
    @DisplayName("不支持 mark/reset，markSupported 恒为 false")
    void shouldNotSupportMarkReset() throws IOException {
        try (BoundedInputStream in = new BoundedInputStream(
            new ByteArrayInputStream("ab".getBytes(StandardCharsets.UTF_8)), 2)) {
            assertFalse(in.markSupported());
            assertThrows(IOException.class, in::reset);
        }
    }

    @Test
    @DisplayName("软限模式下按块读取超过上限时静默截断")
    void shouldTruncateBulkReadAtSoftLimit() throws IOException {
        byte[] data = "0123456789".getBytes(StandardCharsets.UTF_8);
        try (BoundedInputStream in = new BoundedInputStream(
            new ByteArrayInputStream(data), 4)) {
            byte[] buf = new byte[10];
            int n = in.read(buf);
            assertEquals(4, n);
            assertArrayEquals("0123".getBytes(StandardCharsets.UTF_8),
                java.util.Arrays.copyOf(buf, n));
            assertEquals(-1, in.read(buf));
        }
    }
}
