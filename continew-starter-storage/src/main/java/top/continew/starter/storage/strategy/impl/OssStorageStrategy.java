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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.storage.autoconfigure.properties.OssStorageConfig;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.common.util.StorageUtils;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * oss存储策略
 *
 * @author echo
 * @since 2.14.0
 */
public class OssStorageStrategy implements StorageStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(OssStorageStrategy.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OssStorageConfig config;
    private final long multipartUploadThreshold;
    private final long multipartUploadPartSize;
    private final String multipartTempDir;
    private final long uploadPartInMemoryThreshold;

    public OssStorageStrategy(OssStorageConfig config) {
        this(config, null);
    }

    /**
     * 测试专用构造器：注入预置 S3 客户端（为 null 时按配置创建）
     *
     * @param config   配置
     * @param s3Client 预置客户端
     */
    OssStorageStrategy(OssStorageConfig config, S3Client s3Client) {
        this.config = config;
        this.multipartUploadThreshold = resolveMultipartUploadThreshold(config);
        this.multipartUploadPartSize = resolveMultipartUploadPartSize(config);
        this.multipartTempDir = resolveMultipartTempDir(config);
        this.uploadPartInMemoryThreshold = resolveUploadPartInMemoryThreshold();
        this.s3Client = s3Client == null ? createS3Client(config) : s3Client;
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
        StaticCredentialsProvider auth =
            StaticCredentialsProvider.create(AwsBasicCredentials.create(config
                .getAccessKey(), config.getSecretKey()));

        return S3Client.builder()
            .credentialsProvider(auth)
            .endpointOverride(URI.create(config.getEndpoint()))
            .region(StorageUtils.getRegion(config.getRegion()))
            .serviceConfiguration(buildS3Configuration(config))
            .overrideConfiguration(buildTimeoutConfig(config))
            .build();
    }

    /**
     * 创建S3预签名器
     *
     * @param config 配置
     * @return {@link S3Presigner }
     */
    private S3Presigner createS3Presigner(OssStorageConfig config) {
        StaticCredentialsProvider auth =
            StaticCredentialsProvider.create(AwsBasicCredentials.create(config
                .getAccessKey(), config.getSecretKey()));

        String domain =
            StrUtil.isNotBlank(config.getDomain()) ? config.getDomain() : config.getEndpoint();

        return S3Presigner.builder()
            .credentialsProvider(auth)
            .endpointOverride(URI.create(domain))
            .region(StorageUtils.getRegion(config.getRegion()))
            .serviceConfiguration(buildS3Configuration(config))
            .build();
    }

    @Override
    public void upload(String bucket, String path, MultipartFile file) {
        // 长度未知的单次流直接走流式上传，避免 putObject 因缺失 contentLength 失败
        if (file.getSize() < 0) {
            try (InputStream in = file.getInputStream()) {
                uploadStream(bucket, path, in, file.getContentType(), file.getSize());
                return;
            } catch (IOException e) {
                throw new StorageException("S3上传异常" + e.getMessage(), e);
            }
        }
        String key = normalizeKey(path);
        // 构建上传请求
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(file.getContentType())
            .contentLength(file.getSize());

        try {
            // 执行上传
            s3Client.putObject(requestBuilder.build(),
                RequestBody.fromInputStream(file.getInputStream(), file
                    .getSize()));

        } catch (Exception e) {
            throw new StorageException("S3上传异常" + e.getMessage(), e);
        }
    }

    @Override
    public boolean supportUploadStream() {
        return true;
    }

    @Override
    public void uploadStream(String bucket, String path, InputStream inputStream,
        String contentType, long contentLength) {
        String key = normalizeKey(path);
        try {
            if (contentLength >= 0) {
                // 长度已知：单次 PUT，S3 PUT 上限 5GB 覆盖常规文件
                PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength);
                s3Client.putObject(requestBuilder.build(),
                    RequestBody.fromInputStream(inputStream, contentLength));
                return;
            }

            // 长度未知：探测缓冲，未超过阈值则单次 PUT，否则转分片上传（上限 64MB 防溢出）
            byte[] probe = new byte[(int) Math.min(uploadPartInMemoryThreshold,
                64L * 1024 * 1024)];
            int probeSize = Math.max(readUpTo(inputStream, probe, probe.length), 0);
            if (probeSize < probe.length) {
                // 含空流（probeSize=0）：按实际长度单次 PUT
                PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) probeSize);
                s3Client.putObject(requestBuilder.build(),
                    RequestBody.fromInputStream(new ByteArrayInputStream(probe, 0, probeSize),
                        probeSize));
                return;
            }
            uploadStreamMultipart(bucket, key, inputStream, contentType, probe, probeSize);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("S3流式上传异常: " + e.getMessage(), e);
        }
    }

    /**
     * 未知长度流的分片上传：以探测数据为首块，按分片大小逐块直传，失败时中止分片任务
     *
     * @param bucket      存储桶
     * @param key         规范化对象键
     * @param inputStream 剩余内容输入流
     * @param contentType 内容类型
     * @param head        已探测的首块数据
     * @param headSize    首块数据实际长度
     */
    private void uploadStreamMultipart(String bucket, String key, InputStream inputStream,
        String contentType, byte[] head, int headSize) {
        // S3 要求除最后一片外每片至少 5MB，缓冲尺寸取下限 5MB
        int partBufferSize = (int) Math.min(Math.max(multipartUploadPartSize, 5L * 1024 * 1024),
            64L * 1024 * 1024);
        String uploadId = null;
        try {
            CreateMultipartUploadResponse initResponse = s3Client
                .createMultipartUpload(CreateMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build());
            uploadId = initResponse.uploadId();

            List<CompletedPart> completedParts = new ArrayList<>();
            byte[] buffer = new byte[partBufferSize];
            int bufferSize = 0;
            System.arraycopy(head, 0, buffer, 0, headSize);
            bufferSize += headSize;

            int partNumber = 1;
            boolean eof = false;
            while (!eof) {
                int n = readUpTo(inputStream, buffer, bufferSize, partBufferSize - bufferSize);
                if (n < 0) {
                    eof = true;
                } else {
                    bufferSize += n;
                }
                // 凑满一片或已到流末尾时上传该片（末尾允许不足 5MB）
                if (bufferSize == partBufferSize || (eof && bufferSize > 0)) {
                    UploadPartResponse partResponse =
                        s3Client.uploadPart(UploadPartRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .contentLength((long) bufferSize)
                            .build(),
                            RequestBody
                                .fromInputStream(new ByteArrayInputStream(buffer, 0, bufferSize),
                                    bufferSize));
                    completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(partResponse.eTag())
                        .build());
                    partNumber++;
                    bufferSize = 0;
                }
            }

            if (completedParts.isEmpty()) {
                throw new StorageException("S3流式上传异常: 无有效分片数据");
            }
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(multipartUpload -> multipartUpload.parts(completedParts))
                .build());
        } catch (Exception e) {
            if (uploadId != null) {
                try {
                    s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .build());
                } catch (Exception abortError) {
                    LOGGER.warn("中止分片上传失败: uploadId={}", uploadId, abortError);
                }
            }
            if (e instanceof StorageException storageException) {
                throw storageException;
            }
            throw new StorageException("S3流式上传异常: " + e.getMessage(), e);
        }
    }

    /**
     * 从流中尽量读取指定字节数，直到缓冲区填满或流结束
     *
     * @param in     输入流
     * @param buffer 目标缓冲区
     * @param len    期望读取的最大字节数（从 0 偏移写入）
     * @return 实际读取字节数，流已结束且未读到数据时返回 -1
     * @throws IOException 读取失败时抛出
     */
    private int readUpTo(InputStream in, byte[] buffer, int len) throws IOException {
        return readUpTo(in, buffer, 0, len);
    }

    /**
     * 从流中尽量读取指定字节数写入缓冲区指定偏移，直到填满或流结束
     *
     * @param in     输入流
     * @param buffer 目标缓冲区
     * @param off    缓冲区写入偏移
     * @param len    期望读取的最大字节数
     * @return 实际读取字节数，流已结束且未读到数据时返回 -1
     * @throws IOException 读取失败时抛出
     */
    private int readUpTo(InputStream in, byte[] buffer, int off, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int n = in.read(buffer, off + total, len - total);
            if (n < 0) {
                return total == 0 ? -1 : total;
            }
            total += n;
        }
        return total;
    }

    /**
     * 下载文件
     */
    @Override
    public InputStream download(String bucket, String path) {
        try {
            return s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(normalizeKey(path)).build());
        } catch (Exception e) {
            throw new StorageException("S3下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadRange(String bucket, String path, long start, long end) {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(normalizeKey(path))
                .range("bytes=" + start + "-" + end)
                .build());
        } catch (Exception e) {
            throw new StorageException("S3范围下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     */
    @Override
    public void delete(String bucket, String path) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(normalizeKey(path)).build());
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
                    .toList();

                DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(delete -> delete.objects(objects))
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
            s3Client.headObject(
                HeadObjectRequest.builder().bucket(bucket).key(normalizeKey(path)).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new StorageException("S3检查文件存在性失败: " + e.getMessage(), e);
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
            fileInfo.setUploadTime(
                LocalDateTime.ofInstant(response.lastModified(), java.time.ZoneId.systemDefault()));

            // 添加元数据
            Map<String, String> metadata = new HashMap<>(response.metadata());
            metadata.put("etag", response.eTag());
            if (response.versionId() != null) {
                metadata.put("versionId", response.versionId());
            }
            fileInfo.setMetadata(metadata);

            return fileInfo;

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            throw new StorageException("S3获取文件信息失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StorageException("S3获取文件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出文件
     *
     * <p>
     * 按 continuationToken 翻页聚合，直到返回条数达到 {@code maxKeys} 或结果不再截断；
     * 聚合超过 {@code maxKeys} 时截断返回前 {@code maxKeys} 条。
     * </p>
     */
    // 返回可变列表以保持 StorageStrategy 接口既有契约，允许调用方按需修改结果集（与 LocalStorageStrategy.list 一致）
    @SuppressWarnings("java:S6204")
    @Override
    public List<FileInfo> list(String bucket, String prefix, int maxKeys) {
        if (maxKeys <= 0) {
            return new ArrayList<>();
        }
        try {
            String bucketName = bucket;
            String normalizedPrefix = prefix == null ? null : normalizeKey(prefix);
            List<FileInfo> result = new ArrayList<>();
            String continuationToken = null;

            while (result.size() < maxKeys) {
                // 单次请求量按剩余条数收敛，且不超过 S3 协议上限 1000
                int requestKeys = Math.min(maxKeys - result.size(), 1000);
                ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedPrefix)
                    .maxKeys(requestKeys)
                    .continuationToken(continuationToken)
                    .build();

                ListObjectsV2Response response = s3Client.listObjectsV2(request);
                response.contents()
                    .forEach(s3Object -> result.add(toFileInfo(bucketName, s3Object)));

                continuationToken = response.nextContinuationToken();
                if (!Boolean.TRUE.equals(response.isTruncated())
                    || continuationToken == null) {
                    break;
                }
            }

            if (result.size() > maxKeys) {
                return new ArrayList<>(result.subList(0, maxKeys));
            }
            return result;

        } catch (Exception e) {
            throw new StorageException("S3列出文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 S3 对象摘要转换为 FileInfo
     *
     * @param bucket   存储桶
     * @param s3Object S3 对象摘要
     * @return 文件信息
     */
    private FileInfo toFileInfo(String bucket, S3Object s3Object) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setBucket(bucket);
        fileInfo.setPlatform(config.getPlatform());
        fileInfo.setPath(s3Object.key());
        fileInfo.setFullPath(s3Object.key());
        fileInfo.setName(getFileName(s3Object.key()));
        fileInfo.setSize(s3Object.size());
        fileInfo.setUrl(getFileUrl(s3Object.key()));
        fileInfo.setUploadTime(
            LocalDateTime.ofInstant(s3Object.lastModified(), java.time.ZoneId.systemDefault()));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("etag", s3Object.eTag());
        metadata.put("storageClass", s3Object.storageClassAsString());
        fileInfo.setMetadata(metadata);
        return fileInfo;
    }

    /**
     * 复制文件
     */
    @Override
    public void copy(String sourceBucket, String targetBucket, String sourcePath,
        String targetPath) {
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
    public void move(String sourceBucket, String targetBucket, String sourcePath,
        String targetPath) {
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

    @Override
    public String generatePresignedUrl(String bucket, String path, long expireSeconds) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expireSeconds))
                .getObjectRequest(getObject -> getObject.bucket(bucket).key(normalizeKey(path)))
                .build();

            PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();

        } catch (Exception e) {
            throw new StorageException("S3生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateUploadPresignedUrl(String bucket, String path, long expireSeconds) {
        try {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expireSeconds))
                .putObjectRequest(putObject -> putObject.bucket(bucket).key(normalizeKey(path)))
                .build();

            PresignedPutObjectRequest presignedRequest =
                s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            throw new StorageException("S3生成上传预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件URL
     *
     * @param path 文件路径
     * @return 文件访问 URL
     */
    private String getFileUrl(String path) {
        String baseUrl =
            StrUtil.isNotBlank(config.getDomain()) ? config.getDomain() : config.getEndpoint();
        if (StrUtil.isBlank(baseUrl)) {
            return StringConstants.SLASH + path;
        }
        baseUrl = StrUtil.removeSuffix(baseUrl, StringConstants.SLASH);
        if (StrUtil.isNotBlank(config.getDomain())) {
            return baseUrl + StringConstants.SLASH + path;
        }
        return baseUrl + StringConstants.SLASH + config.getBucketName() + StringConstants.SLASH
            + path;
    }

    /**
     * 获取文件名
     *
     * @param path 文件路径
     * @return 文件名
     */
    private String getFileName(String path) {
        int lastSlashIndex = path.lastIndexOf(StringConstants.SLASH);
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
    }

    /**
     * 分割列表
     *
     * @param <T>  元素类型
     * @param list 列表
     * @param size 每组大小
     * @return 分组后的列表
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
            CreateMultipartUploadRequest.Builder requestBuilder =
                CreateMultipartUploadRequest.builder()
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
            CreateMultipartUploadResponse response =
                s3Client.createMultipartUpload(requestBuilder.build());

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
        byte[] partBytes = null;
        Path tempFile = null;
        long partSize = 0L;
        try {
            String key = normalizeKey(path);

            if (StrUtil.isBlank(key)) {
                throw new StorageException("无效的uploadId: " + uploadId);
            }

            // 小分片优先走内存，超过阈值自动切换到临时文件，兼顾吞吐和内存控制
            PartPayload payload = readPartPayload(data);
            partBytes = payload.bytes();
            tempFile = payload.tempFile();
            partSize = payload.size();

            // 构建请求
            UploadPartRequest request = UploadPartRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength(partSize)
                .build();

            // 执行上传
            RequestBody requestBody = partBytes != null
                ? RequestBody.fromBytes(partBytes)
                : RequestBody.fromFile(tempFile);
            UploadPartResponse response = s3Client.uploadPart(request, requestBody);

            // 构建返回结果
            MultipartUploadResp result = new MultipartUploadResp();
            result.setPartNumber(partNumber);
            result.setPartETag(response.eTag());
            result.setPartSize(partSize);
            result.setSuccess(true);
            return result;

        } catch (Exception e) {
            MultipartUploadResp result = new MultipartUploadResp();
            result.setPartNumber(partNumber);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    LOGGER.warn("删除临时分片文件失败: {}", tempFile, e);
                }
            }
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
                .map(part -> CompletedPart.builder().partNumber(part.getPartNumber())
                    .eTag(part.getPartETag()).build())
                .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                .toList();

            // 构建请求
            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(multipartUpload -> multipartUpload.parts(completedParts))
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
                LOGGER.warn("无效的uploadId，可能已经完成或取消: {}", uploadId);
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
            LOGGER.error("取消分片上传失败: uploadId={}", uploadId, e);
            throw new StorageException("S3取消分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出已上传的分片
     */
    // 返回可变列表以保持 StorageStrategy 接口既有契约，允许调用方按需修改结果集（与 LocalStorageStrategy.listParts 一致）
    @SuppressWarnings("java:S6204")
    @Override
    public List<MultipartUploadResp> listParts(String bucket, String path, String uploadId) {
        try {
            String key = normalizeKey(path);
            if (StrUtil.isBlank(key)) {
                throw new StorageException("无效的uploadId: " + uploadId);
            }

            // 构建请求
            ListPartsRequest request =
                ListPartsRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build();

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
    private void validateParts(List<MultipartUploadResp> recordParts,
        List<MultipartUploadResp> s3Parts) {
        Map<Integer, String> partsByRecord = recordParts.stream()
            .collect(Collectors.toMap(MultipartUploadResp::getPartNumber,
                MultipartUploadResp::getPartETag));

        Map<Integer, String> partsByS3 = s3Parts.stream()
            .collect(Collectors.toMap(MultipartUploadResp::getPartNumber,
                MultipartUploadResp::getPartETag));

        // 检查分片数量
        if (partsByRecord.size() != partsByS3.size()) {
            throw new StorageException(
                String.format("分片数量不一致: 本地记录=%d, S3=%d", partsByRecord.size(), partsByS3.size()));
        }

        // 检查每个分片
        List<Integer> missingParts = new ArrayList<>();
        List<Integer> mismatchParts = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : partsByRecord.entrySet()) {
            Integer partNumber = entry.getKey();
            String recordETag = entry.getValue();
            String s3ETag = partsByS3.get(partNumber);

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
        String key = rawKey.trim().replace("\\", StringConstants.SLASH).replaceAll("/+",
            StringConstants.SLASH);
        while (key.startsWith(StringConstants.SLASH)) {
            key = key.substring(1);
        }
        return key;
    }

    private long resolveMultipartUploadThreshold(OssStorageConfig ossStorageConfig) {
        Long threshold = ossStorageConfig.getMultipartUploadThreshold();
        if (threshold == null || threshold <= 0) {
            return StorageConstant.DEFAULT_MULTIPART_UPLOAD_THRESHOLD;
        }
        return threshold;
    }

    private long resolveMultipartUploadPartSize(OssStorageConfig ossStorageConfig) {
        Long partSize = ossStorageConfig.getMultipartUploadPartSize();
        if (partSize == null || partSize <= 0) {
            return StorageConstant.DEFAULT_MULTIPART_UPLOAD_PART_SIZE;
        }
        return partSize;
    }

    private long resolveUploadPartInMemoryThreshold() {
        // 复用现有配置：以内存分片阈值=min(分片阈值, 分片大小)
        long threshold = Math.min(multipartUploadThreshold, multipartUploadPartSize);
        return Math.max(1L, threshold);
    }

    private String resolveMultipartTempDir(OssStorageConfig ossStorageConfig) {
        if (StrUtil.isBlank(ossStorageConfig.getMultipartTempDir())) {
            return StorageConstant.DEFAULT_LOCAL_MULTIPART_TEMP_DIR;
        }
        return ossStorageConfig.getMultipartTempDir().trim();
    }

    private S3Configuration buildS3Configuration(OssStorageConfig ossStorageConfig) {
        return S3Configuration.builder()
            .pathStyleAccessEnabled(ossStorageConfig.isPathStyleAccessEnabled())
            .accelerateModeEnabled(ossStorageConfig.isTransferAccelerationEnabled())
            .build();
    }

    private ClientOverrideConfiguration buildTimeoutConfig(OssStorageConfig ossStorageConfig) {
        int requestTimeoutSeconds = Math.max(1, ossStorageConfig.getRequestTimeout());
        Duration timeout = Duration.ofSeconds(requestTimeoutSeconds);
        return ClientOverrideConfiguration.builder()
            .apiCallAttemptTimeout(timeout)
            .apiCallTimeout(timeout.multipliedBy(2))
            .build();
    }

    private PartPayload readPartPayload(InputStream data) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream();
        OutputStream fileOutput = null;
        Path tempFile = null;
        long totalRead = 0L;

        try {
            int bytesRead;
            while ((bytesRead = data.read(buffer)) != -1) {
                totalRead += bytesRead;
                if (fileOutput != null) {
                    fileOutput.write(buffer, 0, bytesRead);
                } else if (totalRead <= uploadPartInMemoryThreshold) {
                    memoryBuffer.write(buffer, 0, bytesRead);
                } else {
                    // 超过阈值后，切换到文件模式，先写入已缓存内存数据
                    tempFile = createMultipartTempFile();
                    fileOutput = Files.newOutputStream(tempFile);
                    memoryBuffer.writeTo(fileOutput);
                    fileOutput.write(buffer, 0, bytesRead);
                }
            }
        } finally {
            if (fileOutput != null) {
                fileOutput.close();
            }
        }

        if (tempFile != null) {
            return new PartPayload(null, tempFile, totalRead);
        }
        return new PartPayload(memoryBuffer.toByteArray(), null, totalRead);
    }

    private Path createMultipartTempFile() throws IOException {
        Path tempDir = Path.of(multipartTempDir);
        Files.createDirectories(tempDir);
        return Files.createTempFile(tempDir, "storage-part-", ".tmp");
    }

    /**
     * 分片数据载体（内存缓冲或临时文件）
     *
     * @param bytes    内存数据（使用临时文件时为 null）
     * @param tempFile 临时文件（内存存储时为 null）
     * @param size     分片大小（字节）
     */
    private record PartPayload(byte[] bytes, Path tempFile, long size) {

        @Override
        public boolean equals(Object o) {
            return this == o
                || o instanceof PartPayload(byte[] otherBytes, Path otherTempFile, long otherSize)
                    && size == otherSize && Objects.equals(tempFile, otherTempFile)
                    && Arrays.equals(bytes, otherBytes);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(bytes);
            result = 31 * result + Objects.hashCode(tempFile);
            result = 31 * result + Long.hashCode(size);
            return result;
        }

        @Override
        public String toString() {
            return "PartPayload{" + "bytes.length=" + (bytes == null ? 0 : bytes.length)
                + ", tempFile=" + tempFile + ", size=" + size + '}';
        }
    }
}
