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

package top.continew.starter.storage.operation;

import cn.hutool.core.util.StrUtil;
import top.continew.starter.storage.core.FileStorageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多文件链式操作
 *
 * @author echo
 * @since 3.1.0
 */
public class FilesOperation {

    private final FileStorageService storageService;
    private final String bucket;
    private final List<String> paths;
    private String platform;

    public FilesOperation(FileStorageService storageService, String bucket, List<String> paths) {
        this.storageService = storageService;
        this.bucket = bucket;
        // 快照防止外部可变列表在终结调用前被改动
        this.paths = paths == null ? Collections.emptyList() : new ArrayList<>(paths);
    }

    /**
     * 指定存储平台
     *
     * @param platform 存储平台
     * @return {@link FilesOperation }
     */
    public FilesOperation platform(String platform) {
        this.platform = platform;
        return this;
    }

    /**
     * 批量删除文件
     */
    public void delete() {
        storageService.batchDelete(resolvePlatform(), bucket, paths);
    }

    /**
     * 创建 ZIP 批量下载操作
     *
     * @return {@link ZipFilesOperation }
     */
    public ZipFilesOperation zip() {
        return new ZipFilesOperation(storageService, platform, bucket, paths);
    }

    /**
     * 解析本次操作使用的存储平台
     *
     * @return 显式平台为空时返回默认存储平台
     */
    private String resolvePlatform() {
        return StrUtil.isBlank(platform) ? storageService.getDefaultPlatform() : platform;
    }
}
