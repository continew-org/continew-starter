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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.hutool.core.io.IORuntimeException;
import org.apache.commons.fileupload.FileItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MultipartFile 工具类测试
 *
 * <p>
 * 锁定 FileItem 创建的字段命名、流拷贝与 BaseException 包装行为。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MultipartFileUtilsTest {

    /**
     * 构造测试字节内容
     *
     * @return 测试内容字节
     */
    private static byte[] sampleContent() {
        return "continew-starter".getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("toMultipartFile 按字节与文件名生成默认字段名为 file 的部件")
    void shouldConvertBytesToMultipartFile() throws IOException {
        byte[] content = sampleContent();
        MultipartFile file = MultipartFileUtils.toMultipartFile(content, "a.txt");
        assertThat(file).isInstanceOf(CommonsMultipartFile.class);
        assertThat(file.getName()).isEqualTo("file");
        assertThat(file.getOriginalFilename()).isEqualTo("a.txt");
        assertThat(file.getSize()).isEqualTo(content.length);
        assertThat(file.getBytes()).isEqualTo(content);
        assertThat(file.getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    @Test
    @DisplayName("空字节数组转换后 isEmpty 为 true")
    void shouldReportEmptyForZeroLengthBytes() {
        MultipartFile file = MultipartFileUtils.toMultipartFile(new byte[0], "empty.txt");
        assertThat(file.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("toMultipartFile 按文件内容生成部件")
    void shouldConvertFileToMultipartFile(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("source.txt");
        byte[] content = sampleContent();
        Files.write(source, content);
        MultipartFile file = MultipartFileUtils.toMultipartFile(source.toFile());
        assertThat(file.getOriginalFilename()).isEqualTo("source.txt");
        assertThat(file.getBytes()).isEqualTo(content);
    }

    @Test
    @DisplayName("createFileItem 支持自定义字段名与内容类型")
    void shouldCreateFileItemWithCustomFields() {
        FileItem item = MultipartFileUtils.createFileItem(new ByteArrayInputStream(sampleContent()),
            "custom-field", "data.json", MediaType.APPLICATION_JSON_VALUE);
        assertThat(item.getFieldName()).isEqualTo("custom-field");
        assertThat(item.getName()).isEqualTo("data.json");
        assertThat(item.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(item.get()).isEqualTo(sampleContent());
    }

    @Test
    @DisplayName("createFileItem 流拷贝失败时抛出 IORuntimeException（锁定当前行为：BaseException 分支被 Hutool 运行时包装绕过）")
    void shouldWrapIoExceptionAsBaseException() {
        InputStream brokenStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("stream broken");
            }
        };
        // IoUtil.copy 先将 IOException 包装为 IORuntimeException 抛出，不落入 createFileItem 的 catch (IOException)
        assertThatThrownBy(() -> MultipartFileUtils.createFileItem(brokenStream, "a.txt"))
            .isInstanceOf(IORuntimeException.class)
            .hasMessageContaining("stream broken");
    }
}
