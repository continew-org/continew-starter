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
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.common.util.ZipFileUtils;
import top.continew.starter.storage.core.FileStorageService;
import top.continew.starter.storage.domain.model.resp.ZipImportResp;

import java.util.Locale;

/**
 * 已有 ZIP 文件链式解包导入操作
 *
 * @author echo
 * @since 3.1.0
 */
public class ZipFileOperation {

    private final FileStorageService storageService;
    private final String platform;
    private final String sourceBucket;
    private final String sourcePath;
    private final String defaultTargetPath;
    private String targetBucket;
    private String targetPath;

    ZipFileOperation(FileStorageService storageService, String platform, String sourceBucket,
        String sourcePath) {
        this.storageService = storageService;
        this.platform = platform;
        this.sourceBucket = sourceBucket;
        this.sourcePath = sourcePath;
        this.defaultTargetPath = resolveDefaultTargetPath(sourcePath);
    }

    /**
     * 指定同桶解压目标路径
     *
     * @param targetPath 目标路径，空值表示解压到同级同名目录
     * @return {@link ZipFileOperation }
     */
    public ZipFileOperation importTo(String targetPath) {
        this.targetPath = targetPath;
        return this;
    }

    /**
     * 指定跨桶解压目标桶和目标路径
     *
     * @param targetBucket 目标桶
     * @param targetPath   目标路径，空值表示解压到同级同名目录
     * @return {@link ZipFileOperation }
     */
    public ZipFileOperation importTo(String targetBucket, String targetPath) {
        this.targetBucket = targetBucket;
        this.targetPath = targetPath;
        return this;
    }

    /**
     * 指定跨桶解压目标桶，目标路径沿用默认同级同名目录
     *
     * @param targetBucket 目标桶
     * @return {@link ZipFileOperation }
     */
    public ZipFileOperation importToBucket(String targetBucket) {
        this.targetBucket = targetBucket;
        return this;
    }

    /**
     * 执行 ZIP 解包导入：在同一平台先下载源 ZIP 再逐条导入目标，下载流由
     * {@link FileStorageService#importZip} 负责关闭（含失败路径）
     *
     * @return {@link ZipImportResp }
     */
    public ZipImportResp importZip() {
        String actualPlatform = resolvePlatform();
        return storageService.importZip(actualPlatform, resolveTargetBucket(), resolveTargetPath(),
            storageService.download(actualPlatform, sourceBucket, sourcePath));
    }

    /**
     * 校验源文件是否为 ZIP 文件并解析默认解压目标（同级同名目录，
     * {@code dir/a.zip} 得 {@code dir/a}）。源对象键原样保留不改写：
     * 仅对用于校验与派生的路径先剥单个前导 {@code /}（对象键允许该形态），
     * 再经 {@link ZipFileUtils#normalizeEntryName} 做目录穿越校验；
     * 后缀按大小写不敏感判定，文件名片段须非空。
     *
     * @param path 源文件路径
     * @return 同级同名目录父路径
     */
    private String resolveDefaultTargetPath(String path) {
        // null/穿越/绝对路径（剥单前导斜杠后仍带 /）由 normalizeEntryName 拒绝
        String normalizedPath = ZipFileUtils
            .normalizeEntryName(StrUtil.removePrefix(path, StringConstants.SLASH));
        int lastSlashIndex = normalizedPath.lastIndexOf(StringConstants.SLASH);
        String fileName = normalizedPath.substring(lastSlashIndex + 1);
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(StorageConstant.ZIP_SUFFIX)) {
            throw new StorageException("当前文件不是 ZIP 文件: " + path);
        }
        String folderName = fileName.substring(0,
            fileName.length() - StorageConstant.ZIP_SUFFIX.length());
        if (StrUtil.isBlank(folderName)) {
            throw new StorageException("ZIP 文件名不能为空: " + path);
        }
        return (lastSlashIndex < 0 ? StringConstants.EMPTY
            : normalizedPath.substring(0, lastSlashIndex + 1)) + folderName;
    }

    /**
     * 解析本次操作使用的存储平台
     *
     * @return 显式平台为空时返回默认存储平台
     */
    private String resolvePlatform() {
        return StrUtil.isBlank(platform) ? storageService.getDefaultPlatform() : platform;
    }

    /**
     * 解析解压目标桶
     *
     * @return 目标桶为空时返回源桶
     */
    private String resolveTargetBucket() {
        return StrUtil.blankToDefault(targetBucket, sourceBucket);
    }

    /**
     * 解析解压目标路径：显式目标交给 importZip 的父路径规范化校验
     *
     * @return 目标路径为空时返回默认同级同名目录
     */
    private String resolveTargetPath() {
        return StrUtil.blankToDefault(targetPath, defaultTargetPath);
    }
}
