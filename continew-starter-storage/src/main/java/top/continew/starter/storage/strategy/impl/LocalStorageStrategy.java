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

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.SpringUtils;
import top.continew.starter.storage.autoconfigure.properties.LocalStorageConfig;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 本地存储策略
 *
 * @author echo
 * @since 2.14.0
 */
public class LocalStorageStrategy implements StorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageStrategy.class);

    private final LocalStorageConfig config;

    /**
     * 分片上传分片大小
     */
    private final long multipartUploadPartSize;

    /**
     * 分片上传临时根目录
     */
    private final String multipartTempDir;

    public LocalStorageStrategy(LocalStorageConfig config) {
        this.config = config;
        this.multipartUploadPartSize = resolveMultipartUploadPartSize(config);
        this.multipartTempDir = resolveMultipartTempDir(config);
        initTempDir(config.getBucketName());
        registerResources(config);
    }

    /**
     * 注册资源
     *
     * @param config 配置
     */
    public void registerResources(LocalStorageConfig config) {
        // 注册资源映射
        SpringUtils.registerResourceHandler(MapUtil.of(URLUtil.url(config.getEndpoint()).getPath(), config
            .getBucketName()));
    }

    /**
     * 初始化临时目录
     */
    private void initTempDir(String bucket) {
        Path tempPath = resolveMultipartTempPath(bucket, null);
        try {
            Files.createDirectories(tempPath);
        } catch (IOException e) {
            log.error("创建临时目录失败", e);
        }
    }

    @Override
    public void upload(String bucket, String path, MultipartFile file) {
        String normalizedPath = normalizePath(path);
        Path filePath = Paths.get(bucket, normalizedPath);
        try {
            // 创建目录
            Files.createDirectories(filePath.getParent());

            // 复制文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     */
    @Override
    public InputStream download(String bucket, String path) {
        Path filePath = Paths.get(bucket, normalizePath(path));
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new StorageException("本地读取失败: " + e.getMessage(), e);
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
        Path filePath = Paths.get(bucket, normalizePath(path));
        try {
            Files.deleteIfExists(filePath);
            // 尝试删除空目录
            deleteEmptyParentDirectories(filePath.getParent());

        } catch (IOException e) {
            throw new StorageException("删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void batchDelete(String bucket, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        List<String> failedPaths = new ArrayList<>();
        Set<Path> parentDirs = new HashSet<>();
        for (String path : paths) {
            try {
                Path filePath = Paths.get(bucket, normalizePath(path));
                Files.deleteIfExists(filePath);
                parentDirs.add(filePath.getParent());
            } catch (IOException e) {
                failedPaths.add(path);
                log.warn("批量删除本地文件失败: path={}", path, e);
            }
        }
        // 文件删除后统一尝试清理空目录，避免批量删除过程中的重复递归扫描
        for (Path parentDir : parentDirs) {
            deleteEmptyParentDirectories(parentDir);
        }
        if (!failedPaths.isEmpty()) {
            throw new StorageException("批量删除失败，部分文件删除异常: " + failedPaths);
        }
    }

    /**
     * 检查文件是否存在
     */
    @Override
    public boolean exists(String bucket, String path) {
        Path filePath = Paths.get(bucket, normalizePath(path));
        return Files.exists(filePath);
    }

    /**
     * 获取文件信息
     */
    @Override
    public FileInfo getFileInfo(String bucket, String path) {
        String normalizedPath = normalizePath(path);
        Path filePath = Paths.get(bucket, normalizedPath);

        if (!Files.exists(filePath)) {
            return null;
        }

        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setBucket(bucket);
            fileInfo.setPlatform(config.getPlatform());
            fileInfo.setPath(normalizedPath);
            fileInfo.setFullPath(normalizedPath);
            fileInfo.setName(filePath.getFileName().toString());
            fileInfo.setSize(attrs.size());
            fileInfo.setUrl(config.getEndpoint() + StringConstants.SLASH + normalizedPath);
            fileInfo.setUploadTime(LocalDateTime.ofInstant(attrs.creationTime().toInstant(), java.time.ZoneId
                .systemDefault()));

            // 添加元数据
            Map<String, String> metadata = new HashMap<>();
            metadata.put("etag", calculateMD5(filePath));
            fileInfo.setMetadata(metadata);

            return fileInfo;

        } catch (Exception e) {
            throw new StorageException("获取文件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出文件
     */
    @Override
    public List<FileInfo> list(String bucket, String prefix, int maxKeys) {
        Path basePath = Paths.get(bucket);
        String normalizedPrefix = prefix == null ? null : normalizePath(prefix);
        Path searchPath = normalizedPrefix != null ? basePath.resolve(normalizedPrefix) : basePath;

        try (Stream<Path> stream = Files.walk(searchPath)) {
            return stream.filter(Files::isRegularFile).limit(maxKeys).map(path -> {
                String relativePath = basePath.relativize(path).toString();
                return getFileInfo(bucket, relativePath);
            }).filter(Objects::nonNull).collect(Collectors.toList());

        } catch (IOException e) {
            throw new StorageException("列出文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 复制文件
     */
    @Override
    public void copy(String sourceBucket, String targetBucket, String sourcePath, String targetPath) {
        Path source = Paths.get(sourceBucket, normalizePath(sourcePath));
        Path target = Paths.get(targetBucket, normalizePath(targetPath));

        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("复制文件失败: " + e.getMessage(), e);
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
     * 生成预签名URL（本地存储返回普通URL）
     */
    @Override
    public String generatePresignedUrl(String bucket, String path, long expireSeconds) {
        // 本地存储不支持预签名URL，返回普通URL
        return config.getEndpoint() + StringConstants.SLASH + normalizePath(path);
    }

    /**
     * 删除空的父目录
     */
    private void deleteEmptyParentDirectories(Path dir) {
        if (dir == null || !dir.startsWith(config.getBucketName())) {
            return;
        }

        try {
            if (Files.isDirectory(dir) && isDirectoryEmpty(dir)) {
                Files.delete(dir);
                deleteEmptyParentDirectories(dir.getParent());
            }
        } catch (IOException e) {
            // 忽略删除目录失败的错误
        }
    }

    /**
     * 检查目录是否为空
     */
    private boolean isDirectoryEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        }
    }

    @Override
    public MultipartInitResp initMultipartUpload(String bucket,
                                                 String path,
                                                 String contentType,
                                                 Map<String, String> metadata) {
        try {
            String uploadId = UUID.randomUUID().toString();
            String normalizedPath = normalizePath(path);

            // 创建临时目录
            Path tempUploadPath = resolveMultipartTempPath(bucket, uploadId);
            Files.createDirectories(tempUploadPath);

            // 构建结果
            MultipartInitResp result = new MultipartInitResp();
            result.setBucket(bucket);
            result.setFileId(UUID.randomUUID().toString());
            result.setUploadId(uploadId);
            result.setPlatform(config.getPlatform());
            result.setPath(normalizedPath);
            result.setPartSize(multipartUploadPartSize);
            return result;
        } catch (Exception e) {
            throw new StorageException("初始化分片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MultipartUploadResp uploadPart(String bucket,
                                          String path,
                                          String uploadId,
                                          int partNumber,
                                          InputStream data) {
        MultipartUploadResp result = new MultipartUploadResp();
        result.setPartNumber(partNumber);

        try {
            // 分片文件路径
            Path partPath = resolveMultipartTempPath(bucket, uploadId).resolve(buildPartFileName(partNumber));
            Files.createDirectories(partPath.getParent());

            // 保存分片
            long size = Files.copy(data, partPath, StandardCopyOption.REPLACE_EXISTING);

            // 计算MD5作为ETag
            String eTag = calculateMD5(partPath);

            result.setPartETag(eTag);
            result.setPartSize(size);
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public FileInfo completeMultipartUpload(String bucket,
                                            String path,
                                            String uploadId,
                                            List<MultipartUploadResp> parts,
                                            boolean verifyParts) {
        try {
            // 本地存储不需要验证，直接使用传入的分片信息
            String normalizedPath = normalizePath(path);
            Path targetPath = Paths.get(bucket, normalizedPath);
            Files.createDirectories(targetPath.getParent());

            // 合并分片
            try (OutputStream out = Files
                .newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                // 按分片编号排序
                List<MultipartUploadResp> sortedParts = parts.stream()
                    .filter(MultipartUploadResp::isSuccess)
                    .sorted(Comparator.comparingInt(MultipartUploadResp::getPartNumber))
                    .toList();

                // 逐个读取并写入
                for (MultipartUploadResp part : sortedParts) {
                    Path partPath = resolveMultipartTempPath(bucket, uploadId).resolve(buildPartFileName(part
                        .getPartNumber()));

                    if (!Files.exists(partPath)) {
                        throw new StorageException("分片文件不存在: part " + part.getPartNumber());
                    }

                    Files.copy(partPath, out);
                }
            }

            // 清理临时文件
            cleanupTempFiles(bucket, uploadId);

            // 获取文件信息
            return getFileInfo(bucket, normalizedPath);

        } catch (Exception e) {
            throw new StorageException("完成分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消分片上传
     */
    @Override
    public void abortMultipartUpload(String bucket, String path, String uploadId) {
        try {
            // 清理临时文件
            cleanupTempFiles(bucket, uploadId);
        } catch (Exception e) {
            log.error("取消分片上传失败: uploadId={}", uploadId, e);
        }
    }

    @Override
    public List<MultipartUploadResp> listParts(String bucket, String path, String uploadId) {
        // 本地存储不再维护分片信息，返回空列表
        // 实际分片信息应该从 FileRecorder 获取
        return new ArrayList<>();
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(String bucket, String uploadId) {
        Path tempUploadPath = resolveMultipartTempPath(bucket, uploadId);
        if (Files.exists(tempUploadPath)) {
            try (Stream<Path> paths = Files.walk(tempUploadPath)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.error("删除分片临时文件失败: path={}", path, e);
                    }
                }
            } catch (IOException e) {
                log.error("清理临时文件失败: uploadId={}", uploadId, e);
            }
        }
    }

    /**
     * 计算文件MD5
     */
    private String calculateMD5(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
        }
        byte[] digest = md.digest();
        return HexFormat.of().formatHex(digest).toLowerCase();
    }

    private String normalizePath(String rawPath) {
        String normalized = rawPath == null ? StringConstants.EMPTY : rawPath.trim();
        normalized = normalized.replace("\\", StringConstants.SLASH).replaceAll("/+", StringConstants.SLASH);
        while (normalized.startsWith(StringConstants.SLASH)) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private long resolveMultipartUploadPartSize(LocalStorageConfig localStorageConfig) {
        Long partSize = localStorageConfig.getMultipartUploadPartSize();
        if (partSize == null || partSize <= 0) {
            return StorageConstant.DEFAULT_MULTIPART_UPLOAD_PART_SIZE;
        }
        return partSize;
    }

    private String resolveMultipartTempDir(LocalStorageConfig localStorageConfig) {
        if (StrUtil.isBlank(localStorageConfig.getMultipartTempDir())) {
            return StorageConstant.DEFAULT_LOCAL_MULTIPART_TEMP_DIR;
        }
        return localStorageConfig.getMultipartTempDir().trim();
    }

    private Path resolveMultipartTempPath(String bucket, String uploadId) {
        Path path = Paths.get(multipartTempDir).resolve(toBucketSegment(bucket));
        if (StrUtil.isNotBlank(uploadId)) {
            path = path.resolve(uploadId);
        }
        return path;
    }

    private String toBucketSegment(String bucket) {
        if (StrUtil.isBlank(bucket)) {
            return "default";
        }
        return bucket.trim().replace("\\", "_").replace("/", "_").replace(":", "_");
    }

    private String buildPartFileName(int partNumber) {
        return "part_" + partNumber;
    }

    @Override
    public void cleanup() {
        // 清理静态资源映射
        if (config != null) {
            SpringUtils.deRegisterResourceHandler(MapUtil.of(URLUtil.url(config.getEndpoint()).getPath(), config
                .getBucketName()));
        }
    }

}
