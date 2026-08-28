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

package top.continew.starter.storage.service;

import top.continew.starter.storage.domain.model.context.UploadContext;

/**
 * 文件处理器接口
 *
 * @author echo
 * @since 2.14.0
 */
public interface FileProcessor {

    /**
     * 获取处理器名称
     *
     * @return 处理器名称
     */
    String getName();

    /**
     * 获取处理器优先级（数值越大优先级越高）
     *
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }

    /**
     * 是否支持该文件
     *
     * @param context 上传上下文
     * @return 是否支持
     */
    boolean support(UploadContext context);
}
