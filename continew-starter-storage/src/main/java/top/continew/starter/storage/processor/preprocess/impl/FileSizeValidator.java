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

package top.continew.starter.storage.processor.preprocess.impl;

import cn.hutool.core.io.FileUtil;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.processor.preprocess.FileValidator;

/**
 * 文件大小验证器
 *
 * @author echo
 * @since 2.14.0
 */
public class FileSizeValidator implements FileValidator {

    private final long maxSize;

    public FileSizeValidator(MultipartProperties multipartProperties) {
        this.maxSize = multipartProperties.getMaxFileSize().toBytes();
    }

    public FileSizeValidator() {
        this(StorageConstant.DEFAULT_FILE_SIZE);
    }

    public FileSizeValidator(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public String getName() {
        return FileSizeValidator.class.getSimpleName();
    }

    @Override
    public boolean support(UploadContext context) {
        return true;
    }

    @Override
    public void validate(UploadContext context) throws StorageException {
        long fileSize = context.getFile().getSize();
        if (fileSize > maxSize) {
            throw new StorageException(String.format("文件大小超过限制: %s (当前: %s)", FileUtil
                .readableFileSize(maxSize), FileUtil.readableFileSize(fileSize)));
        }
    }

    /**
     * 创建默认的文件大小验证器（10MB）
     *
     * @return 文件大小验证器
     */
    public FileSizeValidator create() {
        return new FileSizeValidator(maxSize);
    }

    /**
     * 创建指定大小的验证器
     *
     * @param bytes 最大文件大小（字节）
     * @return 文件大小验证器
     */
    public static FileSizeValidator maxSize(long bytes) {
        return new FileSizeValidator(bytes);
    }

    /**
     * 创建指定KB大小的验证器
     *
     * @param kb 最大文件大小（KB）
     * @return 文件大小验证器
     */
    public static FileSizeValidator maxKB(long kb) {
        return new FileSizeValidator(kb * 1024);
    }

    /**
     * 创建指定MB大小的验证器
     *
     * @param mb 最大文件大小（MB）
     * @return 文件大小验证器
     */
    public static FileSizeValidator maxMB(long mb) {
        return new FileSizeValidator(mb * 1024 * 1024);
    }

    /**
     * 创建指定GB大小的验证器
     *
     * @param gb 最大文件大小（GB）
     * @return 文件大小验证器
     */
    public static FileSizeValidator maxGB(long gb) {
        return new FileSizeValidator(gb * 1024L * 1024 * 1024);
    }
}
