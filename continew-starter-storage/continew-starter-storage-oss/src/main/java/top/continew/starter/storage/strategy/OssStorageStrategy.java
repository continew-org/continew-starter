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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.validation.CheckUtils;
import top.continew.starter.core.validation.ValidationUtils;
import top.continew.starter.storage.client.OssClient;
import top.continew.starter.storage.constant.StorageConstant;
import top.continew.starter.storage.dao.StorageDao;
import top.continew.starter.storage.enums.FileTypeEnum;
import top.continew.starter.storage.model.req.StorageProperties;
import top.continew.starter.storage.model.resp.ThumbnailResp;
import top.continew.starter.storage.model.resp.UploadResp;
import top.continew.starter.storage.util.ImageThumbnailUtils;
import top.continew.starter.storage.util.OssUtils;
import top.continew.starter.storage.util.StorageUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * OSS存储策略
 * <p><a href="https://docs.aws.amazon.com/zh_cn/sdk-for-java/latest/developer-guide/home.html">...</a></p>
 *
 * @author echo
 * @date 2024/12/16 20:29
 */
public class OssStorageStrategy implements StorageStrategy<OssClient> {
    private final static Logger log = LoggerFactory.getLogger(OssStorageStrategy.class);

    private final OssClient client;
    private final StorageDao storageDao;
    private String etag;

    public OssStorageStrategy(OssClient ossClient, StorageDao storageDao) {
        this.client = ossClient;
        this.storageDao = storageDao;
    }

    private StorageProperties getStorageProperties() {
        return client.getProperties();
    }

