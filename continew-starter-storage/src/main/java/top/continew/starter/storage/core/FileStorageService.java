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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.storage.autoconfigure.properties.StorageProperties;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.common.util.BoundedInputStream;
import top.continew.starter.storage.common.util.ZipFileUtils;
import top.continew.starter.storage.domain.file.EnhancedMultipartFile;
import top.continew.starter.storage.domain.file.FileWrapper;
import top.continew.starter.storage.domain.file.InputStreamMultipartFile;
import top.continew.starter.storage.domain.file.ProgressAwareMultipartFile;
import top.continew.starter.storage.domain.file.TempFileMultipartFile;
import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.domain.model.req.MultipartUploadInitReq;
import top.continew.starter.storage.domain.model.req.ThumbnailInfo;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.FilePartInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.domain.model.resp.StrategyStatusResp;
import top.continew.starter.storage.domain.model.resp.ZipImportResp;
import top.continew.starter.storage.engine.StorageStrategyRouter;
import top.continew.starter.storage.operation.FileOperation;
import top.continew.starter.storage.operation.FilesOperation;
import top.continew.starter.storage.processor.preprocess.FileNameGenerator;
import top.continew.starter.storage.processor.preprocess.FilePathGenerator;
import top.continew.starter.storage.processor.preprocess.FileValidator;
import top.continew.starter.storage.processor.preprocess.ThumbnailProcessor;
import top.continew.starter.storage.processor.preprocess.UploadCompleteProcessor;
import top.continew.starter.storage.processor.preprocess.impl.DefaultFileNameGenerator;
import top.continew.starter.storage.processor.preprocess.impl.DefaultFilePathGenerator;
import top.continew.starter.storage.processor.preprocess.impl.DefaultThumbnailProcessor;
import top.continew.starter.storage.processor.preprocess.impl.FileSizeValidator;
import top.continew.starter.storage.processor.preprocess.impl.FileTypeValidator;
import top.continew.starter.storage.processor.progress.ProgressInputStream;
import top.continew.starter.storage.processor.progress.ProgressTracker;
import top.continew.starter.storage.processor.progress.UploadProgressListener;
import top.continew.starter.storage.processor.registry.ProcessorRegistry;
import top.continew.starter.storage.service.FileProcessor;
import top.continew.starter.storage.service.FileRecorder;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 增强版文件存储服务
 * 支持链式调用和更多功能
 *
 * @author echo
 * @since 2.14.0
 */
