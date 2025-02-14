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

package top.continew.starter.storage.model.resp;

import java.time.LocalDateTime;

/**
 * 上传结果
 *
 * @author echo
 * @since 2.9.0
 */
public class UploadResp {

    /**
     * 存储 code
     */
    private String code;

    /**
     * 访问地址
     * <p>如果桶为私有,则提供临时链接,时间默认为 12 小时</p>
     */
    private String url;

    /**
     * 文件基础路径
     */
    private String basePath;

    /**
     * 原始 文件名
     */
    private String originalFilename;

    /**
     * 扩展名
     */
    private String ext;

    /**
     * 文件大小(字节)
     */
    private long size;

    /**
     * 已上传对象的实体标记（用来校验文件）-S3
     */
    private String eTag;

    /**
     * 存储路径
     * <p></p> 格式 桶/文件名 continew/2024/12/24/1234.jpg
     */
    private String path;

    /**
     * 存储桶
     */
    private String bucketName;

    /**
     * 缩略图大小（字节)
     */
    private Long thumbnailSize;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 上传时间
     */
    private LocalDateTime createTime;

    public UploadResp() {
    }

    public UploadResp(String code,
                      String url,
                      String basePath,
                      String originalFilename,
                      String ext,
                      long size,
                      String eTag,
                      String path,
                      String bucketName,
                      Long thumbnailSize,
                      String thumbnailUrl,
                      LocalDateTime createTime) {
        this.code = code;
        this.url = url;
        this.basePath = basePath;
        this.originalFilename = originalFilename;
        this.ext = ext;
        this.size = size;
        this.eTag = eTag;
        this.path = path;
        this.bucketName = bucketName;
        this.thumbnailSize = thumbnailSize;
        this.thumbnailUrl = thumbnailUrl;
        this.createTime = createTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Long getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(Long thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
