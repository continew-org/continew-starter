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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link TempFileMultipartFile} 落盘临时文件语义单元测试。
 *
 * <p>物化、复读、幂等删除与权限收敛均可在本地文件系统裁决。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("TempFileMultipartFile 落盘临时文件单元测试")
class TempFileMultipartFileTest {

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("物化后可重复读取，元数据透传")
    void shouldMaterializeAndAllowRepeatableReads() throws IOException {
        byte[] content = "temp-content".getBytes(StandardCharsets.UTF_8);
        TempFileMultipartFile file = TempFileMultipartFile.materialize("f", "a.txt",
            "text/plain", new ByteArrayInputStream(content));
        try {
            assertEquals("f", file.getName());
            assertEquals("a.txt", file.getOriginalFilename());
            assertEquals("text/plain", file.getContentType());
            assertEquals(content.length, file.getSize());
            assertFalse(file.isEmpty());
            assertArrayEquals(content, file.getBytes());
            try (InputStream in = file.getInputStream()) {
                assertArrayEquals(content, in.readAllBytes());
            }
        } finally {
            file.delete();
        }
    }

    @Test
    @DisplayName("POSIX 平台上物化文件权限限属主读写")
    void shouldRestrictTempFilePermissionsOnPosix() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(FileSystems.getDefault()
            .supportedFileAttributeViews().contains("posix"));
        TempFileMultipartFile file = TempFileMultipartFile.materialize("f", "a.txt",
            "text/plain", new ByteArrayInputStream(new byte[] {1}));
        try {
            assertEquals(PosixFilePermissions.fromString("rw-------"),
                Files.getPosixFilePermissions(tempFilePath(file)));
        } finally {
            file.delete();
        }
    }

    @Test
    @DisplayName("POSIX 平台上物化写入期间权限始终限属主读写")
    void shouldKeepOwnerOnlyPermissionsDuringWrite() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(FileSystems.getDefault()
            .supportedFileAttributeViews().contains("posix"));
        // 探测流在每次 read 时抓取刚落盘的临时文件权限；newOutputStream 不重建文件，
        // 写入窗口内权限必须保持 rw-------（Files.copy REPLACE_EXISTING 会放宽为默认）
        ProbingInputStream in = new ProbingInputStream(
            "perm-during-write".getBytes(StandardCharsets.UTF_8), uploadTempNames());
        TempFileMultipartFile file = TempFileMultipartFile.materialize("f", "p.txt",
            "text/plain", in);
        try {
            assertFalse(in.observed.isEmpty(), "探测流应在写入期间采到权限");
            for (Set<PosixFilePermission> perms : in.observed) {
                assertEquals(PosixFilePermissions.fromString("rw-------"), perms,
                    "写入期间权限不得放宽为默认");
            }
            assertEquals(PosixFilePermissions.fromString("rw-------"),
                Files.getPosixFilePermissions(tempFilePath(file)));
        } finally {
            file.delete();
        }
    }

    @Test
    @DisplayName("transferTo 复制内容到目标文件")
    void shouldTransferToDestination() throws IOException {
        byte[] content = "copy-me".getBytes(StandardCharsets.UTF_8);
        TempFileMultipartFile file = TempFileMultipartFile.materialize("f", "a.txt",
            "text/plain", new ByteArrayInputStream(content));
        try {
            Path dest = tempDir.resolve("out.bin");
            file.transferTo(dest.toFile());
            assertArrayEquals(content, Files.readAllBytes(dest));
        } finally {
            file.delete();
        }
    }

    @Test
    @DisplayName("delete 幂等，重复调用不抛错")
    void shouldDeleteIdempotently() throws Exception {
        TempFileMultipartFile file = TempFileMultipartFile.materialize("f", "a.txt",
            "text/plain", new ByteArrayInputStream(new byte[] {1}));
        Path tempPath = tempFilePath(file);
        assertTrue(Files.exists(tempPath), "物化后临时文件应已落盘");
        file.delete();
        assertFalse(Files.exists(tempPath), "delete 应删除临时文件");
        assertDoesNotThrow(file::delete, "重复 delete 不应抛错");
        assertFalse(Files.exists(tempPath), "重复 delete 不应重建临时文件");
    }

    @Test
    @DisplayName("空内容物化为 size=0 的可复读文件")
    void shouldMaterializeEmptyContent() throws IOException {
        TempFileMultipartFile file = TempFileMultipartFile.materialize("f", "e.txt",
            "text/plain", new ByteArrayInputStream(new byte[0]));
        try {
            assertTrue(file.isEmpty());
            assertEquals(0, file.getSize());
            assertEquals(0, file.getBytes().length);
        } finally {
            file.delete();
        }
    }

    /**
     * 在每次 read 时探测临时目录中最新 storage-upload-*.tmp 权限的输入流
     */
    private static class ProbingInputStream extends InputStream {

        private final byte[] data;
        private final Set<String> baseline;
        private int offset;
        private final List<Set<PosixFilePermission>> observed = new ArrayList<>();

        ProbingInputStream(byte[] data, Set<String> baseline) {
            this.data = data;
            this.baseline = baseline;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            probe();
            if (this.offset >= this.data.length) {
                return -1;
            }
            int n = Math.min(len, this.data.length - this.offset);
            System.arraycopy(this.data, this.offset, b, off, n);
            this.offset += n;
            return n;
        }

        @Override
        public int read() {
            probe();
            return this.offset < this.data.length ? this.data[this.offset++] & 0xFF : -1;
        }

        private void probe() {
            // baseline 排除既有/并行同名临时文件，只观察本次物化新建的文件
            for (Path p : uploadTemps()) {
                if (this.baseline.contains(p.getFileName().toString())) {
                    continue;
                }
                try {
                    this.observed.add(Files.getPosixFilePermissions(p));
                } catch (IOException e) {
                    // 文件可能刚被并发用例删除，跳过该次采样
                }
            }
        }
    }

    /**
     * 枚举临时目录中 storage-upload-*.tmp 文件名（不可枚举返回空集）
     *
     * @return 匹配文件名集
     */
    private static Set<String> uploadTempNames() {
        return uploadTemps().stream().map(p -> p.getFileName().toString())
            .collect(Collectors.toSet());
    }

    /**
     * 枚举临时目录中 storage-upload-*.tmp 路径（不可枚举返回空集）
     *
     * @return 匹配路径集
     */
    private static Set<Path> uploadTemps() {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().startsWith("storage-upload-"))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            return Set.of();
        }
    }

    /**
     * 读取物化临时文件路径（仅测试断言用）
     *
     * @param file 临时文件型 MultipartFile
     * @return 临时文件路径
     * @throws Exception 反射失败时抛出
     */
    private static Path tempFilePath(TempFileMultipartFile file) throws Exception {
        Field field = TempFileMultipartFile.class.getDeclaredField("tempFile");
        field.setAccessible(true);
        return (Path) field.get(file);
    }
}
