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

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.enums.DefaultStorageSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储属性
 *
 * @author echo
 * @since 2.14.0
 */
@ConfigurationProperties(prefix = PropertiesConstants.STORAGE)
public class StorageProperties {

    /**
     * 默认使用的存储平台
     */
    private String defaultPlatform = StorageConstant.DEFAULT_STORAGE_PLATFORM;

    /**
     * 默认存储配置来源 (配置文件/动态配置)
     */
    private DefaultStorageSource defaultStorageSource = DefaultStorageSource.DYNAMIC;

    /**
     * 全局分片阈值（字节）
     */
    private long multipartUploadThreshold = StorageConstant.DEFAULT_MULTIPART_UPLOAD_THRESHOLD;

    /**
     * 全局分片大小（字节）
     */
    private long multipartUploadPartSize = StorageConstant.DEFAULT_MULTIPART_UPLOAD_PART_SIZE;

    /**
     * 全局本地分片临时目录
     */
    private String localMultipartTempDir = StorageConstant.DEFAULT_LOCAL_MULTIPART_TEMP_DIR;

    /**
     * 本地存储配置列表
     */
    private List<LocalStorageConfig> local = new ArrayList<>();

    /**
     * oss 存储配置列表
     */
    private List<OssStorageConfig> oss = new ArrayList<>();

    public String getDefaultPlatform() {
        return defaultPlatform;
    }

    public void setDefaultPlatform(String defaultPlatform) {
        this.defaultPlatform = defaultPlatform;
    }

    public DefaultStorageSource getDefaultStorageSource() {
        return defaultStorageSource;
    }

    public void setDefaultStorageSource(DefaultStorageSource defaultStorageSource) {
        this.defaultStorageSource = defaultStorageSource;
    }

    public List<LocalStorageConfig> getLocal() {
        return local;
    }

    public void setLocal(List<LocalStorageConfig> local) {
        this.local = local;
    }

    public List<OssStorageConfig> getOss() {
        return oss;
    }

    public void setOss(List<OssStorageConfig> oss) {
        this.oss = oss;
    }

    public long getMultipartUploadThreshold() {
        return multipartUploadThreshold;
    }

    public void setMultipartUploadThreshold(long multipartUploadThreshold) {
        this.multipartUploadThreshold = multipartUploadThreshold;
    }

    public long getMultipartUploadPartSize() {
        return multipartUploadPartSize;
    }

    public void setMultipartUploadPartSize(long multipartUploadPartSize) {
        this.multipartUploadPartSize = multipartUploadPartSize;
    }

    public String getLocalMultipartTempDir() {
        return localMultipartTempDir;
    }

    public void setLocalMultipartTempDir(String localMultipartTempDir) {
        this.localMultipartTempDir = localMultipartTempDir;
    }
}
