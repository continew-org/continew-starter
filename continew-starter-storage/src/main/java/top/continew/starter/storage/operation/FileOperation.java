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
import top.continew.starter.storage.domain.model.resp.FileInfo;

import java.io.InputStream;

/**
 * 单文件链式操作
 *
 * @author echo
 * @since 3.1.0
 */
public class FileOperation {

    private final FileStorageService storageService;
    private final String bucket;
    private final String path;
    private String platform;

    public FileOperation(FileStorageService storageService, String bucket, String path) {
        this.storageService = storageService;
        this.bucket = bucket;
        this.path = path;
    }

    /**
     * 指定存储平台
     *
     * @param platform 存储平台
     * @return {@link FileOperation }
     */
    public FileOperation platform(String platform) {
        this.platform = platform;
        return this;
    }

    /**
     * 下载文件
     *
     * @return 文件输入流
     */
    public InputStream download() {
        return storageService.download(resolvePlatform(), bucket, path);
    }

    /**
     * 删除文件
     */
    public void delete() {
        storageService.delete(resolvePlatform(), bucket, path);
    }

    /**
     * 查询文件信息
     *
     * @return {@link FileInfo }
     */
    public FileInfo info() {
        return storageService.getFileInfo(resolvePlatform(), bucket, path);
    }

    /**
     * 判断文件是否存在
     *
     * @return 存在返回 true
     */
    public boolean exists() {
        return storageService.exists(resolvePlatform(), bucket, path);
    }

    /**
     * 复制到同桶目标路径
     *
     * @param targetPath 目标路径
     */
    public void copyTo(String targetPath) {
        copyTo(bucket, targetPath);
    }

    /**
     * 复制到指定桶和目标路径
     *
     * @param targetBucket 目标桶
     * @param targetPath   目标路径
     */
    public void copyTo(String targetBucket, String targetPath) {
        storageService.copy(resolvePlatform(), bucket, targetBucket, path, targetPath);
    }

    /**
     * 移动到同桶目标路径
     *
     * @param targetPath 目标路径
     */
    public void moveTo(String targetPath) {
        moveTo(bucket, targetPath);
    }

    /**
     * 移动到指定桶和目标路径
     *
     * @param targetBucket 目标桶
     * @param targetPath   目标路径
     */
    public void moveTo(String targetBucket, String targetPath) {
        storageService.move(resolvePlatform(), bucket, targetBucket, path, targetPath);
    }

    /**
     * 生成下载预签名 URL
     *
     * @param expireSeconds 过期秒数
     * @return 预签名 URL
     */
    public String presignedUrl(long expireSeconds) {
        return storageService.generatePresignedUrl(resolvePlatform(), bucket, path, expireSeconds);
    }

    /**
     * 生成上传预签名 URL
     *
     * @param expireSeconds 过期秒数
     * @return 预签名 URL
     */
    public String uploadPresignedUrl(long expireSeconds) {
        return storageService.generateUploadPresignedUrl(resolvePlatform(), bucket, path,
            expireSeconds);
    }

    /**
     * 创建范围下载操作
     *
     * @param start 起始字节
     * @param end   结束字节
     * @return {@link FileRangeOperation }
     */
    public FileRangeOperation range(long start, long end) {
        return new FileRangeOperation(storageService, platform, bucket, path, start, end);
    }

    /**
     * 将当前文件按 ZIP 文件处理
     *
     * @return {@link ZipFileOperation }
     */
    public ZipFileOperation asZip() {
        return new ZipFileOperation(storageService, platform, bucket, path);
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
