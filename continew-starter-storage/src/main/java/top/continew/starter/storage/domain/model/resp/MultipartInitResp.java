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

package top.continew.starter.storage.domain.model.resp;

import java.util.Set;

/**
 * 分片上传初始化结果
 *
 * <p>
 * 复用命中已有会话时，本对象中的 {@code fileName}、{@code path} 来自已存在的会话，可能与本次请求传入的
 * {@code fileName} 不同（存储文件名不参与会话复用判定）。调用方后续上传分片、完成上传时必须使用本对象返回的
 * {@code path}，不能使用自行拼接的路径，否则对象会落到另一个位置。
 * </p>
 *
 * @author echo
 * @since 2.14.0
 */
public class MultipartInitResp {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 上传ID（S3返回的uploadId）
     */
    private String uploadId;

    /**
     * 存储桶
     */
    private String bucket;

    /**
     * 存储平台
     */
    private String platform;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 分片大小
     */
    private Long partSize;

    /**
     * 总分片数
     */
    private Integer totalParts;

    /**
     * 存储文件名
     */
    private String fileName;

    /**
     * 原始文件名
     *
     * <p>
     * 用户上传时的原始文件名，用于展示；未指定时为空，调用方可回退为 {@link #fileName}。
     * </p>
     */
    private String originalFileName;

    /**
     * 文件 MD5
     */
    private String fileMd5;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 扩展名
     */
    private String extension;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 文件父路径
     */
    private String parentPath;

    /**
     * 已上传分片编号集合
     */
    private Set<Integer> uploadedPartNumbers;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getPartSize() {
        return partSize;
    }

    public void setPartSize(Long partSize) {
        this.partSize = partSize;
    }

    public Integer getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(Integer totalParts) {
        this.totalParts = totalParts;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public Set<Integer> getUploadedPartNumbers() {
        return uploadedPartNumbers;
    }

    public void setUploadedPartNumbers(Set<Integer> uploadedPartNumbers) {
        this.uploadedPartNumbers = uploadedPartNumbers;
    }
}
