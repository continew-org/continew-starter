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

package top.continew.starter.storage.processor.preprocess;

import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.domain.model.req.ThumbnailInfo;
import top.continew.starter.storage.service.FileProcessor;

import java.io.InputStream;

/**
 * 缩略图处理器
 *
 * @author echo
 * @since 2.14.0
 */
public interface ThumbnailProcessor extends FileProcessor {

    /**
     * 生成缩略图
     *
     * @param context           上传上下文
     * @param sourceInputStream 原始文件流
     * @return 缩略图信息
     */
    ThumbnailInfo process(UploadContext context, InputStream sourceInputStream);
}
