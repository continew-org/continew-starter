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
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.validation.CheckUtils;
import top.continew.starter.core.validation.ValidationUtils;
import top.continew.starter.storage.client.LocalClient;
import top.continew.starter.storage.constant.StorageConstant;
import top.continew.starter.storage.dao.StorageDao;
import top.continew.starter.storage.enums.FileTypeEnum;
import top.continew.starter.storage.model.req.StorageProperties;
import top.continew.starter.storage.model.resp.ThumbnailResp;
import top.continew.starter.storage.model.resp.UploadResp;
import top.continew.starter.storage.util.ImageThumbnailUtils;
import top.continew.starter.storage.util.LocalUtils;
import top.continew.starter.storage.util.StorageUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 本地存储策略
 *
 * @author echo
 * @date 2024/12/16 19:48
 */
public class LocalStorageStrategy implements StorageStrategy<LocalClient> {

    private final LocalClient client;
    private final StorageDao storageDao;

    public LocalStorageStrategy(LocalClient client, StorageDao storageDao) {
        this.client = client;
        this.storageDao = storageDao;
    }

    private StorageProperties getStorageProperties() {
        return client.getProperties();
    }

    @Override
    public LocalClient getClient() {
        return client;
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return Files.exists(Path.of(bucketName));
        } catch (RuntimeException e) {
            throw new BusinessException("local存储 查询桶 失败", e);
        }
    }

    @Override
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            try {
                Files.createDirectories(Path.of(bucketName));
            } catch (IOException e) {
                throw new BusinessException("local存储 创建桶 失败", e);
            }
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
            // 获取流大小
            byte[] originalBytes = IoUtil.readBytes(inputStream);
            ValidationUtils.throwIf(originalBytes.length == 0, "输入流内容长度不可用或无效");

            // 获取文件扩展名
            String fileExtension = FileNameUtil.extName(fileName);
            // 格式化文件名 防止上传后重复
            String formatFileName = StorageUtils.formatFileName(fileName);
            // 判断文件路径是否为空  为空给默认路径 格式 2024/12/30/
            if (StrUtil.isEmpty(path)) {
                path = StorageUtils.localDefaultPath();
            }
            // 判断文件夹是否存在 不存在则创建
            Path folderPath = Paths.get(bucketName, path);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
            ThumbnailResp thumbnailResp = null;
            //判断是否需要上传缩略图 前置条件 文件必须为图片
            boolean contains = FileTypeEnum.IMAGE.getExtensions().contains(fileExtension);
            if (contains && isThumbnail) {
                try (InputStream thumbnailStream = new ByteArrayInputStream(originalBytes)) {
                    thumbnailResp = this.uploadThumbnail(bucketName, formatFileName, path, thumbnailStream, fileType);
                }
            }

            // 上传文件
            try (InputStream uploadStream = new ByteArrayInputStream(originalBytes)) {
                this.upload(bucketName, formatFileName, path, uploadStream, fileType);
            }

            // 构建文件 md5
            String eTag = LocalUtils.calculateMD5(inputStream);
            // 构建 上传后的文件路径地址 格式 xxx/xxx/xxx.jpg
            String filePath = Paths.get(path, formatFileName).toString();
            // 构建 文件上传记录 并返回
            return buildStorageRecord(bucketName, fileName, filePath, eTag, originalBytes.length, thumbnailResp);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new BusinessException("文件上传异常", e);
        }

    }

    @Override
    public void upload(String bucketName, String fileName, String path, InputStream inputStream, String fileType) {
        byte[] fileBytes = IoUtil.readBytes(inputStream);
        // 拼接完整地址
        String filePath = Paths.get(bucketName, path, fileName).toString();
        try {
            //上传文件
            File targetFile = new File(filePath);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(fileBytes);
            }
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
            this.upload(bucketName, thumbnailFileName, path, inputStream, null);

            return new ThumbnailResp((long)outputStream.size(), Paths.get(path, thumbnailFileName).toString());
        } catch (IOException e) {
            throw new BusinessException("缩略图处理异常", e);
        }
    }

    @Override
    public InputStream download(String bucketName, String fileName) {
        String fullPath = Paths.get(bucketName, fileName).toString();
        File file = new File(fullPath);
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            throw new BusinessException("下载文件异常", e);
        }
    }

    @Override
    public void delete(String bucketName, String fileName) {
        try {
            String fullPath = Paths.get(bucketName, fileName).toString();
            Files.delete(Paths.get(fullPath));
        } catch (Exception e) {
            throw new BusinessException("删除文件异常", e);
        }
    }

    @Override
    public String getImageBase64(String bucketName, String fileName) {
        try (InputStream inputStream = download(bucketName, fileName)) {
            if (ObjectUtil.isEmpty(inputStream)) {
                return null;
            }
            String extName = FileUtil.extName(fileName);
            CheckUtils.throwIf(!FileTypeEnum.IMAGE.getExtensions().contains(extName), "{} 不是图像格式", extName);
            return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new BusinessException("无法查看图片", e);
        }
    }

    /**
     * 构建存储记录
     *
     * @param bucketName    桶名称
     * @param fileName      原始文件名
     * @param filePath      文件路径 xx/xx/xxx.jpg
     * @param eTag          标签 - md5
     * @param size          文件大小
     * @param thumbnailResp 缩略图信息
     * @return {@link UploadResp }
     */
    private UploadResp buildStorageRecord(String bucketName,
                                          String fileName,
                                          String filePath,
                                          String eTag,
                                          long size,
                                          ThumbnailResp thumbnailResp) {
        // 获取当前存储 code
        String code = client.getProperties().getCode();
        // 构建访问地址前缀
        String baseUrl = "http://" + getStorageProperties().getEndpoint() + StringConstants.SLASH;

        UploadResp resp = new UploadResp();
        resp.setCode(code);
        resp.setUrl(baseUrl + filePath);
        resp.setBasePath(filePath);
        resp.setOriginalFilename(fileName);
        resp.setExt(FileNameUtil.extName(fileName));
        resp.setSize(size);
        resp.seteTag(eTag);
        resp.setPath(filePath);
        resp.setBucketName(bucketName);
        resp.setCreateTime(LocalDateTime.now());
        if (ObjectUtil.isNotEmpty(thumbnailResp)) {
            resp.setThumbnailUrl(baseUrl + thumbnailResp.getThumbnailPath());
            resp.setThumbnailSize(thumbnailResp.getThumbnailSize());
        }
        storageDao.add(resp);
        return resp;
    }
}
