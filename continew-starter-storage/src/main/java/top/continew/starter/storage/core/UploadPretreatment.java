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

package top.continew.starter.storage.core;

import cn.hutool.core.util.StrUtil;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.domain.model.req.ThumbnailSize;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.processor.progress.UploadProgressListener;
import top.continew.starter.storage.service.FileProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 上传预处理器，支持链式调用
 *
 * @author echo
 * @since 2.14.0
 */
public class UploadPretreatment {

    private final FileStorageService storageService;
    private final UploadContext context;
    private UploadProgressListener progressListener;
    private final List<FileProcessor> processors = new ArrayList<>();

    public UploadPretreatment(FileStorageService storageService, MultipartFile file) {
        this.storageService = storageService;
        this.context = new UploadContext();
        this.context.setFile(file);
    }

    /**
     * 设置平台
     *
     * @param platform 平台
     * @return {@link UploadPretreatment }
     */
    public UploadPretreatment platform(String platform) {
        context.setPlatform(platform);
        return this;
    }

    /**
     * 设置存储桶
     *
     * @param bucket 存储桶
     * @return {@link UploadPretreatment }
     */
    public UploadPretreatment bucket(String bucket) {
        context.setBucket(bucket);
        return this;
    }

    /**
     * 设置路径
     *
     * @param path 路径
     * @return {@link UploadPretreatment }
     */
    public UploadPretreatment path(String path) {
        context.setPath(path);
        return this;
    }

    /**
     * 格式化文件名 - 不传则使用全局格式化
     *
     * @param fileName 文件名
     * @return {@link UploadPretreatment }
     */
    public UploadPretreatment fileName(String fileName) {
        context.setFormatFileName(fileName);
        return this;
    }

    /**
     * 添加元数据
     */
    public UploadPretreatment metadata(String key, String value) {
        context.getMetadata().put(key, value);
        return this;
    }

    /**
     * 添加处理器
     *
     * @param processor 处理器
     * @return {@link UploadPretreatment }
     */
    public UploadPretreatment processor(FileProcessor processor) {
        processors.add(processor);
        return this;
    }

    /**
     * 设置缩略图
     *
     * @param width  宽度
     * @param height 高度
     * @return {@link UploadPretreatment }
     */
    public UploadPretreatment thumbnail(int width, int height) {
        context.setGenerateThumbnail(true);
        context.setThumbnailSize(new ThumbnailSize(width, height));
        return this;
    }

    /**
     * 设置进度监听器
     */
    public UploadPretreatment onProgress(UploadProgressListener listener) {
        this.progressListener = listener;
        return this;
    }

    /**
     * 设置简单的进度监听（只关心百分比）
     */
    public UploadPretreatment onProgress(Consumer<Integer> progressConsumer) {
        this.progressListener =
            (bytesRead, totalBytes, percentage) -> progressConsumer.accept(percentage);
        return this;
    }

    /**
     * 执行上传
     *
     * @return {@link FileInfo }
     */
    public FileInfo upload() {

        // 添加文件处理器
        for (FileProcessor processor : processors) {
            storageService.addProcessor(processor);
        }

        // 设置进度监听器
        if (progressListener != null) {
            storageService.onProgress(progressListener);
        }

        // 如果没有设置平台，则获取默认平台 (延迟获取默认存储平台)
        if (StrUtil.isBlank(context.getPlatform())) {
            context.setPlatform(storageService.getDefaultPlatform());
        }

        // 执行上传
        return storageService.upload(context);
    }

}
