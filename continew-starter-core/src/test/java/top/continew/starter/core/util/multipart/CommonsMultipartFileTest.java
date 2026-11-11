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

package top.continew.starter.core.util.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CommonsMultipartFile 测试
 *
 * <p>
 * 锁定文件名路径剥离（默认剥离、preserveFilename 保留）与内存部件的内容读取契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CommonsMultipartFileTest {

    /**
     * 构造内存 FileItem
     *
     * @param fieldName 字段名
     * @param fileName  文件名
     * @param content   内容字节
     * @return 内存存储的 FileItem
     */
    private static FileItem buildMemoryItem(String fieldName, String fileName, byte[] content) {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        FileItem item = factory.createItem(fieldName, "multipart/form-data", true, fileName);
        try {
            item.getOutputStream().write(content);
        } catch (IOException e) {
            throw new IllegalStateException("测试部件写入失败", e);
        }
        return item;
    }

    @Test
    @DisplayName("包装内存部件后元信息与内容可读")
    void shouldExposeItemMetadata() {
        byte[] content = "continew".getBytes(StandardCharsets.UTF_8);
        CommonsMultipartFile file =
            new CommonsMultipartFile(buildMemoryItem("file", "a.txt", content));
        assertThat(file.getFileItem()).isInstanceOf(FileItem.class);
        assertThat(file.getName()).isEqualTo("file");
        assertThat(file.getContentType()).isEqualTo("multipart/form-data");
        assertThat(file.getSize()).isEqualTo(content.length);
        assertThat(file.isEmpty()).isFalse();
        assertThat(file.getBytes()).isEqualTo(content);
        assertThat(file.getStorageDescription()).isEqualTo("in memory");
    }

    @Test
    @DisplayName("getOriginalFilename 默认剥离 Unix 与 Windows 路径成分")
    void shouldStripPathFromOriginalFilename() {
        CommonsMultipartFile unixStyle =
            new CommonsMultipartFile(buildMemoryItem("file", "/var/tmp/a.txt", new byte[0]));
        CommonsMultipartFile winStyle =
            new CommonsMultipartFile(buildMemoryItem("file", "C:\\tmp\\b.txt", new byte[0]));
        assertThat(unixStyle.getOriginalFilename()).isEqualTo("a.txt");
        assertThat(winStyle.getOriginalFilename()).isEqualTo("b.txt");
    }

    @Test
    @DisplayName("setPreserveFilename(true) 保留客户端原始路径")
    void shouldPreserveOriginalFilenameWhenEnabled() {
        CommonsMultipartFile file =
            new CommonsMultipartFile(buildMemoryItem("file", "/var/tmp/a.txt", new byte[0]));
        file.setPreserveFilename(true);
        assertThat(file.getOriginalFilename()).isEqualTo("/var/tmp/a.txt");
    }

    @Test
    @DisplayName("toString 包含字段名、文件名与大小")
    void shouldDescribeItemInToString() {
        CommonsMultipartFile file =
            new CommonsMultipartFile(buildMemoryItem("file", "a.txt", new byte[] {1}));
        assertThat(file.toString()).contains("file").contains("a.txt").contains("size=1");
    }

    @Test
    @DisplayName("transferTo(Path) 将部件内容写出目标文件")
    void shouldTransferToPath(@TempDir Path tempDir) throws Exception {
        byte[] content = "transfer-content".getBytes(StandardCharsets.UTF_8);
        CommonsMultipartFile file =
            new CommonsMultipartFile(buildMemoryItem("file", "a.txt", content));
        Path dest = tempDir.resolve("dest.txt");
        file.transferTo(dest);
        assertThat(dest).hasBinaryContent(content);
    }

    @Test
    @DisplayName("transferTo(File) 覆盖已存在的目标文件")
    void shouldTransferToExistingFile(@TempDir Path tempDir) throws Exception {
        byte[] content = "new-content".getBytes(StandardCharsets.UTF_8);
        CommonsMultipartFile file =
            new CommonsMultipartFile(buildMemoryItem("file", "a.txt", content));
        File dest = tempDir.resolve("dest.txt").toFile();
        Files.write(dest.toPath(), "old-content".getBytes(StandardCharsets.UTF_8));
        file.transferTo(dest);
        assertThat(dest).hasBinaryContent(content);
    }
}
