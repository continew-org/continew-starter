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

import java.time.LocalDateTime;

/**
 * 文件分片信息
 *
 * @author echo
 * @since 2.14.0
 */
public class FilePartInfo {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 分片编号（从1开始）
     */
    private Integer partNumber;

    /**
     * 分片大小
     */
    private Long partSize;

    /**
     * 分片MD5
     */
    private String partMd5;

    /**
     * 分片ETag（S3返回的标识）
     */
    private String partETag;

    /**
     * 上传ID（S3分片上传标识）
     */
    private String uploadId;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 状态：UPLOADING, SUCCESS, FAILED
     */
    private String status;

    /**
     * 存储桶
     */
    private String bucket;

    /**
     * 文件路径
     */
    private String path;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public Long getPartSize() {
        return partSize;
    }

    public void setPartSize(Long partSize) {
        this.partSize = partSize;
    }

    public String getPartMd5() {
        return partMd5;
    }

    public void setPartMd5(String partMd5) {
        this.partMd5 = partMd5;
    }

    public String getPartETag() {
        return partETag;
    }

    public void setPartETag(String partETag) {
        this.partETag = partETag;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
