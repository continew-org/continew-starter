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

package top.continew.starter.storage.domain.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 增强版 MultipartFile 实现，支持缓存和包装
 * 功能特性：
 * 1. 缓存功能：避免重复读取文件内容
 * 2. 包装模式：可以包装现有的 MultipartFile
 * 3. 创建模式：可以直接从字节数组创建
 *
 * @author echo
 * @since 2.14.0
 */
public class EnhancedMultipartFile implements MultipartFile {

    private final MultipartFile originalFile;
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private byte[] cachedBytes;
    private final boolean cacheEnabled;
    private final boolean isWrapped;

    /**
     * 包装模式构造器 - 包装现有的 MultipartFile
     */
    public EnhancedMultipartFile(MultipartFile originalFile, boolean enableCache) {
        this.originalFile = originalFile;
        this.name = originalFile.getName();
        this.originalFilename = originalFile.getOriginalFilename();
        this.contentType = originalFile.getContentType();
        this.cacheEnabled = enableCache;
        this.isWrapped = true;
        this.cachedBytes = null;
    }

    /**
     * 创建模式构造器 - 直接从字节数组创建
     */
    public EnhancedMultipartFile(String name, String originalFilename, String contentType,
        byte[] content) {
        this.originalFile = null;
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.cachedBytes = content;
        this.cacheEnabled = false;
        this.isWrapped = false;
    }

    /**
     * 便捷的静态工厂方法 - 包装已有文件并启用缓存
     */
    public static EnhancedMultipartFile wrap(MultipartFile file, boolean enableCache) {
        if (file instanceof EnhancedMultipartFile) {
            return (EnhancedMultipartFile) file;
        }
        return new EnhancedMultipartFile(file, enableCache);
    }

    /**
     * 便捷的静态工厂方法 - 包装已有文件（不启用缓存）
     */
    public static EnhancedMultipartFile wrap(MultipartFile file) {
        return wrap(file, false);
    }

    /**
     * 便捷的静态工厂方法 - 创建新文件
     */
    public static EnhancedMultipartFile create(String name,
        String originalFilename,
        String contentType,
        byte[] content) {
        return new EnhancedMultipartFile(name, originalFilename, contentType, content);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        if (isWrapped) {
            return originalFile.isEmpty();
        }
        return cachedBytes == null || cachedBytes.length == 0;
    }

    @Override
    public long getSize() {
        if (cachedBytes != null) {
            return cachedBytes.length;
        }
        if (isWrapped) {
            return originalFile.getSize();
        }
        return 0;
    }

    @Override
    public byte[] getBytes() throws IOException {
        // 缓存模式下的处理
        if (cacheEnabled) {
            return getBytesWithCache();
        }

        // 非缓存模式下的处理
        return getBytesWithoutCache();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // 如果已有缓存数据，直接使用
        if (cachedBytes != null) {
            return new ByteArrayInputStream(cachedBytes);
        }

        // 缓存模式且需要缓存
        if (cacheEnabled && isWrapped) {
            loadToCache();
            return new ByteArrayInputStream(cachedBytes);
        }

        // 非缓存模式且是包装模式
        if (isWrapped) {
            return originalFile.getInputStream();
        }

        // 创建模式（理论上不应该到这里，因为创建模式下cachedBytes应该有值）
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        if (cachedBytes != null) {
            // 使用缓存的数据
            Files.write(dest.toPath(), cachedBytes);
        } else if (isWrapped) {
            // 使用原始文件
            originalFile.transferTo(dest);
        } else {
            // 创建模式但没有数据
            if (!dest.createNewFile()) {
                throw new IOException("目标文件已存在: " + dest.getAbsolutePath());
            }
        }
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        if (cacheEnabled && isWrapped) {
            cachedBytes = null;
        }
    }

    /**
     * 判断是否已缓存
     */
    public boolean isCached() {
        return cachedBytes != null;
    }

    /**
     * 获取缓存的字节数组（不触发加载）
     */
    public byte[] getCachedBytes() {
        return cachedBytes;
    }

    /**
     * 判断是否为包装模式
     */
    public boolean isWrapped() {
        return isWrapped;
    }

    /**
     * 判断是否启用缓存
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * 获取字节数组 - 带缓存
     */
    private byte[] getBytesWithCache() throws IOException {
        if (cachedBytes == null && isWrapped) {
            loadToCache();
        }
        return cachedBytes;
    }

    /**
     * 获取字节数组 - 不带缓存
     */
    private byte[] getBytesWithoutCache() throws IOException {
        if (isWrapped) {
            return originalFile.getBytes();
        }
        // 创建模式下直接返回内容
        return cachedBytes;
    }

    /**
     * 加载文件内容到缓存
     */
    private void loadToCache() throws IOException {
        if (isWrapped && originalFile != null) {
            cachedBytes = originalFile.getBytes();
        }
    }
}
