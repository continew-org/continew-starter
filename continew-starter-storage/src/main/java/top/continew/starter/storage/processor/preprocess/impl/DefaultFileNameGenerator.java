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

import cn.hutool.core.util.StrUtil;
import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.processor.preprocess.FileNameGenerator;
import top.continew.starter.storage.common.util.StorageUtils;

/**
 * 默认文件名生成器
 *
 * @author echo
 * @since 2.14.0
 */
public class DefaultFileNameGenerator implements FileNameGenerator {

    @Override
    public String getName() {
        return DefaultFileNameGenerator.class.getSimpleName();
    }

    @Override
    public boolean support(UploadContext context) {
        return StrUtil.isBlank(context.getFormatFileName());
    }

    @Override
    public String generate(UploadContext context) {
        return StorageUtils.generateFileName(context.getFile().getOriginalFilename());
    }
}
