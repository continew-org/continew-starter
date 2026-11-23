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

package top.continew.starter.storage.strategy;

import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.common.util.ZipFileUtils;
import top.continew.starter.storage.domain.file.ZipStreamInputStream;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储策略
 *
 * @author echo
 * @since 2.14.0
 */
public interface StorageStrategy {

    /**
     * 上传
     *
     * @param bucket 存储桶
     * @param path   路径
     * @param file   文件
     */
    void upload(String bucket, String path, MultipartFile file);

    /**
     * 流式上传
     *
     * <p>
     * 当文件源为不可重复读取的输入流时调用，策略不得对数据做全量内存缓冲。
     * 入参 {@code contentLength} 为 -1 表示内容长度未知，实现应自行探测或按分片协议直传。
     * </p>
     *
     * @param bucket        存储桶
     * @param path          路径
     * @param inputStream   文件输入流（单次可读，由调用方关闭）
     * @param contentType   内容类型，允许为 null
     * @param contentLength 内容长度，-1 表示未知
     */
    default void uploadStream(String bucket, String path, InputStream inputStream,
        String contentType, long contentLength) {
        throw new StorageException("存储策略 " + getPlatform() + " 不支持流式上传");
    }

    /**
     * 是否支持流式上传（不可重复读取的单次流）
     *
     * <p>
     * 不支持时服务层会将流物化为临时文件后回退到 {@link #upload(String, String, MultipartFile)}，
     * 第三方策略可不实现流式上传保持兼容。
     * </p>
     *
     * @return 是否支持
     */
    default boolean supportUploadStream() {
        return false;
    }

    /**
     * 下载
     *
     * @param bucket 存储桶
     * @param path   路径
     * @return {@link InputStream }
     */
    InputStream download(String bucket, String path);

    /**
     * 字节范围下载
     *
     * <p>
     * 返回闭区间 [{@code start}, {@code end}] 的内容流，调用方关闭返回流。
     * 服务层只校验参数，文件大小解析、末端钳制与不存在/越界处理由策略自行决定。
     * </p>
     *
     * @param bucket 存储桶
     * @param path   路径
     * @param start  起始字节偏移（含，从 0 计）
     * @param end    结束字节偏移（含）
     * @return 范围内容输入流
     */
    default InputStream downloadRange(String bucket, String path, long start, long end) {
        throw new StorageException("存储策略 " + getPlatform() + " 不支持范围下载");
    }

    /**
     * 批量下载为 ZIP 流
     *
     * <p>
     * 默认实现基于 {@link #download(String, String)} 流式打包：消费方读取时才逐个拉取
     * 源文件写入 ZIP，条目名取对象路径，重名自动追加 " (n)" 后缀。策略可覆盖定制
     * （如批量获取、并发拉取）。ZIP 中央目录随条目数占用内存。返回流由调用方关闭。
     * </p>
     *
     * @param bucket 存储桶
     * @param paths  路径列表
     * @return ZIP 格式输入流
     */
    default InputStream batchDownload(String bucket, List<String> paths) {
        List<String> pathList = paths == null ? List.of() : paths;
        Map<String, Integer> nameCount = new HashMap<>();
        return new ZipStreamInputStream(pathList, path -> download(bucket, path),
            path -> ZipFileUtils.dedupEntryName(ZipFileUtils.normalizeEntryName(path),
                nameCount));
    }

    /**
     * 删除
     *
     * @param bucket 存储桶
     * @param path   路径
     */
    void delete(String bucket, String path);

    /**
     * 批量删除
     *
     * @param bucket 存储桶
     * @param paths  路径列表
     */
    void batchDelete(String bucket, List<String> paths);

    /**
     * 是否存在
     *
     * @param bucket 存储桶
     * @param path   路径
     * @return 是否存在
     */
    boolean exists(String bucket, String path);

    /**
     * 获取文件信息
     *
     * @param bucket 存储桶
     * @param path   路径
     * @return 文件信息
     */
    FileInfo getFileInfo(String bucket, String path);

    /**
     * 列出文件
     *
     * @param bucket  存储桶
     * @param prefix  路径前缀
     * @param maxKeys 最大返回条数
     * @return 文件信息列表
     */
    List<FileInfo> list(String bucket, String prefix, int maxKeys);

    /**
     * 复制文件
     *
     * @param sourceBucket 源存储桶
     * @param targetBucket 目标存储桶
     * @param sourcePath   源路径
     * @param targetPath   目标路径
     */
    void copy(String sourceBucket, String targetBucket, String sourcePath, String targetPath);

    /**
     * 移动文件
     *
     * @param sourceBucket 源存储桶
     * @param targetBucket 目标存储桶
     * @param sourcePath   源路径
     * @param targetPath   目标路径
     */
    void move(String sourceBucket, String targetBucket, String sourcePath, String targetPath);

    /**
     * 获取平台
     *
     * @return 平台编码
     */
    String getPlatform();

    /**
     * 默认桶
     *
     * @return {@link String }
     */
    String defaultBucket();

    /**
     * 生成预签名URL
     *
     * @param bucket        存储桶
     * @param path          路径
     * @param expireSeconds 过期时间（秒）
     * @return 预签名 URL
     */
    String generatePresignedUrl(String bucket, String path, long expireSeconds);

    /**
     * 生成上传预签名URL
     *
     * @param bucket        存储桶
     * @param path          路径
     * @param expireSeconds 过期时间（秒）
     * @return 上传预签名 URL
     */
    String generateUploadPresignedUrl(String bucket, String path, long expireSeconds);

    /**
     * 初始化分片上传
     *
     * @param bucket      存储桶
     * @param path        文件路径
     * @param contentType 内容类型
     * @param metadata    元数据
     * @return 初始化结果
     */
    MultipartInitResp initMultipartUpload(String bucket, String path, String contentType,
        Map<String, String> metadata);

    /**
     * 上传分片
     *
     * @param bucket     存储桶
     * @param path       文件路径
     * @param uploadId   上传ID
     * @param partNumber 分片编号
     * @param data       分片数据
     * @return 上传结果
     */
    MultipartUploadResp uploadPart(String bucket, String path, String uploadId, int partNumber,
        InputStream data);

    /**
     * 完成分片上传
     *
     * @param bucket      存储桶
     * @param path        文件路径
     * @param uploadId    上传ID
     * @param parts       已上传的分片信息（从 FileRecorder 获取）
     * @param verifyParts 是否验证分片（对象存储需要验证）
     * @return 文件信息
     */
    FileInfo completeMultipartUpload(String bucket,
        String path,
        String uploadId,
        List<MultipartUploadResp> parts,
        boolean verifyParts);

    // 保留原方法作为默认实现
    default FileInfo completeMultipartUpload(String bucket,
        String path,
        String uploadId,
        List<MultipartUploadResp> parts) {
        return completeMultipartUpload(bucket, path, uploadId, parts, true);
    }

    /**
     * 取消分片上传
     *
     * @param bucket   存储桶
     * @param path     文件路径
     * @param uploadId 上传ID
     */
    void abortMultipartUpload(String bucket, String path, String uploadId);

    /**
     * 列出已上传的分片
     *
     * @param bucket   存储桶
     * @param path     文件路径
     * @param uploadId 上传ID
     * @return 分片列表
     */
    List<MultipartUploadResp> listParts(String bucket, String path, String uploadId);

    /**
     * 清理 - 针对本地静态资源卸载
     */
    default void cleanup() {
        // 默认不做任何清理
    }
}
