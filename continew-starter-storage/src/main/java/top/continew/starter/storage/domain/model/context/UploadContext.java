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

package top.continew.starter.storage.domain.model.context;

import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.storage.domain.model.req.ThumbnailSize;
import top.continew.starter.storage.processor.progress.UploadProgressListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 上传上下文，包含上传过程中的所有信息
 *
 * @author echo
 * @since 2.14.0
 */
public class UploadContext {

    /**
     * 原始文件
     */
    private MultipartFile file;

    /**
     * 存储平台代码
     */
    private String platform;

    /**
     * 桶名
     */
    private String bucket;

    /**
     * 文件路径（不含文件名）
     */
    private String path;

    /**
     * 格式化文件名 为空则默认使用格式化规则格式化
     */
    private String formatFileName;

    /**
     * 是否生成缩略图
     */
    private boolean generateThumbnail;

    /**
     * 缩略图尺寸
     */
    private ThumbnailSize thumbnailSize;

    /**
     * 元数据
     */
    private Map<String, String> metadata = new HashMap<>();

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 进度监听器
     */
    private UploadProgressListener progressListener;

    /**
     * 获取完整路径
     */
    public String getFullPath() {
        String safePath = path == null ? "" : path.trim();
        String safeFileName = formatFileName == null ? "" : formatFileName.trim();

        if (safePath.isEmpty()) {
            return safeFileName;
        }
        if (safeFileName.isEmpty()) {
            return safePath;
        }
        return safePath.endsWith(StringConstants.SLASH)
            ? safePath + safeFileName
            : safePath + StringConstants.SLASH + safeFileName;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public String getFormatFileName() {
        return formatFileName;
    }

    public void setFormatFileName(String formatFileName) {
        this.formatFileName = formatFileName;
    }

    public boolean isGenerateThumbnail() {
        return generateThumbnail;
    }

    public void setGenerateThumbnail(boolean generateThumbnail) {
        this.generateThumbnail = generateThumbnail;
    }

    public ThumbnailSize getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(ThumbnailSize thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public UploadProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(UploadProgressListener progressListener) {
        this.progressListener = progressListener;
    }
}
