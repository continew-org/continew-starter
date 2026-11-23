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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 落盘临时文件型 MultipartFile
 *
 * <p>
 * 内容已物化到临时文件，{@link #getInputStream()} 可重复读取；由 {@link #delete()} 删除
 * 临时文件。用于不可复读流在需要二次读（如缩略图）或目标策略不支持流式上传时的兜底。
 * </p>
 *
 * @author echo
 * @since 3.1.0
 */
public class TempFileMultipartFile implements MultipartFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempFileMultipartFile.class);

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final Path tempFile;
    private final long size;

    /**
     * 构造落盘临时文件型 MultipartFile
     *
     * @param name             参数名
     * @param originalFilename 原始文件名
     * @param contentType      内容类型
     * @param tempFile         临时文件路径
     * @param size             文件大小
     */
    public TempFileMultipartFile(String name, String originalFilename, String contentType,
        Path tempFile, long size) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.tempFile = tempFile;
        this.size = size;
    }

    /**
     * 将输入流物化为临时文件
     *
     * @param name             参数名
     * @param originalFilename 原始文件名
     * @param contentType      内容类型
     * @param in               内容输入流（读取到 EOF，不关闭）
     * @return 临时文件型 MultipartFile
     * @throws IOException 读取或写入失败时抛出
     */
    public static TempFileMultipartFile materialize(String name, String originalFilename,
        String contentType, InputStream in) throws IOException {
        Path tempFile = Files.createTempFile("storage-upload-", ".tmp");
        try {
            long size = Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return new TempFileMultipartFile(name, originalFilename, contentType, tempFile, size);
        } catch (IOException | RuntimeException e) {
            deleteQuietly(tempFile);
            throw e;
        }
    }

    /**
     * 删除临时文件（幂等）
     */
    public void delete() {
        deleteQuietly(tempFile);
    }

    /**
     * 删除临时文件，失败仅记日志
     *
     * @param path 临时文件路径
     */
    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.warn("删除上传临时文件失败: {}", path, e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(tempFile);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(tempFile);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(tempFile, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
