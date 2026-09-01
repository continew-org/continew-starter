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
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.processor.preprocess.FileValidator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件类型验证器
 *
 * @author echo
 * @since 2.14.0
 */
public class FileTypeValidator implements FileValidator {

    private final Set<String> allowedExtensions;

    /**
     * 默认构造函数，允许所有支持的文件类型
     */
    public FileTypeValidator() {
        this.allowedExtensions = null;
    }

    /**
     * 指定允许的文件扩展名
     *
     * @param extensions 允许的扩展名
     */
    public FileTypeValidator(String... extensions) {
        this.allowedExtensions = new HashSet<>(Arrays.asList(extensions));
    }

    @Override
    public String getName() {
        return FileTypeValidator.class.getSimpleName();
    }

    @Override
    public boolean support(UploadContext context) {
        return true;
    }

    @Override
    public void validate(UploadContext context) throws StorageException {
        String filename = context.getFile().getOriginalFilename();
        if (filename != null) {
            String extension = FileUtil.extName(filename).toLowerCase();
            // 如果指定了允许的扩展名，则只允许这些扩展名
            if (allowedExtensions != null && !allowedExtensions.contains(extension)) {
                throw new StorageException("不支持的文件类型: " + extension + "，仅支持: " + String
                    .join(", ", allowedExtensions));
            }
        }
    }
}