public class FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    private final StorageStrategyRouter router;
    private final ProcessorRegistry processorRegistry;
    private final FileRecorder fileRecorder;
    private final StorageProperties properties;
    private final ThreadLocal<List<FileProcessor>> tempProcessors =
        ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<UploadProgressListener> progressListener = new ThreadLocal<>();

    public FileStorageService(StorageStrategyRouter router,
        ProcessorRegistry processorRegistry,
        FileRecorder fileRecorder) {
        this(router, processorRegistry, fileRecorder, new StorageProperties());
    }

    public FileStorageService(StorageStrategyRouter router,
        ProcessorRegistry processorRegistry,
        FileRecorder fileRecorder,
        StorageProperties properties) {
        this.router = router;
        this.processorRegistry = processorRegistry;
        this.fileRecorder = fileRecorder;
        this.properties = properties;
    }

    /**
     * 获取默认存储平台
     *
     * @return 默认存储平台编码
     */
    public String getDefaultPlatform() {
        return router.getDefaultStorage();
    }

    /**
     * 获取处理器注册表
     *
     * @return 处理器注册表
     */
    public ProcessorRegistry getProcessorRegistry() {
        return processorRegistry;
    }

    /**
     * MultipartFile 直接上传
     *
     * @param file 文件
     * @return 上传预处理
     */
    public UploadPretreatment of(MultipartFile file) {
        return createPretreatment(file, null);
    }

    /**
     * MultipartFile 指定平台
     *
     * @param file     文件
     * @param platform 平台编码
     * @return 上传预处理
     */
    public UploadPretreatment of(MultipartFile file, String platform) {
        return createPretreatment(file, platform);
    }

    /**
     * byte[] 上传
     *
     * @param bytes       文件字节
     * @param filename    文件名
     * @param contentType 内容类型
     * @return 上传预处理
     */
    public UploadPretreatment of(byte[] bytes, String filename, String contentType) {
        return createPretreatment(bytes, filename, contentType);
    }

    /**
     * InputStream 上传
     *
     * @param inputStream 输入流
     * @param filename    文件名
     * @param contentType 内容类型
     * @return 上传预处理
     */
    public UploadPretreatment of(InputStream inputStream, String filename, String contentType) {
        return createPretreatment(inputStream, filename, contentType);
    }

    /**
     * 任意对象上传
     *
     * @param obj         文件来源对象
     * @param filename    文件名
     * @param contentType 内容类型
     * @return 上传预处理
     */
    public UploadPretreatment of(Object obj, String filename, String contentType) {
        return createPretreatment(obj, filename, contentType);
    }

    /**
     * 任意对象智能识别
     *
     * @param obj 文件来源对象
     * @return 上传预处理
     */
    public UploadPretreatment of(Object obj) {
        return createPretreatment(obj, null, null);
    }

    /**
     * 单文件链式操作
     *
     * @param bucket 存储桶
     * @param path   路径
     * @return 单文件链式操作
     */
    public FileOperation file(String bucket, String path) {
        return new FileOperation(this, bucket, path);
    }

    /**
     * 多文件链式操作
     *
     * @param bucket 存储桶
     * @param paths  路径列表（调用方列表会被快照，后续外部变更不影响本次操作）
     * @return 多文件链式操作
     */
    public FilesOperation files(String bucket, List<String> paths) {
        return new FilesOperation(this, bucket, paths);
    }

    /**
     * 创建预处理
     *
     * @param file     文件
     * @param platform 站台
     * @return {@link UploadPretreatment }
     */
    private UploadPretreatment createPretreatment(MultipartFile file, String platform) {
        UploadPretreatment pretreatment = new UploadPretreatment(this, file);
        return platform != null ? pretreatment.platform(platform) : pretreatment;
    }

    /**
     * 创建预处理
     *
     * @param source      来源
     * @param filename    文件名
     * @param contentType 内容类型
     * @return {@link UploadPretreatment }
     */
    private UploadPretreatment createPretreatment(Object source, String filename,
        String contentType) {
        FileWrapper wrapper = (filename != null || contentType != null)
            ? FileWrapper.of(source, filename, contentType)
            : FileWrapper.of(source);

        return createPretreatment(wrapper.toMultipartFile(), null);
    }

    /**
     * 添加处理器
     *
     * @param processor 文件处理器
     */
    public void addProcessor(FileProcessor processor) {
        tempProcessors.get().add(processor);
    }

    /**
     * 设置进度监听器
     *
     * @param listener 进度监听器
     */
    public void onProgress(UploadProgressListener listener) {
        progressListener.set(listener);
    }

    /**
     * 执行上传
     *
     * @param context 上传上下文
     * @return 文件信息
     */
    public FileInfo upload(UploadContext context) {
        String platform = context.getPlatform();
        try {
            // 初始化处理器和监听器
            List<FileProcessor> customProcessors = tempProcessors.get();
            UploadProgressListener listener = progressListener.get();

            // 设置进度监听器到上下文
            if (listener != null) {
                context.setProgressListener(listener);
            }

            // 包装文件
            prepareFile(context, listener);

            // 流式内容的读取上限：取启用的文件大小校验器最小上限，无校验器则不限
            long streamLimit = unwrapStreamFile(context.getFile()) == null ? Long.MAX_VALUE
                : resolveStreamLimit(platform, context, customProcessors);

            InputStreamMultipartFile streamFile = unwrapStreamFile(context.getFile());
            // 存在需要读取内容的自定义处理器时先限量物化，保证验证拿到可复读文件
            if (streamFile != null
                && hasContentReadingProcessors(platform, context, customProcessors)) {
                context.setFile(
                    materializeForRepeatableRead(streamFile, listener, streamLimit));
            }

            // 1. 执行文件验证（验证阶段）
            setFileReadPhase(context.getFile(), ProgressAwareMultipartFile.ReadPhase.VALIDATION);
            executeValidation(context, platform, customProcessors);

            // 2. 处理文件名和路径生成
            generateFileNameIfEmpty(context, platform, customProcessors);
            generateFilePathIfEmpty(context, platform, customProcessors);

            // 3. 设置默认bucket
            if (StrUtil.isBlank(context.getBucket())) {
                context.setBucket(getDefaultBucket(platform));
            }

            // 4. 准备缩略图处理
            ThumbnailProcessor thumbnailProcessor =
                prepareThumbnail(context, platform, customProcessors);

            // 缩略图需二次读取：不可复读的流式文件先限量物化为临时文件再上传
            if (context.isGenerateThumbnail()) {
                InputStreamMultipartFile streamFileForThumbnail =
                    unwrapStreamFile(context.getFile());
                if (streamFileForThumbnail != null) {
                    context.setFile(materializeForRepeatableRead(streamFileForThumbnail,
                        listener, streamLimit));
                }
            }

            // 5. 执行实际上传
            setFileReadPhase(context.getFile(), ProgressAwareMultipartFile.ReadPhase.UPLOAD);
            upload(platform, context.getBucket(), context.getFullPath(), context.getFile(),
                streamLimit);

            // 7. 构建文件信息
            FileInfo fileInfo = buildFileInfo(platform, context);

            // 8. 处理缩略图生成（缩略图阶段）
            if (thumbnailProcessor != null && context.isGenerateThumbnail()) {
                setFileReadPhase(context.getFile(), ProgressAwareMultipartFile.ReadPhase.THUMBNAIL);
                processThumbnail(fileInfo, thumbnailProcessor, context);
            }

            // 9. 保存文件记录
            if (fileRecorder != null) {
                fileRecorder.save(fileInfo);
            }

            // 10. 触发完成事件
            triggerCompleteEvent(fileInfo, context, platform, customProcessors);

            return fileInfo;

        } finally {
            cleanup(context);
        }
    }

    /**
     * 上传
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @param file     文件
     */
    public void upload(String platform, String bucket, String path, MultipartFile file) {
        upload(platform, bucket, path, file, Long.MAX_VALUE);
    }

    /**
     * 上传（带流式内容读取上限）
     *
     * @param platform    平台编码
     * @param bucket      存储桶
     * @param path        路径
     * @param file        文件
     * @param streamLimit 流式内容读取上限（字节，Long.MAX_VALUE 表示不限）
     */
    private void upload(String platform, String bucket, String path, MultipartFile file,
        long streamLimit) {
        StorageStrategy strategy = router.route(platform);
        InputStreamMultipartFile streamFile = unwrapStreamFile(file);
        if (streamFile == null) {
            try {
                strategy.upload(bucket, path, file);
                ProgressTracker tracker = trackerOf(file);
                if (tracker != null) {
                    tracker.complete();
                }
            } catch (Exception e) {
                ProgressTracker tracker = trackerOf(file);
                if (tracker != null) {
                    tracker.error(e);
                }
                throw e;
            }
            return;
        }
        if (strategy.supportUploadStream()) {
            uploadStreamFile(strategy, bucket, path, streamFile, file, streamLimit);
            return;
        }
        // 第三方策略不支持流式上传：物化为临时文件回退到常规上传，保持兼容
        materializeAndUpload(strategy, bucket, path, streamFile, file, streamLimit);
    }

    /**
     * 流式文件直传：源流只经 {@link InputStreamMultipartFile#closeStream()} 关闭一次；
     * 上传成功显式 complete，失败显式 error，进度不随流关闭误报成功
     *
     * @param strategy    目标策略
     * @param bucket      存储桶
     * @param path        路径
     * @param streamFile  单次读流文件
     * @param file        原始包装文件（含进度监听包装）
     * @param streamLimit 流式内容读取上限
     */
    private void uploadStreamFile(StorageStrategy strategy, String bucket, String path,
        InputStreamMultipartFile streamFile, MultipartFile file, long streamLimit) {
        try {
            InputStream raw;
            try {
                raw = streamFile.getInputStream();
            } catch (IOException e) {
                throw new StorageException("流式上传失败: " + e.getMessage(), e);
            }
            // bounded 由 finally 中 streamFile.closeStream() 唯一关闭；TWR 会因
            // FilterInputStream.close() 二次关闭源流，且 close 异常会误报落盘后失败
            BoundedInputStream bounded =
                new BoundedInputStream(raw, streamLimit, true);
            InputStream in = withProgress(bounded, file);
            ProgressTracker tracker = trackerOf(in);
            try {
                strategy.uploadStream(bucket, path, in, streamFile.getContentType(),
                    streamFile.getSize());
                streamFile.setActualSize(bounded.getCount());
                if (tracker != null) {
                    tracker.complete();
                }
            } catch (Exception e) {
                streamFile.setActualSize(bounded.getCount());
                if (tracker != null) {
                    tracker.error(e);
                }
                if (e instanceof StorageException se) {
                    throw se;
                }
                throw new StorageException("流式上传失败: " + e.getMessage(), e);
            }
        } finally {
            try {
                streamFile.closeStream();
            } catch (IOException e) {
                LOGGER.warn("关闭上传源流失败", e);
            }
        }
    }

    /**
     * 将流式文件物化为临时文件后走常规上传，临时文件随用随删
     *
     * @param strategy    目标策略
     * @param bucket      存储桶
     * @param path        路径
     * @param streamFile  单次读流文件
     * @param file        原始包装文件
     * @param streamLimit 流式内容读取上限
     */
    private void materializeAndUpload(StorageStrategy strategy, String bucket, String path,
        InputStreamMultipartFile streamFile, MultipartFile file, long streamLimit) {
        TempFileMultipartFile tempFile = null;
        try {
            tempFile = materializeViaStream(strategy, bucket, path, streamFile, file,
                streamLimit);
        } finally {
            try {
                streamFile.closeStream();
            } catch (IOException e) {
                LOGGER.warn("关闭上传源流失败", e);
            }
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * 以限量流物化临时文件并完成常规上传，进度成功/失败显式上报
     *
     * @param strategy    目标策略
     * @param bucket      存储桶
     * @param path        路径
     * @param streamFile  单次读流文件
     * @param file        原始包装文件
     * @param streamLimit 流式内容读取上限
     * @return 已上传的临时文件（调用方负责删除）
     */
    private TempFileMultipartFile materializeViaStream(StorageStrategy strategy,
        String bucket, String path, InputStreamMultipartFile streamFile,
        MultipartFile file, long streamLimit) {
        // bounded 由调用方 finally 中 streamFile.closeStream() 唯一关闭，不在此 TWR
        BoundedInputStream bounded;
        try {
            bounded =
                new BoundedInputStream(streamFile.getInputStream(), streamLimit, true);
        } catch (IOException e) {
            throw new StorageException("流式上传物化失败: " + e.getMessage(), e);
        }
        InputStream in = withProgress(bounded, file);
        ProgressTracker tracker = trackerOf(in);
        return doUploadMaterialized(strategy, bucket, path, streamFile, bounded,
            tracker, in);
    }

    /**
     * 把限量流物化为临时文件并完成常规上传；上传失败时清理临时文件防泄漏
     *
     * @param strategy   目标策略
     * @param bucket     存储桶
     * @param path       路径
     * @param streamFile 单次读流文件
     * @param bounded    限量输入流
     * @param tracker    进度追踪器（可空）
     * @param in         已按需包装进度的限量流
     * @return 已上传的临时文件（调用方负责删除）
     */
    private TempFileMultipartFile doUploadMaterialized(StorageStrategy strategy,
        String bucket, String path, InputStreamMultipartFile streamFile,
        BoundedInputStream bounded, ProgressTracker tracker, InputStream in) {
        TempFileMultipartFile tempFile = null;
        try {
            tempFile = TempFileMultipartFile.materialize(streamFile.getName(),
                streamFile.getOriginalFilename(), streamFile.getContentType(), in);
            streamFile.setActualSize(bounded.getCount());
            strategy.upload(bucket, path, tempFile);
            if (tracker != null) {
                tracker.complete();
            }
            return tempFile;
        } catch (Exception e) {
            // 物化成功但上传失败：方法内清理，避免泄漏临时文件
            if (tempFile != null) {
                tempFile.delete();
            }
            streamFile.setActualSize(bounded.getCount());
            if (tracker != null) {
                tracker.error(e);
            }
            if (e instanceof StorageException se) {
                throw se;
            }
            if (e instanceof IOException ioe) {
                throw new StorageException("流式上传物化失败: " + ioe.getMessage(), ioe);
            }
            throw (RuntimeException) e;
        }
    }

    /**
     * 按需包装进度监听流
     *
     * @param in   内容输入流
     * @param file 原始包装文件
     * @return 包装后输入流
     */
    private InputStream withProgress(InputStream in, MultipartFile file) {
        if (file instanceof ProgressAwareMultipartFile aware
            && aware.getProgressTracker() != null) {
            return new ProgressInputStream(in, aware.getProgressTracker());
        }
        return in;
    }

    /**
     * 从包装流中取得进度追踪器
     *
     * @param in 内容输入流
     * @return 进度追踪器，无则 null
     */
    private ProgressTracker trackerOf(InputStream in) {
        return in instanceof ProgressInputStream pis ? pis.getTracker() : null;
    }

    /**
     * 从上传文件中取得进度追踪器
     *
     * @param file 上传文件
     * @return 进度追踪器，无则 null
     */
    private ProgressTracker trackerOf(MultipartFile file) {
        return file instanceof ProgressAwareMultipartFile aware ? aware.getProgressTracker()
            : null;
    }

    /**
     * 将不可复读的流式文件物化为临时文件（按 streamLimit 限量），并按需补回进度包装
     *
     * @param streamFile  已判空的流式文件
     * @param listener    进度监听器
     * @param streamLimit 流式内容读取上限
     * @return 可复读的 MultipartFile
     */
    private MultipartFile materializeForRepeatableRead(
        InputStreamMultipartFile streamFile, UploadProgressListener listener,
        long streamLimit) {
        try {
            InputStream raw = streamFile.getInputStream();
            // bounded 由 finally 中 streamFile.closeStream() 唯一关闭；TWR 会因
            // FilterInputStream.close() 二次关闭源流，且 close 异常会误报物化失败
            BoundedInputStream bounded =
                new BoundedInputStream(raw, streamLimit, true);
            TempFileMultipartFile tempFile = TempFileMultipartFile.materialize(streamFile
                .getName(), streamFile.getOriginalFilename(), streamFile.getContentType(),
                bounded);
            streamFile.setActualSize(bounded.getCount());
            if (listener != null) {
                return new ProgressAwareMultipartFile(tempFile, false, listener, true);
            }
            return tempFile;
        } catch (IOException e) {
            throw new StorageException("需要可复读内容，流物化失败: " + e.getMessage(), e);
        } finally {
            try {
                streamFile.closeStream();
            } catch (IOException e) {
                LOGGER.warn("关闭上传源流失败", e);
            }
        }
    }

    /**
     * 剥离包装层，取回不可复读的流式文件；非流式文件返回 null
     *
     * @param file 包装文件
     * @return 流式文件
     */
    private InputStreamMultipartFile unwrapStreamFile(MultipartFile file) {
        MultipartFile unwrapped = file;
        while (unwrapped instanceof EnhancedMultipartFile enhanced && enhanced.isWrapped()) {
            unwrapped = enhanced.getOriginalFile();
        }
        return unwrapped instanceof InputStreamMultipartFile streamFile ? streamFile : null;
    }

    /**
     * 准备文件
     *
     * @param context  上传上下文
     * @param listener 进度监听器
     */
    private void prepareFile(UploadContext context, UploadProgressListener listener) {
        MultipartFile file = context.getFile();

        // 如果已经是 ProgressAwareMultipartFile，不重复包装
        if (file instanceof ProgressAwareMultipartFile) {
            return;
        }

        // 单次读的流式文件不能进内存缓存，仅在有监听器时补进度包装
        if (file instanceof InputStreamMultipartFile) {
            if (listener != null) {
                context.setFile(new ProgressAwareMultipartFile(file, false, listener, true));
            }
            return;
        }

        // 判断是否需要缓存
        boolean needCache = true;

        // 如果有进度监听器，使用 ProgressAwareMultipartFile
        if (listener != null) {
            context.setFile(new ProgressAwareMultipartFile(file, needCache, listener, true));
        } else if (!(file instanceof EnhancedMultipartFile)) {
            // 否则只使用 EnhancedMultipartFile 提供缓存
            context.setFile(EnhancedMultipartFile.wrap(file, needCache));
        }
    }

    /**
     * 设置文件读取阶段
     *
     * @param file  文件
     * @param phase 读取阶段
     */
    private void setFileReadPhase(MultipartFile file, ProgressAwareMultipartFile.ReadPhase phase) {
        if (file instanceof ProgressAwareMultipartFile awareMultipartFile) {
            awareMultipartFile.setReadPhase(phase);
        }
    }

    /**
     * 执行文件验证
     *
     * @param context          上传上下文
     * @param platform         平台编码
     * @param customProcessors 自定义处理器
     */
    private void executeValidation(UploadContext context, String platform,
        List<FileProcessor> customProcessors) {
        List<FileValidator> validators =
            collectProcessors(customProcessors, FileValidator.class, platform, context);

        for (FileValidator validator : validators) {
            if (validator.support(context)) {
                validator.validate(context);
            }
        }
    }

    /**
     * 仅在文件名为空时生成文件名
     *
     * @param context          上传上下文
     * @param platform         平台编码
     * @param customProcessors 自定义处理器
     */
    private void generateFileNameIfEmpty(UploadContext context, String platform,
        List<FileProcessor> customProcessors) {
        // 如果已有文件名，直接返回
        if (StrUtil.isNotBlank(context.getFormatFileName())) {
            return;
        }

        FileNameGenerator nameGenerator =
            findFirstProcessor(customProcessors, FileNameGenerator.class, platform,
                context);

        if (nameGenerator != null && nameGenerator.support(context)) {
            context.setFormatFileName(nameGenerator.generate(context));
        }
    }

    /**
     * 仅在路径为空时生成路径
     *
     * @param context          上传上下文
     * @param platform         平台编码
     * @param customProcessors 自定义处理器
     */
    private void generateFilePathIfEmpty(UploadContext context, String platform,
        List<FileProcessor> customProcessors) {
        // 如果已有路径，直接返回
        if (StrUtil.isNotBlank(context.getPath())) {
            return;
        }

        FilePathGenerator pathGenerator =
            findFirstProcessor(customProcessors, FilePathGenerator.class, platform,
                context);

        if (pathGenerator != null && pathGenerator.support(context)) {
            context.setPath(pathGenerator.path(context));
        }
    }

    /**
     * 准备缩略图处理
     *
     * @param context          上传上下文
     * @param platform         平台编码
     * @param customProcessors 自定义处理器
     * @return 缩略图处理器
     */
    private ThumbnailProcessor prepareThumbnail(UploadContext context,
        String platform,
        List<FileProcessor> customProcessors) {
        ThumbnailProcessor thumbnailProcessor =
            findFirstProcessor(customProcessors, ThumbnailProcessor.class, platform,
                context);

        boolean needThumbnail = thumbnailProcessor != null && thumbnailProcessor.support(context);
        context.setGenerateThumbnail(needThumbnail);

        return thumbnailProcessor;
    }

    /**
     * 构建文件信息
     *
     * @param platform 平台编码
     * @param context  上传上下文
     * @return 文件信息
     */
    private FileInfo buildFileInfo(String platform, UploadContext context) {
        FileInfo fileInfo = getFileInfo(platform, context.getBucket(), context.getFullPath());
        fileInfo.setOriginalFileName(context.getFile().getOriginalFilename());

        // 长度未知的流式上传以实际读取字节数回填大小，保证 FileInfo.size 不为 -1
        InputStreamMultipartFile streamFile = unwrapStreamFile(context.getFile());
        if (streamFile != null && streamFile.getActualSize() >= 0
            && (fileInfo.getSize() == null || fileInfo.getSize() < 0)) {
            fileInfo.setSize(streamFile.getActualSize());
        }

        if (context.getMetadata() != null && !context.getMetadata().isEmpty()) {
            fileInfo.getMetadata().putAll(context.getMetadata());
        }

        return fileInfo;
    }

    /**
     * 触发上传完成事件
     *
     * @param fileInfo         文件信息
     * @param context          上传上下文
     * @param platform         平台编码
     * @param customProcessors 自定义处理器
     */
    private void triggerCompleteEvent(FileInfo fileInfo,
        UploadContext context,
        String platform,
        List<FileProcessor> customProcessors) {
        List<UploadCompleteProcessor> completeProcessors = collectProcessors(customProcessors,
            UploadCompleteProcessor.class, platform, context);

        for (UploadCompleteProcessor processor : completeProcessors) {
            if (processor.support(context)) {
                processor.onComplete(fileInfo);
            }
        }
    }

    /**
     * 收集指定类型的处理器
     *
     * @param <T>              处理器类型
     * @param customProcessors 自定义处理器
     * @param processorClass   处理器类
     * @param platform         平台编码
     * @param context          上传上下文
     * @return 匹配的处理器列表
     */
    private <T extends FileProcessor> List<T> collectProcessors(
        List<FileProcessor> customProcessors,
        Class<T> processorClass,
        String platform,
        UploadContext context) {

        List<T> processors = new ArrayList<>();

        // 添加自定义处理器
        if (customProcessors != null) {
            customProcessors.stream()
                .filter(processorClass::isInstance)
                .map(processorClass::cast)
                .forEach(processors::add);
        }

        // 添加注册的处理器
        processors.addAll(processorRegistry.getProcessors(processorClass, platform, context));

        return processors;
    }

    /**
     * 查找第一个匹配的处理器
     *
     * @param <T>              处理器类型
     * @param customProcessors 自定义处理器
     * @param processorClass   处理器类
     * @param platform         平台编码
     * @param context          上传上下文
     * @return 第一个匹配的处理器，未找到返回 null
     */
    private <T extends FileProcessor> T findFirstProcessor(List<FileProcessor> customProcessors,
        Class<T> processorClass,
        String platform,
        UploadContext context) {

        // 优先从自定义处理器中查找
        if (customProcessors != null) {
            Optional<T> customProcessor = customProcessors.stream()
                .filter(processorClass::isInstance)
                .map(processorClass::cast)
                .findFirst();

            if (customProcessor.isPresent()) {
                return customProcessor.get();
            }
        }

        // 从注册的处理器中获取
        return processorRegistry.getProcessor(processorClass, platform, context);
    }

    /**
     * 流式内容读取上限：取所有启用的大小校验器中最严格的上限，无校验器则不限制
     *
     * @param platform         平台编码
     * @param context          上传上下文
     * @param customProcessors 自定义处理器
     * @return 读取上限（字节）
     */
    private long resolveStreamLimit(String platform, UploadContext context,
        List<FileProcessor> customProcessors) {
        return collectProcessors(customProcessors, FileValidator.class, platform, context)
            .stream()
            .filter(FileSizeValidator.class::isInstance)
            .map(FileSizeValidator.class::cast)
            .mapToLong(FileSizeValidator::getMaxSize)
            .min()
            .orElse(Long.MAX_VALUE);
    }

    /**
     * 是否存在需要读取文件内容的处理器：除内置不读内容的类型外一律按"可能读"处理，
     * 单次读流式文件须先物化才可安全进入后续校验/生成/处理阶段
     *
     * @param platform         平台编码
     * @param context          上传上下文
     * @param customProcessors 自定义处理器
     * @return 是否存在内容读取型处理器
     */
    private boolean hasContentReadingProcessors(String platform, UploadContext context,
        List<FileProcessor> customProcessors) {
        List<FileProcessor> all = new ArrayList<>();
        if (customProcessors != null) {
            all.addAll(customProcessors);
        }
        all.addAll(processorRegistry.getProcessors(FileValidator.class, platform, context));
        all.addAll(processorRegistry.getProcessors(FileNameGenerator.class, platform, context));
        all.addAll(processorRegistry.getProcessors(FilePathGenerator.class, platform, context));
        all.addAll(processorRegistry.getProcessors(ThumbnailProcessor.class, platform, context));
        all.addAll(processorRegistry.getProcessors(UploadCompleteProcessor.class, platform,
            context));
        return all.stream().anyMatch(p -> !(p instanceof FileSizeValidator)
            && !(p instanceof FileTypeValidator)
            && !(p instanceof DefaultFileNameGenerator)
            && !(p instanceof DefaultFilePathGenerator)
            && !(p instanceof DefaultThumbnailProcessor));
    }

    /**
     * 处理缩略图
     *
     * @param fileInfo  文件信息
     * @param processor 缩略图处理器
     * @param context   上传上下文
     */
    private void processThumbnail(FileInfo fileInfo, ThumbnailProcessor processor,
        UploadContext context) {
        try {
            MultipartFile file = context.getFile();

            // 缩略图生成使用普通流
            try (InputStream is = file.getInputStream()) {
                ThumbnailInfo thumbnailInfo = processor.process(context, is);

                // 生成缩略图路径
                String filePrefix =
                    StrUtil.subBefore(fileInfo.getPath(), StringConstants.DOT, true);
                String thumbnailPath =
                    filePrefix + StorageConstant.THUMBNAIL_SUFFIX + thumbnailInfo.getFormat();
                String thumbnailFileName =
                    StrUtil.subAfter(thumbnailPath, StringConstants.SLASH, true);

                // 创建缩略图文件
                EnhancedMultipartFile thumbnailFile =
                    new EnhancedMultipartFile(thumbnailFileName, thumbnailFileName,
                        StorageConstant.CONTENT_TYPE_IMAGE + thumbnailInfo
                            .getFormat(),
                        thumbnailInfo.getData());

                // 上传缩略图
                upload(context.getPlatform(), context.getBucket(), thumbnailPath, thumbnailFile);

                fileInfo.setThumbnailPath(thumbnailPath);
                fileInfo.setThumbnailSize((long) thumbnailInfo.getData().length);
            }
        } catch (Exception e) {
            LOGGER.warn("缩略图处理失败: {}", e.getMessage());
        }
    }

    /**
     * 清理资源
     *
     * @param context 上传上下文
     */
    private void cleanup(UploadContext context) {
        // 清理临时处理器和监听器
        tempProcessors.remove();
        progressListener.remove();

        // 清理文件缓存
        cleanupFileCache(context.getFile());
    }

    /**
     * 清理文件缓存
     *
     * @param file 文件
     */
    private void cleanupFileCache(MultipartFile file) {
        if (file == null) {
            return;
        }
        if (file instanceof ProgressAwareMultipartFile awareFile) {
            awareFile.clearCache();
        } else if (file instanceof EnhancedMultipartFile enhancedFile) {
            enhancedFile.clearCache();
        }
        // 流式与物化文件的释放：关闭源流、删除临时文件（幂等）
        InputStreamMultipartFile unwrapped = unwrapStreamFile(file);
        if (unwrapped != null) {
            try {
                unwrapped.closeStream();
            } catch (IOException e) {
                LOGGER.warn("关闭上传源流失败", e);
            }
        }
        while (file instanceof EnhancedMultipartFile enhanced && enhanced.isWrapped()) {
            file = enhanced.getOriginalFile();
        }
        if (file instanceof TempFileMultipartFile tempFile) {
            tempFile.delete();
        }
    }

    /**
     * 初始化分片上传
     *
     * @param bucket      存储桶
     * @param platform    平台编码
     * @param path        路径
     * @param contentType 内容类型
     * @param metadata    元数据
     * @return 分片上传初始化结果
     */
    public MultipartInitResp initMultipartUpload(String bucket,
        String platform,
        String path,
        String contentType,
        Map<String, String> metadata) {
        bucket = bucket == null ? getDefaultBucket(platform) : bucket;
        MultipartInitResp result =
            router.route(platform).initMultipartUpload(bucket, path, contentType, metadata);

        // 记录文件信息
        if (fileRecorder != null) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(result.getFileId());
            fileInfo.setPlatform(platform);
            fileInfo.setBucket(bucket);
            fileInfo.setPath(path);
            fileInfo.setContentType(contentType);
            fileInfo.setMetadata(metadata != null ? new HashMap<>(metadata) : new HashMap<>());
            fileInfo.getMetadata().put("uploadId", result.getUploadId());
            fileInfo.getMetadata().put("status", "UPLOADING");
            fileRecorder.save(fileInfo);
        }
        return result;
    }

    /**
     * 初始化分片上传（包含断点续传会话恢复）
     *
     * @param req 分片上传初始化请求
     * @return 分片上传初始化结果
     */
    public MultipartInitResp initMultipartUpload(MultipartUploadInitReq req) {
        String platform = StrUtil.blankToDefault(req.getPlatform(), getDefaultPlatform());
        String bucket = StrUtil.blankToDefault(req.getBucket(), getDefaultBucket(platform));
        String parentPath = normalizeParentPath(req.getParentPath());
        String path = buildMultipartPath(parentPath, req.getFileName());
        // 空白值统一按未传入处理，避免 null 与空串被识别成不同的上传维度
        String originalFileName = normalizeOriginalFileName(req.getOriginalFileName());
        // fileName 仅决定存储路径，不参与复用键：旧调用方不传 originalFileName，且续传时可能重新生成 fileName；
        // 复用命中时返回的 fileName/path 以已存在会话为准（见 MultipartInitResp 的类注释）。
        String sessionKey = null;

        // 基于文件 MD5 复用已有上传会话
        if (fileRecorder != null && StrUtil.isNotBlank(req.getFileMd5())) {
            sessionKey = buildSessionKey(platform, bucket, req.getFileMd5(), originalFileName,
                parentPath);
            String cachedUploadId = fileRecorder.getUploadIdBySessionKey(sessionKey);
            if (StrUtil.isNotBlank(cachedUploadId)) {
                MultipartInitResp cachedSession = fileRecorder.getMultipartSession(cachedUploadId);
                // 原始文件名或上级目录不一致时不能复用：会话共用同一个存储对象与路径，复用会让后一次上传
                // 落进先一次上传的目录。未传原始文件名（旧调用方）时双方都为空，仍按旧行为复用；此时返回的
                // fileName/path 以已有会话为准，调用方必须使用返回的 path。并发初始化同一维度时可能各自创建
                // 会话，映射以最后写入者为准；清理时会按 uploadId 条件删除，避免误删其他会话的映射。
                if (cachedSession != null && StrUtil.equals(platform, cachedSession.getPlatform())
                    && StrUtil.equals(bucket, cachedSession.getBucket())
                    && StrUtil.equals(originalFileName, normalizeOriginalFileName(cachedSession
                        .getOriginalFileName()))
                    && StrUtil.equals(parentPath, normalizeParentPath(cachedSession
                        .getParentPath()))) {
                    List<FilePartInfo> fileParts = fileRecorder.getFileParts(cachedUploadId);
                    cachedSession.setUploadedPartNumbers(fileParts.stream()
                        .map(FilePartInfo::getPartNumber)
                        .collect(Collectors.toSet()));
                    return cachedSession;
                }
            }
        }

        MultipartInitResp result =
            initMultipartUpload(bucket, platform, path, req.getContentType(), req.getMetadata());
        result.setFileName(req.getFileName());
        result.setOriginalFileName(originalFileName);
        result.setFileMd5(req.getFileMd5());
        result.setFileSize(req.getFileSize());
        result.setContentType(req.getContentType());
        result.setParentPath(parentPath);
        result.setExtension(StrUtil.subAfter(req.getFileName(), StringConstants.DOT, true));
        result.setUploadedPartNumbers(new HashSet<>());

        if (fileRecorder != null) {
            fileRecorder.saveMultipartSession(result.getUploadId(), result, req.getMetadata());
            if (sessionKey != null) {
                fileRecorder.setSessionKeyMapping(sessionKey, result.getUploadId());
            }
        }
        return result;
    }

    /**
     * 上传分片
     *
     * @param platform   平台编码
     * @param bucket     存储桶
     * @param path       路径
     * @param uploadId   分片上传 ID
     * @param partNumber 分片编号
     * @param data       分片数据流
     * @return 分片上传结果
     */
    public MultipartUploadResp uploadPart(String platform,
        String bucket,
        String path,
        String uploadId,
        int partNumber,
        InputStream data) {
        MultipartUploadResp result =
            router.route(platform).uploadPart(bucket, path, uploadId, partNumber, data);

        // 记录分片信息
        if (fileRecorder != null && result.isSuccess()) {
            FilePartInfo partInfo = new FilePartInfo();
            partInfo.setUploadId(uploadId);
            partInfo.setBucket(bucket);
            partInfo.setPath(path);
            partInfo.setPartNumber(partNumber);
            partInfo.setPartETag(result.getPartETag());
            partInfo.setPartSize(result.getPartSize());
            partInfo.setStatus("SUCCESS");
            partInfo.setUploadTime(LocalDateTime.now(ZoneId.systemDefault()));
            fileRecorder.saveFilePart(partInfo);
        }

        return result;
    }

    /**
     * 完成分片上传
     *
     * @param platform    平台编码
     * @param bucket      存储桶
     * @param path        路径
     * @param uploadId    分片上传 ID
     * @param clientParts 客户端上报的分片列表（无记录时兜底）
     * @return 文件信息
     */
    public FileInfo completeMultipartUpload(String platform,
        String bucket,
        String path,
        String uploadId,
        List<MultipartUploadResp> clientParts) {
        // 从 FileRecorder 获取所有分片信息
        List<FilePartInfo> recordedParts = fileRecorder != null
            ? fileRecorder.getFileParts(uploadId)
            : new ArrayList<>();

        // 转换为 MultipartUploadResp
        List<MultipartUploadResp> parts = recordedParts.stream().map(partInfo -> {
            MultipartUploadResp resp = new MultipartUploadResp();
            resp.setPartNumber(partInfo.getPartNumber());
            resp.setPartETag(partInfo.getPartETag());
            resp.setPartSize(partInfo.getPartSize());
            resp.setSuccess("SUCCESS".equals(partInfo.getStatus()));
            return resp;
        }).toList();

        // 如果没有记录，使用客户端传入的分片信息
        if (parts.isEmpty() && clientParts != null) {
            parts = clientParts;
        }

        // 验证分片完整性
        validatePartsCompleteness(parts);

        // 获取策略，判断是否需要验证
        boolean needVerify = true;
        // 完成上传
        FileInfo fileInfo = router.route(platform).completeMultipartUpload(bucket, path, uploadId,
            parts, needVerify);

        // 更新文件记录
        if (fileRecorder != null) {
            if (fileInfo.getMetadata() == null) {
                fileInfo.setMetadata(new HashMap<>());
            }
            MultipartInitResp session = fileRecorder.getMultipartSession(uploadId);
            if (session != null) {
                // 优先使用会话中记录的原始文件名；未指定时沿用旧行为（会话文件名即展示名）
                fileInfo.setOriginalFileName(StrUtil
                    .blankToDefault(resolveDisplayName(session), fileInfo.getOriginalFileName()));
                fileInfo.getMetadata()
                    .put("fileMd5",
                        StrUtil.blankToDefault(session.getFileMd5(), StringConstants.EMPTY));
            }
            fileInfo.getMetadata().put("uploadId", uploadId);
            fileInfo.getMetadata().put("status", "COMPLETED");
            fileRecorder.update(fileInfo);

            cleanupMultipartSession(uploadId, session);
        }

        return fileInfo;
    }

    /**
     * 根据 uploadId 完成分片上传
     *
     * @param uploadId    分片上传 ID
     * @param clientParts 客户端上报的分片列表（无记录时兜底）
     * @return 文件信息
     */
    public FileInfo completeMultipartUpload(String uploadId,
        List<MultipartUploadResp> clientParts) {
        MultipartInitResp session = getMultipartSession(uploadId);
        if (session == null) {
            throw new StorageException("无效的 uploadId: " + uploadId);
        }
        return completeMultipartUpload(session.getPlatform(), session.getBucket(), session
            .getPath(), uploadId, clientParts);
    }

    /**
     * 取消分片上传
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @param uploadId 分片上传 ID
     */
    public void abortMultipartUpload(String platform, String bucket, String path, String uploadId) {
        router.route(platform).abortMultipartUpload(bucket, path, uploadId);

        // 删除相关记录
        if (fileRecorder != null) {
            cleanupMultipartSession(uploadId, fileRecorder.getMultipartSession(uploadId));
        }
    }

    /**
     * 根据 uploadId 取消分片上传
     *
     * @param uploadId 分片上传 ID
     */
    public void abortMultipartUpload(String uploadId) {
        MultipartInitResp session = getMultipartSession(uploadId);
        if (session == null) {
            if (fileRecorder != null) {
                cleanupMultipartSession(uploadId, null);
            }
            return;
        }
        abortMultipartUpload(session.getPlatform(), session.getBucket(), session.getPath(),
            uploadId);
    }

    /**
     * 验证分片完整性
     *
     * @param parts 分片列表
     */
    private void validatePartsCompleteness(List<MultipartUploadResp> parts) {
        if (parts.isEmpty()) {
            throw new StorageException("没有找到任何分片信息");
        }

        // 检查分片编号连续性
        List<Integer> partNumbers =
            parts.stream().map(MultipartUploadResp::getPartNumber).sorted().toList();

        for (int i = 0; i < partNumbers.size(); i++) {
            if (partNumbers.get(i) != i + 1) {
                throw new StorageException("分片编号不连续，缺失分片: " + (i + 1));
            }
        }

        // 检查是否所有分片都成功
        List<Integer> failedParts = parts.stream()
            .filter(part -> !part.isSuccess())
            .map(MultipartUploadResp::getPartNumber)
            .toList();

        if (!failedParts.isEmpty()) {
            throw new StorageException("存在失败的分片: " + failedParts);
        }
    }

    /**
     * 列出已上传的分片
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @param uploadId 分片上传 ID
     * @return 已上传的分片列表
     */
    public List<MultipartUploadResp> listParts(String platform, String bucket, String path,
        String uploadId) {
        return router.route(platform).listParts(bucket, path, uploadId);
    }

    /**
     * 获取分片会话
     *
     * @param uploadId 分片上传 ID
     * @return 分片上传初始化信息，不存在返回 null
     */
    public MultipartInitResp getMultipartSession(String uploadId) {
        return fileRecorder == null ? null : fileRecorder.getMultipartSession(uploadId);
    }

    /**
     * 获取默认存储桶
     *
     * @param platform 平台编码
     * @return {@link String }
     */
    public String getDefaultBucket(String platform) {
        return router.route(platform).defaultBucket();
    }

    /**
     * 下载文件
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @return 文件输入流
     */
    public InputStream download(String platform, String bucket, String path) {
        return router.route(platform).download(bucket, path);
    }

    /**
     * 使用默认存储下载
     *
     * @param bucket 存储桶
     * @param path   路径
     * @return 文件输入流
     */
    public InputStream download(String bucket, String path) {
        return download(getDefaultPlatform(), bucket, path);
    }

    /**
     * 字节范围下载（闭区间 [start, end]）
     *
     * <p>
     * 本方法只校验范围参数本身，文件大小解析、末端钳制与不存在/越界处理由策略
     * 实现决定（避免服务层预查造成整文件读取）：本地策略按文件大小钳制末端并
     * 抛出越界异常；S3 按 Range 语义由服务端处理（end 越界自动截断至文件末尾，
     * start 越界或文件不存在返回错误）。返回流由调用方关闭。
     * </p>
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @param start    起始字节偏移（含，从 0 计）
     * @param end      结束字节偏移（含）
     * @return 范围内容输入流
     */
    public InputStream downloadRange(String platform, String bucket, String path, long start,
        long end) {
        if (start < 0 || end < start) {
            throw new StorageException("下载范围非法: start=" + start + ", end=" + end);
        }
        return router.route(platform).downloadRange(bucket, path, start, end);
    }

    /**
     * 字节范围下载到输出流（闭区间 [start, end]），输出流由调用方关闭
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @param out      目标输出流
     * @param start    起始字节偏移（含，从 0 计）
     * @param end      结束字节偏移（含）
     */
    public void writeRangeDownload(String platform, String bucket, String path, OutputStream out,
        long start, long end) {
        try (InputStream in = downloadRange(platform, bucket, path, start, end)) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new StorageException("范围下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量下载为 ZIP 流
     *
     * <p>
     * 走 {@link StorageStrategy#batchDownload(String, List)}，默认实现按流式生成：
     * 消费方读取时才逐个拉取源文件写入 ZIP 结构，内存以复制缓冲区为界，但 ZIP
     * 中央目录随条目数增长占用内存；条目名取对象路径，重名自动追加 " (n)" 后缀。
     * 第三方/装饰器策略可覆盖定制。返回流由调用方关闭，关闭即停止拉取源文件。
     * </p>
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param paths    路径列表
     * @return ZIP 格式输入流
     */
    public InputStream batchDownload(String platform, String bucket, List<String> paths) {
        return router.route(platform).batchDownload(bucket, paths);
    }

    /**
     * 批量下载为 ZIP 写入输出流，输出流由调用方关闭
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param paths    路径列表
     * @param out      目标输出流
     */
    public void writeBatchDownload(String platform, String bucket, List<String> paths,
        OutputStream out) {
        try (InputStream in = batchDownload(platform, bucket, paths)) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new StorageException("批量下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导入 ZIP 包到存储
     *
     * <p>
     * 流式解包：逐条上传，不做整包内存缓冲。条目名做目录穿越校验（空名、绝对路径、
     * Windows 驱动器路径、".." 段）；目录与非法名条目先经受预算的 closeEntry 排空
     * 再记失败继续，排空超限记失败并停止，同一条目只计一次。同名目标对象会被覆盖。
     * 解压侧在读取侧计量：所有途经字节按单条与全包两层硬限，读取请求钳到剩余
     * 额度外仅多探测 1 字节；任一层超限时把当前条目记失败并停止导入，此前已成功
     * 的对象保留、不做回滚。total 与明细一一对应，含因条目数上限被拒绝的条目。
     * </p>
     * <p>
     * 归档级错误（下一条目头部损坏、源流关闭失败等）不折算成条目失败，包装为
     * {@link StorageException} 抛出，已上传对象同样不回滚。
     * </p>
     *
     * @param platform   平台编码
     * @param bucket     存储桶
     * @param parentPath 导入后对象存放的父级路径（可空，为根）
     * @param zipSource  ZIP 输入流（由本方法关闭）
     * @return 导入结果（总数、成功、失败与逐条明细）
     * @throws StorageException 归档级读取失败或源流关闭失败时抛出
     */
    public ZipImportResp importZip(String platform, String bucket, String parentPath,
        InputStream zipSource) {
        ZipImportResp resp = new ZipImportResp();

        // zipSource 自进入方法即由本方法负责关闭，路由/父路径校验失败同样生效
        try (BudgetedZipInputStream zin =
            new BudgetedZipInputStream(zipSource, properties.getZipImportMaxEntryBytes(),
                properties.getZipImportMaxTotalBytes())) {
            StorageStrategy strategy = router.route(platform);
            // 父路径规范化后剥尾斜杠，避免与条目名拼出连续斜杠
            String normalizedParent = StrUtil.isBlank(parentPath) ? StringConstants.EMPTY
                : StrUtil.removeSuffix(ZipFileUtils.normalizeEntryName(parentPath),
                    StringConstants.SLASH);
            long maxEntries = properties.getZipImportMaxEntries();
            boolean stopped = false;
            ZipEntry entry;
            while (!stopped && (entry = zin.getNextEntry()) != null) {
                String entryName = entry.getName();
                resp.setTotal(resp.getTotal() + 1);
                if (resp.getTotal() > maxEntries) {
                    // 首条被拒条目计入 total 并留痕，但不读取其内容
                    resp.addEntry(entryName, false, 0L, "超过条目数限制: " + maxEntries);
                    break;
                }

                ZipEntryCheck check = checkEntry(entry);
                if (!check.importable()) {
                    // 目录或非法名条目：排空成功记原因、排空失败记该条一次失败并停止
                    stopped = !drainSkippedEntry(zin, resp, entryName, check.skipReason());
                    continue;
                }
                String normalizedName = check.name();
                String targetPath = StrUtil.isBlank(normalizedParent) ? normalizedName
                    : normalizedParent + StringConstants.SLASH + normalizedName;

                // 读取侧已按单条/全包硬限计量，物化到临时文件后才可能触碰目标对象；
                // 任一层超限或读取失败都记该条失败并停止导入，不触碰既有对象
                TempFileMultipartFile temp = materializeZipEntry(zin, resp, entryName,
                    normalizedName);
                if (temp == null) {
                    stopped = true;
                    continue;
                }
                try {
                    uploadZipEntry(strategy, bucket, targetPath, temp, resp, entryName);
                } finally {
                    temp.delete();
                }
            }
        } catch (IOException e) {
            // 下一条目头部损坏或源流关闭失败属归档级错误，不归到上一条目
            throw new StorageException("ZIP 导入读取失败: " + e.getMessage(), e);
        }
        return resp;
    }

    /**
     * 校验条目：目录或非法名记为不可导入并返回原因，可导入返回规范化名
     *
     * @param entry ZIP 条目
     * @return 校验结果（规范化名或拒绝原因）
     */
    private ZipEntryCheck checkEntry(ZipEntry entry) {
        if (entry.isDirectory()) {
            return new ZipEntryCheck(null, "目录条目不导入");
        }
        try {
            return new ZipEntryCheck(ZipFileUtils.normalizeEntryName(entry.getName()),
                null);
        } catch (StorageException e) {
            return new ZipEntryCheck(null, e.getMessage());
        }
    }

    /**
     * 条目校验结果
     *
     * @param name       规范化条目名（可导入时非 null）
     * @param skipReason 拒绝原因（可导入时为 null）
     */
    private record ZipEntryCheck(String name, String skipReason) {

        boolean importable() {
            return name != null;
        }
    }

    /**
     * 经计量 read 排空被跳过的条目；成功记拒绝原因，失败记该条一次失败
     *
     * @param zin        计量 ZIP 输入流
     * @param resp       导入结果
     * @param entryName  条目名
     * @param skipReason 拒绝原因
     * @return 排空成功；排空失败时记该条一次失败并返回 false
     */
    private boolean drainSkippedEntry(BudgetedZipInputStream zin, ZipImportResp resp,
        String entryName, String skipReason) {
        try {
            zin.closeEntry();
            resp.addEntry(entryName, false, 0L, skipReason);
            return true;
        } catch (IOException e) {
            resp.addEntry(entryName, false, 0L, e.getMessage());
            return false;
        }
    }

    /**
     * 物化单条 ZIP 条目到临时文件；读取超限或失败记该条一次失败并返回 null
     *
     * @param zin            计量 ZIP 输入流
     * @param resp           导入结果
     * @param entryName      原始条目名
     * @param normalizedName 规范化条目名
     * @return 临时文件；失败返回 null
     */
    private TempFileMultipartFile materializeZipEntry(BudgetedZipInputStream zin,
        ZipImportResp resp, String entryName, String normalizedName) {
        try {
            return TempFileMultipartFile.materialize(entryName, normalizedName,
                guessZipEntryContentType(normalizedName), zin);
        } catch (IOException e) {
            resp.addEntry(entryName, false, 0L, e.getMessage());
            return null;
        }
    }

    /**
     * 上传单条物化条目并记录结果；同名目标对象覆盖写入，已存在对象不特殊保护。
     * 成功/失败与消息分离：无异常消息的失败也如实计为失败
     *
     * @param strategy   目标策略
     * @param bucket     存储桶
     * @param targetPath 目标路径
     * @param temp       物化临时文件
     * @param resp       导入结果
     * @param entryName  原始条目名
     */
    private void uploadZipEntry(StorageStrategy strategy, String bucket,
        String targetPath, TempFileMultipartFile temp, ZipImportResp resp,
        String entryName) {
        try {
            strategy.upload(bucket, targetPath, temp);
            resp.addEntry(entryName, true, temp.getSize(), null);
        } catch (Exception e) {
            resp.addEntry(entryName, false, temp.getSize(), e.getMessage());
        }
    }

    /**
     * 按读取侧计量的 ZIP 输入流：单条与全包两层硬限，读取请求钳到剩余额度外
     * 仅多探测 1 字节判定超限。JDK 的 closeEntry 排空同样走 read，因此目录、
     * 非法名、STORED 与 DEFLATED 条目一致受限；close 只关闭源流，不做排空。
     */
    private static class BudgetedZipInputStream extends ZipInputStream {

        private final long entryLimit;
        private final long totalLimit;
        private long entryRead;
        private long totalRead;

        /**
         * 构造预算统计流
         *
         * @param in         源输入流（关闭本流即关闭源）
         * @param entryLimit 单条解压字节上限
         * @param totalLimit 全包解压字节上限
         */
        BudgetedZipInputStream(InputStream in, long entryLimit, long totalLimit) {
            super(in);
            this.entryLimit = entryLimit;
            this.totalLimit = totalLimit;
        }

        @Override
        public ZipEntry getNextEntry() throws IOException {
            // super 先 closeEntry 排空上一条目（走计量 read），返回后重置单条计数
            ZipEntry entry = super.getNextEntry();
            this.entryRead = 0;
            return entry;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            // 剩余额度按减法计，防溢出；额度外仅多探测 1 字节判定超限
            long remaining = Math.min(entryLimit - entryRead, totalLimit - totalRead);
            int toRead = remaining >= len ? len : (int) (remaining + 1L);
            int n = super.read(b, off, toRead);
            if (n <= 0) {
                return n;
            }
            if (n > entryLimit - entryRead) {
                throw new IOException("ZIP 条目超过单条解压大小限制: " + entryLimit + " 字节");
            }
            if (n > totalLimit - totalRead) {
                throw new IOException("ZIP 导入超过总解压大小限制: " + totalLimit + " 字节");
            }
            this.entryRead += n;
            this.totalRead += n;
            return n;
        }
    }

    /**
     * 按条目名推断内容类型
     *
     * @param entryName ZIP 条目名
     * @return 内容类型，未知返回 application/octet-stream
     */
    private String guessZipEntryContentType(String entryName) {
        String type = URLConnection.guessContentTypeFromName(entryName);
        return type == null ? "application/octet-stream" : type;
    }

    /**
     * 删除文件
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     */
    public void delete(String platform, String bucket, String path) {
        router.route(platform).delete(bucket, path);
        if (fileRecorder != null) {
            fileRecorder.delete(platform, path);
        }
    }

    /**
     * 删除文件
     *
     * @param info 信息
     */
    public void delete(FileInfo info) {
        router.route(info.getPlatform()).delete(info.getBucket(), info.getFullPath());
        if (fileRecorder != null) {
            fileRecorder.delete(info.getPlatform(),
                StrUtil.blankToDefault(info.getPath(), info.getFullPath()));
        }
    }

    /**
     * 根据 URL 删除文件（依赖 FileRecorder 实现 URL 映射）
     *
     * @param url 文件 URL
     */
    public void delete(String url) {
        if (fileRecorder == null) {
            throw new StorageException("未配置 FileRecorder，无法根据 URL 删除文件");
        }
        FileInfo info = fileRecorder.getByUrl(url);
        if (info == null) {
            return;
        }
        delete(info);
    }

    /**
     * 批量删除
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param paths    路径列表
     */
    public void batchDelete(String platform, String bucket, List<String> paths) {
        router.route(platform).batchDelete(bucket, paths);
    }

    /**
     * 检查文件是否存在
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @return 是否存在
     */
    public boolean exists(String platform, String bucket, String path) {
        return router.route(platform).exists(bucket, path);
    }

    /**
     * 检查策略是否存在
     *
     * @param platform 平台编码
     * @return 是否存在
     */
    public boolean exists(String platform) {
        return router.getAllPlatform().contains(platform);
    }

    /**
     * 获取文件信息
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param path     路径
     * @return 文件信息
     */
    public FileInfo getFileInfo(String platform, String bucket, String path) {
        return router.route(platform).getFileInfo(bucket, path);
    }

    /**
     * 列出文件
     *
     * @param platform 平台编码
     * @param bucket   存储桶
     * @param prefix   路径前缀
     * @param maxKeys  最大返回条数
     * @return 文件信息列表
     */
    public List<FileInfo> list(String platform, String bucket, String prefix, int maxKeys) {
        return router.route(platform).list(bucket, prefix, maxKeys);
    }

    /**
     * 复制文件
     *
     * @param platform     平台编码
     * @param sourceBucket 源存储桶
     * @param targetBucket 目标存储桶
     * @param sourcePath   源路径
     * @param targetPath   目标路径
     */
    public void copy(String platform, String sourceBucket, String targetBucket, String sourcePath,
        String targetPath) {
        router.route(platform).copy(sourceBucket, targetBucket, sourcePath, targetPath);
    }

    /**
     * 移动文件
     *
     * @param platform     平台编码
     * @param sourceBucket 源存储桶
     * @param targetBucket 目标存储桶
     * @param sourcePath   源路径
     * @param targetPath   目标路径
     */
    public void move(String platform, String sourceBucket, String targetBucket, String sourcePath,
        String targetPath) {
        router.route(platform).move(sourceBucket, targetBucket, sourcePath, targetPath);
    }

    /**
     * 生成预签名URL
     *
     * @param bucket        存储桶
     * @param path          路径
     * @param expireSeconds 过期时间（秒）
     * @return 预签名 URL
     */
    public String generatePresignedUrl(String bucket, String path, long expireSeconds) {
        return generatePresignedUrl(getDefaultPlatform(), bucket, path, expireSeconds);
    }

    /**
     * 生成预签名URL
     *
     * @param platform      平台编码
     * @param bucket        存储桶
     * @param path          路径
     * @param expireSeconds 过期时间（秒）
     * @return 预签名 URL
     */
    public String generatePresignedUrl(String platform, String bucket, String path,
        long expireSeconds) {
        return router.route(platform).generatePresignedUrl(bucket, path, expireSeconds);
    }

    /**
     * 生成上传预签名URL
     *
     * @param bucket        存储桶
     * @param path          路径
     * @param expireSeconds 过期时间（秒）
     * @return 上传预签名 URL
     */
    public String generateUploadPresignedUrl(String bucket, String path, long expireSeconds) {
        return generateUploadPresignedUrl(getDefaultPlatform(), bucket, path, expireSeconds);
    }

    /**
     * 生成上传预签名URL
     *
     * @param platform      平台编码
     * @param bucket        存储桶
     * @param path          路径
     * @param expireSeconds 过期时间（秒）
     * @return 上传预签名 URL
     */
    public String generateUploadPresignedUrl(String platform, String bucket, String path,
        long expireSeconds) {
        return router.route(platform).generateUploadPresignedUrl(bucket, path, expireSeconds);
    }

    /**
     * 动态注册存储策略
     *
     * @param <T>      策略类型
     * @param strategy 存储策略实例
     */
    public <T extends StorageStrategy> void register(T strategy) {
        router.registerDynamic(strategy);
    }

    /**
     * 加载动态默认存储
     *
     * @param platform 平台编码
     */
    public void defaultStorage(String platform) {
        router.registerDynamicDefaultStorage(platform);
    }

    /**
     * 卸载动态注册的策略
     *
     * @param platform 平台编码
     * @return 是否卸载成功
     */
    public boolean unload(String platform) {
        if (!router.isDynamic(platform)) {
            throw new StorageException("只能卸载动态注册的策略: " + platform);
        }
        return router.unloadDynamic(platform);
    }

    /**
     * 获取所有可用策略代码
     *
     * @return 策略编码集合
     */
    public Set<String> getAvailablePlatform() {
        return router.getAllPlatform();
    }

    /**
     * 检查是否为动态注册的策略
     *
     * @param platform 平台编码
     * @return 是否动态注册
     */
    public boolean isDynamic(String platform) {
        return router.isDynamic(platform);
    }

    /**
     * 检查是否为配置文件策略
     *
     * @param platform 平台编码
     * @return 是否来自配置文件
     */
    public boolean isFromConfig(String platform) {
        return router.isFromConfig(platform);
    }

    /**
     * 获取策略详细信息
     *
     * @return 各策略的状态信息（平台编码 -> 状态）
     */
    public Map<String, StrategyStatusResp> getStrategyStatus() {
        return router.getFullStrategyStatus();
    }

    /**
     * 获取当前生效的策略信息
     *
     * @return 生效策略信息
     */
    public Map<String, String> getActiveStrategyInfo() {
        return router.getActiveStrategyInfo();
    }

    private void cleanupMultipartSession(String uploadId, MultipartInitResp session) {
        fileRecorder.deleteFileParts(uploadId);
        fileRecorder.deleteMultipartSession(uploadId);
        if (session != null && StrUtil.isNotBlank(session.getFileMd5())) {
            String sessionKey = buildSessionKey(session.getPlatform(), session.getBucket(), session
                .getFileMd5(), session.getOriginalFileName(), session.getParentPath());
            // 由记录器做「键 + uploadId」条件删除：映射可能已被并发初始化的新会话覆盖，无条件删除会误删他人的映射
            fileRecorder.deleteSessionKeyMapping(sessionKey, uploadId);
        }
    }

    /**
     * 归一化原始文件名
     *
     * <p>
     * 空白值统一视为未传入（null），避免 null 与空串在会话键、复用校验之间不一致。
     * </p>
     *
     * @param originalFileName 原始文件名
     * @return 归一化后的原始文件名
     */
    private static String normalizeOriginalFileName(String originalFileName) {
        return StrUtil.isBlank(originalFileName) ? null : originalFileName;
    }

    /**
     * 解析会话展示名
     *
     * <p>
     * 优先使用会话中记录的原始文件名，未提供时回退为存储文件名。
     * </p>
     *
     * @param session 分片上传会话
     * @return 展示名
     */
    private static String resolveDisplayName(MultipartInitResp session) {
        return StrUtil.blankToDefault(session.getOriginalFileName(), session.getFileName());
    }

    /**
     * 构建会话复用键
     *
     * <p>
     * 键包含存储平台、存储桶、文件 MD5、原始文件名与上级目录，取 SHA-256 摘要以控制长度；空值按空串参与计算，
     * 上级目录按同一规则归一，保证写入与删除时键一致。存储文件名不参与键（续传时可能重新生成）。
     * </p>
     *
     * @param platform         存储平台
     * @param bucket           存储桶
     * @param fileMd5          文件 MD5
     * @param originalFileName 原始文件名（可能为空）
     * @param parentPath       上级目录
     * @return 会话复用键
     */
    static String buildSessionKey(String platform,
        String bucket,
        String fileMd5,
        String originalFileName,
        String parentPath) {
        StringBuilder dimension = new StringBuilder();
        appendKeyPart(dimension, platform);
        appendKeyPart(dimension, bucket);
        appendKeyPart(dimension, fileMd5);
        appendKeyPart(dimension, normalizeOriginalFileName(originalFileName));
        appendKeyPart(dimension, normalizeParentPath(parentPath));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(dimension.toString()
                .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    /**
     * 追加会话复用键的组成部分
     *
     * <p>
     * 使用「长度 + 冒号 + 原值」拼接，避免字段本身包含分隔符时产生键冲突。
     * </p>
     *
     * @param builder 键构建器
     * @param value   字段值
     */
    private static void appendKeyPart(StringBuilder builder, String value) {
        String part = StrUtil.blankToDefault(value, StringConstants.EMPTY);
        builder.append(part.length()).append(StringConstants.COLON).append(part);
    }

    private static String normalizeParentPath(String parentPath) {
        if (StrUtil.isBlank(parentPath) || StringConstants.SLASH.equals(parentPath)) {
            return StringConstants.SLASH;
        }
        String normalized =
            parentPath.replace("\\", StringConstants.SLASH).replaceAll("/+", StringConstants.SLASH);
        return StringConstants.SLASH + StrUtil.removeSuffix(StrUtil
            .removePrefix(normalized, StringConstants.SLASH), StringConstants.SLASH);
    }

    private String buildMultipartPath(String parentPath, String fileName) {
        String normalizedName = StrUtil.removePrefix(fileName, StringConstants.SLASH);
        if (StringConstants.SLASH.equals(parentPath)) {
            return normalizedName;
        }
        return StrUtil.removePrefix(parentPath + StringConstants.SLASH + normalizedName,
            StringConstants.SLASH);
    }
}
