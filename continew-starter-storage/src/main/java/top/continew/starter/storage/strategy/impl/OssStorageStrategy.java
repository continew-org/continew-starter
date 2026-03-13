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

package top.continew.starter.storage.strategy.impl;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.storage.autoconfigure.properties.OssStorageConfig;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.strategy.StorageStrategy;
import top.continew.starter.storage.common.util.StorageUtils;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * oss存储策略
 *
 * @author echo
 * @since 2.14.0
 */
public class OssStorageStrategy implements StorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(OssStorageStrategy.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OssStorageConfig config;
    private final long multipartUploadPartSize;

    public OssStorageStrategy(OssStorageConfig config) {
        this.config = config;
        this.multipartUploadPartSize = resolveMultipartUploadPartSize(config);
        this.s3Client = createS3Client(config);
        this.s3Presigner = createS3Presigner(config);
    }

    /**
     * 获取客户端
     *
     * @return {@link S3Client }
     */
    public S3Client getClient() {
        return s3Client;
    }

    /**
     * 获取预签名者
     *
     * @return {@link S3Presigner }
     */
    public S3Presigner getPresigner() {
        return s3Presigner;
    }

    /**
     * 创建S3客户端
     *
     * @param config 配置
     * @return {@link S3Client }
     */
    private S3Client createS3Client(OssStorageConfig config) {
        // 登录认证账户密码
        StaticCredentialsProvider auth = StaticCredentialsProvider.create(AwsBasicCredentials.create(config
            .getAccessKey(), config.getSecretKey()));

        return S3Client.builder()
            .credentialsProvider(auth)
            .endpointOverride(URI.create(config.getEndpoint()))
            .region(StorageUtils.getRegion(config.getRegion()))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    /**
     * 创建S3预签名器
     *
     * @param config 配置
     * @return {@link S3Presigner }
     */
    private S3Presigner createS3Presigner(OssStorageConfig config) {
        StaticCredentialsProvider auth = StaticCredentialsProvider.create(AwsBasicCredentials.create(config
            .getAccessKey(), config.getSecretKey()));

        String domain = StrUtil.isNotBlank(config.getDomain()) ? config.getDomain() : config.getEndpoint();

        return S3Presigner.builder()
            .credentialsProvider(auth)
            .endpointOverride(URI.create(domain))
            .region(StorageUtils.getRegion(config.getRegion()))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(config.isPathStyleAccessEnabled())
                .build())
            .build();
    }

    @Override
    public void upload(String bucket, String path, MultipartFile file) {
        String key = normalizeKey(path);
        // 构建上传请求
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(file.getContentType())
            .contentLength(file.getSize());

        try {
            // 执行上传
            s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(file.getInputStream(), file
                .getSize()));

        } catch (Exception e) {
            throw new StorageException("S3上传异常" + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     */
    @Override
    public InputStream download(String bucket, String path) {
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(normalizeKey(path)).build());
        } catch (Exception e) {
            throw new StorageException("S3下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream batchDownload(String bucket, List<String> paths) {
        return null;
    }

    /**
     * 删除文件
     */
    @Override
    public void delete(String bucket, String path) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(normalizeKey(path)).build());
        } catch (Exception e) {
            throw new StorageException("S3删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量删除（优化版）
     */
    @Override
    public void batchDelete(String bucket, List<String> paths) {
        if (paths.isEmpty()) {
            return;
        }

        try {
            // S3支持批量删除，最多1000个
            List<List<String>> batches = partition(paths, 1000);

            for (List<String> batch : batches) {
                List<ObjectIdentifier> objects = batch.stream()
                    .map(path -> ObjectIdentifier.builder().key(path).build())
                    .collect(Collectors.toList());

                DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(objects).build())
                    .build();

                s3Client.deleteObjects(deleteRequest);
            }
        } catch (Exception e) {
            throw new StorageException("S3批量删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查文件是否存在
     */
    @Override
    public boolean exists(String bucket, String path) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(normalizeKey(path)).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new StorageException("S3检查文件存在性失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件信息
     */
    @Override
    public FileInfo getFileInfo(String bucket, String path) {
        try {
            String key = normalizeKey(path);
            String bucketName = bucket;
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());

            FileInfo fileInfo = new FileInfo();
            fileInfo.setBucket(bucketName);
            fileInfo.setPlatform(config.getPlatform());
            fileInfo.setPath(key);
            fileInfo.setFullPath(key);
            fileInfo.setName(getFileName(key));
            fileInfo.setSize(response.contentLength());
            fileInfo.setContentType(response.contentType());
            fileInfo.setUrl(getFileUrl(key));
            fileInfo.setUploadTime(LocalDateTime.ofInstant(response.lastModified(), java.time.ZoneId.systemDefault()));

            // 添加元数据
            Map<String, String> metadata = new HashMap<>(response.metadata());
            metadata.put("etag", response.eTag());
            if (response.versionId() != null) {
                metadata.put("versionId", response.versionId());
            }
            fileInfo.setMetadata(metadata);

            return fileInfo;

        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            throw new StorageException("S3获取文件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出文件
     */
    @Override
    public List<FileInfo> list(String bucket, String prefix, int maxKeys) {
        try {
            String bucketName = bucket;
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix == null ? null : normalizeKey(prefix))
                .maxKeys(maxKeys)
                .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            return response.contents().stream().map(s3Object -> {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setBucket(bucketName);
                fileInfo.setPlatform(config.getPlatform());
                fileInfo.setPath(s3Object.key());
                fileInfo.setFullPath(s3Object.key());
                fileInfo.setName(getFileName(s3Object.key()));
                fileInfo.setSize(s3Object.size());
                fileInfo.setUrl(getFileUrl(s3Object.key()));
                fileInfo.setUploadTime(LocalDateTime.ofInstant(s3Object.lastModified(), java.time.ZoneId
                    .systemDefault()));

                Map<String, String> metadata = new HashMap<>();
                metadata.put("etag", s3Object.eTag());
                metadata.put("storageClass", s3Object.storageClassAsString());
                fileInfo.setMetadata(metadata);

                return fileInfo;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            throw new StorageException("S3列出文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 复制文件
     */
    @Override
    public void copy(String sourceBucket, String targetBucket, String sourcePath, String targetPath) {
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(normalizeKey(sourcePath))
                .destinationBucket(targetBucket)
                .destinationKey(normalizeKey(targetPath))
                .build();

            s3Client.copyObject(copyRequest);

        } catch (Exception e) {
            throw new StorageException("S3复制文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void move(String sourceBucket, String targetBucket, String sourcePath, String targetPath) {
        copy(sourceBucket, targetBucket, sourcePath, targetPath);
        delete(sourceBucket, sourcePath);
    }

    @Override
    public String getPlatform() {
        return config.getPlatform();
    }

    @Override
    public String defaultBucket() {
        return config.getBucketName();
    }

    /**
     * 生成预签名URL
     */
    @Override
    public String generatePresignedUrl(String bucket, String path, long expireSeconds) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(normalizeKey(path))
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expireSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();

        } catch (Exception e) {
            throw new StorageException("S3生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件URL
     */
    private String getFileUrl(String path) {
        if (config.getEndpoint() != null && !config.getEndpoint().isEmpty()) {
            return config.getEndpoint() + StringConstants.SLASH + path;
        } else {
            return String.format("%s/%s/%s", config.getEndpoint(), config.getBucketName(), path);
        }
    }

    /**
     * 获取文件名
     */
    private String getFileName(String path) {
        int lastSlashIndex = path.lastIndexOf(StringConstants.SLASH);
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
    }

    /**
     * 分割列表
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    /**
     * 初始化分片上传
     */
    @Override
    public MultipartInitResp initMultipartUpload(String bucket,
                                                 String path,
                                                 String contentType,
                                                 Map<String, String> metadata) {
        try {
            String key = normalizeKey(path);
            // 构建请求
            CreateMultipartUploadRequest.Builder requestBuilder = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType);

            // 添加元数据
            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.metadata(metadata);
            }

            // 设置ACL
            if (config.getDefaultAcl() != null) {
                requestBuilder.acl(config.getDefaultAcl());
            }

            // 执行请求
            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(requestBuilder.build());

            // 构建返回结果
            MultipartInitResp result = new MultipartInitResp();
            result.setBucket(bucket);
            result.setFileId(UUID.randomUUID().toString());
            result.setUploadId(response.uploadId());
            result.setPlatform(config.getPlatform());
            result.setPath(key);
            result.setPartSize(multipartUploadPartSize);
            return result;

        } catch (Exception e) {
            throw new StorageException("S3初始化分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传分片
     */
    @Override
    public MultipartUploadResp uploadPart(String bucket,
                                          String path,
                                          String uploadId,
                                          int partNumber,
                                          InputStream data) {
        try {
            String key = normalizeKey(path);

            if (StrUtil.isBlank(key)) {
                throw new StorageException("无效的uploadId: " + uploadId);
            }

            // 读取数据到内存（注意：实际使用时可能需要优化大文件处理）
            byte[] bytes = data.readAllBytes();

            // 构建请求
            UploadPartRequest request = UploadPartRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength((long)bytes.length)
                .build();

            // 执行上传
            UploadPartResponse response = s3Client.uploadPart(request, RequestBody.fromBytes(bytes));

            // 构建返回结果
            MultipartUploadResp result = new MultipartUploadResp();
            result.setPartNumber(partNumber);
            result.setPartETag(response.eTag());
            result.setSuccess(true);
            return result;

        } catch (Exception e) {
            MultipartUploadResp result = new MultipartUploadResp();
            result.setPartNumber(partNumber);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * 完成分片上传
     */
    @Override
    public FileInfo completeMultipartUpload(String bucket,
                                            String path,
                                            String uploadId,
                                            List<MultipartUploadResp> parts,
                                            boolean verifyParts) {
        try {
            String key = normalizeKey(path);
            if (StrUtil.isBlank(key)) {
                throw new StorageException("无效的uploadId: " + uploadId);
            }

            // 如果需要验证，比较本地记录和S3的分片信息
            if (verifyParts) {
                List<MultipartUploadResp> s3Parts = listParts(bucket, key, uploadId);
                validateParts(parts, s3Parts);
            }

            // 构建已完成的分片列表
            List<CompletedPart> completedParts = parts.stream()
                .filter(MultipartUploadResp::isSuccess)
                .map(part -> CompletedPart.builder().partNumber(part.getPartNumber()).eTag(part.getPartETag()).build())
                .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                .collect(Collectors.toList());

            // 构建请求
            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build();

            // 完成上传
            s3Client.completeMultipartUpload(request);

            // 获取文件信息
            return getFileInfo(bucket, key);

        } catch (Exception e) {
            throw new StorageException("S3完成分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消分片上传
     */
    @Override
    public void abortMultipartUpload(String bucket, String path, String uploadId) {
        try {
            String key = normalizeKey(path);
            if (StrUtil.isBlank(key)) {
                log.warn("无效的uploadId，可能已经完成或取消: {}", uploadId);
                return;
            }

            // 构建请求
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build();

            // 执行取消
            s3Client.abortMultipartUpload(request);
        } catch (Exception e) {
            log.error("取消分片上传失败: uploadId={}", uploadId, e);
            throw new StorageException("S3取消分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出已上传的分片
     */
    @Override
    public List<MultipartUploadResp> listParts(String bucket, String path, String uploadId) {
        try {
            String key = normalizeKey(path);
            if (StrUtil.isBlank(key)) {
                throw new StorageException("无效的uploadId: " + uploadId);
            }

            // 构建请求
            ListPartsRequest request = ListPartsRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build();

            // 获取分片列表
            ListPartsResponse response = s3Client.listParts(request);

            // 转换结果
            return response.parts().stream().map(part -> {
                MultipartUploadResp result = new MultipartUploadResp();
                result.setPartNumber(part.partNumber());
                result.setPartETag(part.eTag());
                result.setSuccess(true);
                return result;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            throw new StorageException("S3列出分片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证分片一致性
     *
     * @param recordParts 记录部件
     * @param s3Parts     s3零件
     */
    private void validateParts(List<MultipartUploadResp> recordParts, List<MultipartUploadResp> s3Parts) {
        Map<Integer, String> recordMap = recordParts.stream()
            .collect(Collectors.toMap(MultipartUploadResp::getPartNumber, MultipartUploadResp::getPartETag));

        Map<Integer, String> s3Map = s3Parts.stream()
            .collect(Collectors.toMap(MultipartUploadResp::getPartNumber, MultipartUploadResp::getPartETag));

        // 检查分片数量
        if (recordMap.size() != s3Map.size()) {
            throw new StorageException(String.format("分片数量不一致: 本地记录=%d, S3=%d", recordMap.size(), s3Map.size()));
        }

        // 检查每个分片
        List<Integer> missingParts = new ArrayList<>();
        List<Integer> mismatchParts = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : recordMap.entrySet()) {
            Integer partNumber = entry.getKey();
            String recordETag = entry.getValue();
            String s3ETag = s3Map.get(partNumber);

            if (s3ETag == null) {
                missingParts.add(partNumber);
            } else if (!recordETag.equals(s3ETag)) {
                mismatchParts.add(partNumber);
            }
        }

        if (!missingParts.isEmpty()) {
            throw new StorageException("S3缺失分片: " + missingParts);
        }

        if (!mismatchParts.isEmpty()) {
            throw new StorageException("分片ETag不匹配: " + mismatchParts);
        }
    }

    private String normalizeKey(String rawKey) {
        if (StrUtil.isBlank(rawKey)) {
            return StringConstants.EMPTY;
        }
        String key = rawKey.trim().replace("\\", StringConstants.SLASH).replaceAll("/+", StringConstants.SLASH);
        while (key.startsWith(StringConstants.SLASH)) {
            key = key.substring(1);
        }
        return key;
    }

    private long resolveMultipartUploadPartSize(OssStorageConfig ossStorageConfig) {
        Long partSize = ossStorageConfig.getMultipartUploadPartSize();
        if (partSize == null || partSize <= 0) {
            return StorageConstant.DEFAULT_MULTIPART_UPLOAD_PART_SIZE;
        }
        return partSize;
    }
}
