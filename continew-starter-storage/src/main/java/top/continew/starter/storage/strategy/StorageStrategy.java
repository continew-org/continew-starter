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
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;

import java.io.InputStream;
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
     * 下载
     *
     * @param bucket 存储桶
     * @param path   路径
     * @return {@link InputStream }
     */
    InputStream download(String bucket, String path);

    /**
     * 批量下载到zip
     */
    InputStream batchDownload(String bucket, List<String> paths);

    /**
     * 删除
     */
    void delete(String bucket, String path);

    /**
     * 批量删除
     */
    void batchDelete(String bucket, List<String> paths);

    /**
     * 是否存在
     */
    boolean exists(String bucket, String path);

    /**
     * 获取文件信息
     */
    FileInfo getFileInfo(String bucket, String path);

    /**
     * 列出文件
     */
    List<FileInfo> list(String bucket, String prefix, int maxKeys);

    /**
     * 复制文件
     */
    void copy(String sourceBucket, String targetBucket, String sourcePath, String targetPath);

    /**
     * 移动文件
     */
    void move(String sourceBucket, String targetBucket, String sourcePath, String targetPath);

    /**
     * 获取平台
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
     */
    String generatePresignedUrl(String bucket, String path, long expireSeconds);

    /**
     * 生成上传预签名URL
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
     * @param uploadId 上传ID
     */
    void abortMultipartUpload(String bucket, String path, String uploadId);

    /**
     * 列出已上传的分片
     *
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
