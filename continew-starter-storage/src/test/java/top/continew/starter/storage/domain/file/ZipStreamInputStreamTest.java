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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * {@link ZipStreamInputStream} 按需生成 ZIP 流单元测试。
 *
 * <p>多源条目流式打包、单字节/按块读取语义、close 释放源流等核心行为
 * 由内存流即可裁决。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("ZipStreamInputStream 按需生成 ZIP 单元测试")
class ZipStreamInputStreamTest {

    private static InputStream open(byte[] data) {
        return new ByteArrayInputStream(data);
    }

    private static List<String> entryNames(byte[] zip) throws IOException {
        List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                names.add(e.getName());
            }
        }
        return names;
    }

    @Test
    @DisplayName("多源条目按需打包为合法 ZIP，条目名经 entryNamer 映射")
    void shouldStreamZipWithMappedEntryNames() throws IOException {
        java.util.Map<String, byte[]> sources = java.util.Map.of("p1", "A"
            .getBytes(StandardCharsets.UTF_8), "p2", "B".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (ZipStreamInputStream zip = new ZipStreamInputStream(List.of("p1", "p2"),
            p -> open(sources.get(p)), p -> "n-" + p)) {
            zip.transferTo(sink);
        }
        assertEquals(List.of("n-p1", "n-p2"), entryNames(sink.toByteArray()));
    }

    @Test
    @DisplayName("read() 单字节与 read(byte[],off,len) 语义一致")
    void shouldReadSingleAndBulkConsistently() throws IOException {
        byte[] content = "xy".getBytes(StandardCharsets.UTF_8);
        try (ZipStreamInputStream zip = new ZipStreamInputStream(List.of("p"),
            k -> open(content), p -> p)) {
            assertTrue(zip.read() >= 0, "ZIP 头部即产出字节");
            byte[] buf = new byte[16];
            int n = zip.read(buf, 0, buf.length);
            assertTrue(n > 0);
            assertEquals(0, zip.read(buf, 0, 0), "len=0 读返回 0");
            int avail = zip.available();
            assertTrue(avail >= 0);
        }
    }

    @Test
    @DisplayName("空路径列表产出仅含末尾目录记录的空 ZIP")
    void shouldProduceEmptyZipForNoSources() throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (ZipStreamInputStream zip = new ZipStreamInputStream(List.of(), k -> open(new byte[0]),
            p -> p)) {
            zip.transferTo(sink);
        }
        // 空 ZIP 仍以 EOCD 结尾
        assertTrue(sink.size() > 0);
    }

    @Test
    @DisplayName("close 关闭当前条目源流")
    void shouldCloseCurrentEntryStream() throws IOException {
        TrackingStream tracking = new TrackingStream("z".getBytes(StandardCharsets.UTF_8));
        ZipStreamInputStream zip = new ZipStreamInputStream(List.of("p"), k -> tracking, p -> p);
        // 触发开启第一条目
        zip.read();
        zip.close();
        assertTrue(tracking.closed, "关闭 ZIP 流应连带关闭当前条目源流");
    }

    /**
     * 记录关闭状态的源输入流
     */
    private static class TrackingStream extends InputStream {

        private final ByteArrayInputStream delegate;
        private boolean closed;

        TrackingStream(byte[] data) {
            this.delegate = new ByteArrayInputStream(data);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
