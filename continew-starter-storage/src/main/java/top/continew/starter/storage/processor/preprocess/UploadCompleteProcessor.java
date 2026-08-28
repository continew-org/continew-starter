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

import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.service.FileProcessor;

/**
 * 上传完成处理器
 *
 * @author echo
 * @since 2.14.0
 */
public interface UploadCompleteProcessor extends FileProcessor {

    /**
     * 处理上传完成事件
     *
     * @param fileInfo 文件信息
     */
    void onComplete(FileInfo fileInfo);
}