    @Override
    public OssClient getClient() {
        return client;
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            // 调用 headBucket 请求，检查桶是否存在
            client.getClient().headBucket(HeadBucketRequest.builder().bucket(bucketName).build()).join();
            return true;  // 桶存在
        } catch (Exception e) {
            // 捕获异常，详细判断具体原因
            if (e.getCause() instanceof NoSuchBucketException) {
                // 桶不存在
                return false;
            } else if (e.getCause() instanceof S3Exception s3Exception) {
                // 检查是否是其他人创建的桶（403 Forbidden 错误）
                if (s3Exception.statusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new BusinessException("全局重复：存储桶名称已被他人创建：" + bucketName);
                }
            }
            // 捕获其他所有异常，并抛出
            throw new BusinessException("S3 存储桶查询失败，存储桶名称：" + bucketName, e);
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            if (!this.bucketExists(bucketName)) {
                client.getClient().createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).join();
            }
        } catch (S3Exception e) {
            throw new BusinessException("S3 存储桶,创建失败", e);
        }
    }

    @Override
    public UploadResp upload(String fileName, InputStream inputStream, String fileType) {
        String bucketName = getStorageProperties().getBucketName();
        return this.upload(bucketName, fileName, null, inputStream, fileType, false);
    }

    @Override
    public UploadResp upload(String fileName,
                             String path,
                             InputStream inputStream,
                             String fileType,
                             boolean isThumbnail) {
        String bucketName = getStorageProperties().getBucketName();
        return this.upload(bucketName, fileName, path, inputStream, fileType, isThumbnail);
    }

    @Override
    public UploadResp upload(String bucketName,
                             String fileName,
                             String path,
                             InputStream inputStream,
                             String fileType,
                             boolean isThumbnail) {
        try {

            // 可重复读流
            inputStream = StorageUtils.ensureByteArrayStream(inputStream);
            byte[] fileBytes = IoUtil.readBytes(inputStream);
            ValidationUtils.throwIf(fileBytes.length == 0, "输入流内容长度不可用或无效");
            // 获取文件扩展名
            String fileExtension = FileNameUtil.extName(fileName);
            // 格式化文件名 防止上传后重复
            String formatFileName = StorageUtils.formatFileName(fileName);
            // 判断文件路径是否为空  为空给默认路径 格式 2024/12/30/
            if (StrUtil.isEmpty(path)) {
                path = StorageUtils.ossDefaultPath();
            }
            ThumbnailResp thumbnailResp = null;
            //判断是否需要上传缩略图 前置条件 文件必须为图片
            boolean contains = FileTypeEnum.IMAGE.getExtensions().contains(fileExtension);
            if (contains && isThumbnail) {
                try (InputStream thumbnailStream = new ByteArrayInputStream(fileBytes)) {
                    thumbnailResp = this.uploadThumbnail(bucketName, formatFileName, path, thumbnailStream, fileType);
                }
            }

            // 上传文件
            try (InputStream uploadStream = new ByteArrayInputStream(fileBytes)) {
                this.upload(bucketName, formatFileName, path, uploadStream, fileType);
            }
            String eTag = etag;
            // 构建 上传后的文件路径地址 格式 xxx/xxx/xxx.jpg
            String filePath = path + formatFileName;
            // 构建 文件上传记录 并返回
            return buildStorageRecord(bucketName, fileName, filePath, eTag, fileBytes.length, thumbnailResp);
        } catch (IOException e) {
            throw new BusinessException("文件上传异常", e);
        }
    }

    @Override
    public void upload(String bucketName, String fileName, String path, InputStream inputStream, String fileType) {
        // 构建 S3 存储 文件路径
        String filePath = path + fileName;
        try {
            long available = inputStream.available();
            // 构建异步请求体，指定内容长度
            BlockingInputStreamAsyncRequestBody requestBody = BlockingInputStreamAsyncRequestBody.builder()
                .contentLength(available)
                .subscribeTimeout(Duration.ofSeconds(30))
                .build();

            // 初始化上传任务
            Upload upload = client.getTransferManager()
                .upload(u -> u.requestBody(requestBody)
                    .putObjectRequest(b -> b.bucket(bucketName).key(filePath).contentType(fileType).build())
                    .build());

            // 写入输入流内容到请求体
            requestBody.writeInputStream(inputStream);
            CompletedUpload uploadResult = upload.completionFuture().join();

            etag = uploadResult.response().eTag().replace("\"", "");
        } catch (IOException e) {
            throw new BusinessException("文件上传异常", e);
        }
    }

    @Override
    public ThumbnailResp uploadThumbnail(String bucketName,
                                         String fileName,
                                         String path,
                                         InputStream inputStream,
                                         String fileType) {
        // 获取文件扩展名
        String fileExtension = FileNameUtil.extName(fileName);
        // 生成缩略图文件名
        String thumbnailFileName = StorageUtils.buildThumbnailFileName(fileName, StorageConstant.SMALL_SUFFIX);
        // 处理文件为缩略图
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageThumbnailUtils.generateThumbnail(inputStream, outputStream, fileExtension);
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            // 上传文件
            this.upload(bucketName, thumbnailFileName, path, inputStream, fileType);
            return new ThumbnailResp((long)outputStream.size(), path + thumbnailFileName);
        } catch (IOException e) {
            throw new BusinessException("缩略图处理异常", e);
        }
    }

    @Override
    public InputStream download(String bucketName, String fileName) {
        try {
            // 构建下载请求
            DownloadRequest<ResponseInputStream<GetObjectResponse>> downloadRequest = DownloadRequest.builder()
                .getObjectRequest(req -> req.bucket(bucketName).key(fileName).build()) // 设置桶名和对象名
                .addTransferListener(LoggingTransferListener.create()) // 添加传输监听器
                .responseTransformer(AsyncResponseTransformer.toBlockingInputStream()) // 转换为阻塞输入流
                .build();
            // 执行下载操作
            Download<ResponseInputStream<GetObjectResponse>> download = client.getTransferManager()
                .download(downloadRequest);
            // 直接等待下载完成并返回 InputStream
            // 返回输入流
            return download.completionFuture().join().result();
        } catch (CompletionException e) {
            // 处理异步执行中的异常
            throw new BusinessException("文件下载失败，错误信息: " + e.getCause().getMessage(), e.getCause());
        } catch (Exception e) {
            // 捕获其他异常
            throw new BusinessException("文件下载失败，发生未知错误", e);
        }
    }

    @Override
    public void delete(String bucketName, String fileName) {
        try {
            client.getClient().deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(fileName).build());
        } catch (Exception e) {
            throw new BusinessException("S3 文件删除失败", e);
        }
    }

    @Override
    public String getImageBase64(String bucketName, String fileName) {
        try (InputStream inputStream = download(bucketName, fileName)) {
            if (ObjectUtil.isEmpty(inputStream)) {
                return null;
            }
            String extName = FileUtil.extName(fileName);
            boolean contains = FileTypeEnum.IMAGE.getExtensions().contains(extName);
            CheckUtils.throwIf(!contains, "{}非图片格式，无法获取", extName);
            return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new BusinessException("图片查看失败", e);
        }
    }

    /**
     * 构建储存记录
     *
     * @param bucketName    桶名称
     * @param fileName      文件名
     * @param filePath      文件路径
     * @param eTag          e 标记
     * @param contentLength 内容长度
     * @param thumbnailResp 相应缩略图
     * @return {@link UploadResp }
     */
    private UploadResp buildStorageRecord(String bucketName,
                                          String fileName,
                                          String filePath,
                                          String eTag,
                                          long contentLength,
                                          ThumbnailResp thumbnailResp) {
        // 获取终端地址
        String endpoint = client.getProperties().getEndpoint();
        // 判断桶策略
        boolean isPrivateBucket = this.isPrivate(bucketName);
        // 如果是私有桶 则生成私有URL链接 默认 访问时间为 12 小时
        String url = isPrivateBucket
            ? this.getPrivateUrl(bucketName, filePath, 12)
            : OssUtils.getUrl(endpoint, bucketName) + StringConstants.SLASH + filePath;

        String thumbnailUrl = "";
        long thumbnailSize = 0;
        // 判断缩略图响应是否为空
        if (ObjectUtil.isNotEmpty(thumbnailResp)) {
            // 同理按照 访问桶策略构建 缩略图访问地址
            thumbnailUrl = isPrivateBucket
                ? this.getPrivateUrl(bucketName, thumbnailResp.getThumbnailPath(), 12)
                : OssUtils.getUrl(endpoint, bucketName) + StringConstants.SLASH + thumbnailResp.getThumbnailPath();
            thumbnailSize = thumbnailResp.getThumbnailSize();
        }

        UploadResp uploadResp = new UploadResp();
        uploadResp.setCode(client.getProperties().getCode());
        uploadResp.setUrl(url);
        uploadResp.setBasePath(filePath);
        uploadResp.setOriginalFilename(fileName);
        uploadResp.setExt(FileNameUtil.extName(fileName));
        uploadResp.setSize(contentLength);
        uploadResp.setThumbnailUrl(thumbnailUrl);
        uploadResp.setThumbnailSize(thumbnailSize);
        uploadResp.seteTag(eTag);
        uploadResp.setPath(bucketName + filePath);
        uploadResp.setBucketName(bucketName);
        uploadResp.setCreateTime(LocalDateTime.now());
        storageDao.add(uploadResp);
        return uploadResp;
    }

    /**
     * 是否为私有桶
     *
     * @param bucketName 桶名称
     * @return boolean T 是 F 不是
     */
    private boolean isPrivate(String bucketName) {
        try {
            // 尝试获取桶的策略
            GetBucketPolicyResponse policyResponse = client.getClient()
                .getBucketPolicy(GetBucketPolicyRequest.builder().bucket(bucketName).build())
                .join();
            //转成 json
            String policy = policyResponse.policy();
            JSONObject json = new JSONObject(policy);
            // 为空则是私有
            return ObjectUtil.isEmpty(json.get("Statement"));
        } catch (Exception e) {
            // 如果 getBucketPolicy 抛出异常，说明不是 MinIO 或不支持策略
            log.warn("获取桶策略失败，可能是 MinIO，异常信息: {}", e.getMessage());
        }

        try {
            // 获取桶的 ACL 信息
            GetBucketAclResponse aclResponse = client.getClient()
                .getBucketAcl(GetBucketAclRequest.builder().bucket(bucketName).build())
                .join();
            List<Grant> grants = aclResponse.grants();
            // 只存在 FULL_CONTROL 权限并且只有一个 Grant，则认为是私有桶
            if (grants.size() == 1 && grants.stream()
                .anyMatch(grant -> grant.permission().equals(Permission.FULL_CONTROL))) {
                return true;
            }
            // 如果存在其他权限 (READ 或 WRITE)，认为是公开桶
            return grants.stream()
                .noneMatch(grant -> grant.permission().equals(Permission.READ) || grant.permission()
                    .equals(Permission.WRITE));
        } catch (Exception e) {
            // 如果 getBucketAcl 失败，可能是权限或连接问题
            log.error("获取桶 ACL 失败: {}", e.getMessage());
            return true; // 出现错误时，默认认为桶是私有的
        }
    }

    /**
     * 获取私有URL链接
     *
     * @param bucketName 桶名称
     * @param fileName   文件名
     * @param second     授权时间
     * @return {@link String }
     */
    private String getPrivateUrl(String bucketName, String fileName, Integer second) {
        try {
            return client.getPresigner()
                .presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(second))
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(fileName).build())
                    .build())
                .url()
                .toString();
        } catch (RuntimeException e) {
            throw new BusinessException("获取私有链接异常", e);
        }
    }
}
