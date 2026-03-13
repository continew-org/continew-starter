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

package top.continew.starter.storage.autoconfigure.properties;

import top.continew.starter.storage.common.constant.StorageConstant;

/**
 * 本地存储配置
 *
 * @author echo
 * @since 2.14.0
 */
public class LocalStorageConfig {

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 存储平台
     */
    private String platform;

    /**
     * 存储路径
     */
    private String bucketName;

    /**
     * 访问路径
     */
    private String endpoint;

    /**
     * 多部分上传阈值（字节）
     */
    private Long multipartUploadThreshold;

    /**
     * 多部分上传的部分大小（字节）
     */
    private Long multipartUploadPartSize;

    /**
     * 分片上传临时目录
     */
    private String multipartTempDir = StorageConstant.DEFAULT_LOCAL_MULTIPART_TEMP_DIR;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Long getMultipartUploadThreshold() {
        return multipartUploadThreshold;
    }

    public void setMultipartUploadThreshold(Long multipartUploadThreshold) {
        this.multipartUploadThreshold = multipartUploadThreshold;
    }

    public Long getMultipartUploadPartSize() {
        return multipartUploadPartSize;
    }

    public void setMultipartUploadPartSize(Long multipartUploadPartSize) {
        this.multipartUploadPartSize = multipartUploadPartSize;
    }

    public String getMultipartTempDir() {
        return multipartTempDir;
    }

    public void setMultipartTempDir(String multipartTempDir) {
        this.multipartTempDir = multipartTempDir;
    }
}
