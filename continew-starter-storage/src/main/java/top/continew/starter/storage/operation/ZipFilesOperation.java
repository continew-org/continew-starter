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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多文件 ZIP 下载链式操作
 *
 * @author echo
 * @since 3.1.0
 */
public class ZipFilesOperation {

    private final FileStorageService storageService;
    private final String platform;
    private final String bucket;
    private final List<String> paths;

    ZipFilesOperation(FileStorageService storageService, String platform, String bucket,
        List<String> paths) {
        this.storageService = storageService;
        this.platform = platform;
        this.bucket = bucket;
        this.paths = paths == null ? Collections.emptyList() : new ArrayList<>(paths);
    }

    /**
     * 下载 ZIP 输入流
     *
     * @return ZIP 输入流
     */
    public InputStream download() {
        return storageService.batchDownload(resolvePlatform(), bucket, paths);
    }

    /**
     * 写出 ZIP 到输出流（输出流由调用方关闭）
     *
     * @param outputStream 输出流
     */
    public void writeTo(OutputStream outputStream) {
        storageService.writeBatchDownload(resolvePlatform(), bucket, paths, outputStream);
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
