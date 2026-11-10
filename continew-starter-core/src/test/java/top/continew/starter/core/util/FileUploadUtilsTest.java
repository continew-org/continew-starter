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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * 文件工具类测试
 *
 * <p>
 * 锁定上传文件名的时间戳/UUID 规则与路径穿越剥离行为，以及下载响应头的编码与写出契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class FileUploadUtilsTest {

    /**
     * 目标文件名格式：原文件前缀 - 16 位毫秒时间戳 . 扩展名
     */
    private static final Pattern KEEP_ORIGINAL_NAME_PATTERN = Pattern
        .compile("^report-\\d{17}\\.txt$");

    /**
     * 目标文件名格式：32 位十六进制 UUID . 扩展名
     */
    private static final Pattern UUID_NAME_PATTERN = Pattern.compile("^[0-9a-f]{32}\\.txt$");

    @Test
    @DisplayName("上传保留原文件名时以 前缀-时间戳 命名")
    void shouldKeepOriginalFilenameWithTimestamp(@TempDir Path tempDir) {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "report.txt",
            MediaType.TEXT_PLAIN_VALUE, "upload".getBytes(StandardCharsets.UTF_8));
        File dest = FileUploadUtils.upload(multipartFile, tempDir.toString(), true);
        assertThat(dest).isNotNull();
        assertThat(dest.getName()).matches(KEEP_ORIGINAL_NAME_PATTERN);
        assertThat(dest).hasBinaryContent("upload".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("上传不保留原文件名时以 UUID 命名")
    void shouldUseUuidFilenameWithoutKeepOriginal(@TempDir Path tempDir) {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "report.txt",
            MediaType.TEXT_PLAIN_VALUE, "upload".getBytes(StandardCharsets.UTF_8));
        File dest = FileUploadUtils.upload(multipartFile, tempDir.toString(), false);
        assertThat(dest).isNotNull();
        assertThat(dest.getName()).matches(UUID_NAME_PATTERN);
    }

    @Test
    @DisplayName("上传剥离原文件名中的目录成分，路径穿越字符不落盘（锁定当前行为）")
    void shouldStripPathComponentsOnUpload(@TempDir Path tempDir) {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "../../evil.txt",
            MediaType.TEXT_PLAIN_VALUE, "upload".getBytes(StandardCharsets.UTF_8));
        File dest = FileUploadUtils.upload(multipartFile, tempDir.toString(), false);
        assertThat(dest).isNotNull();
        assertThat(dest).hasParent(tempDir.toFile());
        assertThat(dest.getName()).matches(UUID_NAME_PATTERN);
    }

    @Test
    @DisplayName("上传失败时返回 null 而不抛出")
    void shouldReturnNullWhenUploadFails(@TempDir Path tempDir) throws IOException {
        File blockingFile = tempDir.resolve("blocked.txt").toFile();
        assertThat(blockingFile.createNewFile()).isTrue();
        MockMultipartFile multipartFile = new MockMultipartFile("file", "report.txt",
            MediaType.TEXT_PLAIN_VALUE, "upload".getBytes(StandardCharsets.UTF_8));
        // 目标路径是一个已存在的文件而非目录，父目录创建失败后写入必然失败
        File dest = FileUploadUtils.upload(multipartFile, blockingFile.getPath(), true);
        assertThat(dest).isNull();
    }

    @Test
    @DisplayName("按文件下载设置响应头并写出内容")
    void shouldDownloadFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("report.txt").toFile();
        Files.write(file.toPath(), "download-content".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FileUploadUtils.download(response, file);
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("attachment; filename=report.txt");
        assertThat(response.getContentAsByteArray())
            .isEqualTo("download-content".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("按流下载对中文文件名做 URL 编码")
    void shouldEncodeChineseFilenameOnDownload() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        InputStream inputStream = new ByteArrayInputStream("数据".getBytes(StandardCharsets.UTF_8));
        FileUploadUtils.download(response, inputStream, "报表.txt");
        assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("attachment; filename=%E6%8A%A5%E8%A1%A8.txt");
        assertThat(response.getContentAsByteArray())
            .isEqualTo("数据".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("文件不存在时按文件下载抛出 IOException")
    void shouldThrowWhenDownloadMissingFile(@TempDir Path tempDir) {
        File missing = tempDir.resolve("missing.txt").toFile();
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> FileUploadUtils.download(response, missing))
            .isInstanceOf(IOException.class);
    }
}
