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

package top.continew.starter.storage.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import top.continew.starter.storage.annotation.PlatformProcessor;
import top.continew.starter.storage.autoconfigure.properties.StorageProperties;
import top.continew.starter.storage.core.FileStorageService;
import top.continew.starter.storage.engine.StorageDecoratorManager;
import top.continew.starter.storage.processor.registry.ProcessorRegistry;
import top.continew.starter.storage.processor.preprocess.*;
import top.continew.starter.storage.processor.preprocess.impl.*;
import top.continew.starter.storage.engine.StorageStrategyRegistrar;
import top.continew.starter.storage.engine.StorageStrategyRouter;
import top.continew.starter.storage.service.FileProcessor;
import top.continew.starter.storage.service.FileRecorder;
import top.continew.starter.storage.service.impl.DefaultFileRecorder;

import java.util.List;
import java.util.Map;

/**
 * 存储自动配置
 *
 * @author echo
 * @since 2.14.0
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@Import({OssStorageAutoConfiguration.class, LocalStorageAutoConfiguration.class})
public class StorageAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StorageAutoConfiguration.class);

    private final StorageProperties properties;
    private final ApplicationContext applicationContext;

    public StorageAutoConfiguration(StorageProperties properties,
        ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * 策略路由器
     *
     * @param registrars 注册
     * @return {@link StorageStrategyRouter }
     */
    @Bean
    public StorageStrategyRouter strategyRouter(List<StorageStrategyRegistrar> registrars,
        StorageDecoratorManager storageDecoratorManager) {
        return new StorageStrategyRouter(registrars, properties, storageDecoratorManager);
    }

    /**
     * 存储装饰器管理器
     *
     * @return {@link StorageDecoratorManager }
     */
    @Bean
    public StorageDecoratorManager storageDecoratorManager() {
        return new StorageDecoratorManager(applicationContext);
    }

    /**
     * 文件存储服务
     *
     * @param router            路由
     * @param processorRegistry 处理器注册表
     * @param fileRecorder      文件记录器
     * @return {@link FileStorageService }
     */
    @Bean
    public FileStorageService fileStorageService(StorageStrategyRouter router,
        ProcessorRegistry processorRegistry,
        FileRecorder fileRecorder) {
        return new FileStorageService(router, processorRegistry, fileRecorder);
    }

    /**
     * 文件记录器
     *
     * @return {@link FileRecorder }
     */
    @Bean
    @ConditionalOnMissingBean
    public FileRecorder fileRecorder() {
        return new DefaultFileRecorder();
    }

    /**
     * 处理器注册中心
     */
    @Bean
    public ProcessorRegistry processorRegistry() {
        ProcessorRegistry registry = new ProcessorRegistry();

        // 自动发现并注册所有 FileProcessor 实现
        Map<String, FileProcessor> processors =
            applicationContext.getBeansOfType(FileProcessor.class);
        processors.values().forEach(processor -> {
            // 检查是否有平台注解（兼容代理类）
            Class<?> targetClass = AopUtils.getTargetClass(processor);
            PlatformProcessor annotation =
                AnnotationUtils.findAnnotation(targetClass, PlatformProcessor.class);
            if (annotation != null) {
                for (String platform : annotation.platforms()) {
                    registry.register(processor, platform);
                }
            } else {
                // 注册为全局处理器
                registry.register(processor);
            }
        });
        return registry;
    }

    /**
     * 默认文件名生成器
     */
    @Bean
    @ConditionalOnMissingBean(FileNameGenerator.class)
    public FileNameGenerator defaultFileNameGenerator() {
        return new DefaultFileNameGenerator();
    }

    /**
     * 默认路径生成器
     */
    @Bean
    @ConditionalOnMissingBean(FilePathGenerator.class)
    public FilePathGenerator defaultFilePathGenerator() {
        return new DefaultFilePathGenerator();
    }

    /**
     * 默认缩略图处理器
     */
    @Bean
    @ConditionalOnMissingBean(ThumbnailProcessor.class)
    @ConditionalOnClass(name = "net.coobird.thumbnailator.Thumbnails")
    public ThumbnailProcessor defaultThumbnailProcessor() {
        return new DefaultThumbnailProcessor();
    }

    /**
     * 文件大小验证器
     */
    @Bean
    @ConditionalOnMissingBean(name = "fileSizeValidator")
    public FileValidator fileSizeValidator(MultipartProperties multipartProperties) {
        return new FileSizeValidator(multipartProperties);
    }

    /**
     * 文件类型验证器
     */
    @Bean
    @ConditionalOnMissingBean(name = "fileTypeValidator")
    public FileValidator fileTypeValidator() {
        return new FileTypeValidator();
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Storage' completed initialization.");
    }
}
