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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import net.coobird.thumbnailator.Thumbnails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.storage.autoconfigure.properties.StorageProperties;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.core.FileStorageService;
import top.continew.starter.storage.engine.StorageDecoratorManager;
import top.continew.starter.storage.engine.StorageStrategyRegistrar;
import top.continew.starter.storage.engine.StorageStrategyRouter;
import top.continew.starter.storage.processor.preprocess.ThumbnailProcessor;
import top.continew.starter.storage.service.FileRecorder;
import top.continew.starter.storage.strategy.impl.LocalStorageStrategy;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import java.nio.file.Path;

/**
 * 存储模块自动配置契约测试
 *
 * <p>固化 StorageAutoConfiguration 入口的契约：imports 登记完整性、组合装配、
 * 属性绑定与 {@code @ConditionalOnMissingBean} 扩展点回退。</p>
 *
 * <p>LocalStorageAutoConfiguration 与 OssStorageAutoConfiguration 为 @Configuration 类型，
 * 由入口 {@code @Import} 组合激活，不登记 imports 属有意设计；AutoConfigurationImportsAssert
 * 仅扫描 {@code @AutoConfiguration} 注解类型，不会将其误报为缺口。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class StorageAutoConfigurationContractTest {

    private final ApplicationContextRunner storageRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(StorageAutoConfiguration.class))
        // fileSizeValidator 依赖容器中的 MultipartProperties，由 Servlet 自动配置供给，此处等价补充
        .withBean(MultipartProperties.class, MultipartProperties::new);

    @TempDir
    private Path tempDir;

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源
     * （Local/Oss 两个 @Configuration 组合实现不参与扫描，见类注释）。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert.findUnregistered("top.continew.starter.storage")
            .isEmpty());
    }

    /**
     * 无装配开关，默认应注册路由器、装饰器管理器、存储服务与默认记录器，
     * 且 Local/Oss 两个策略注册器均由 @Import 组合进入容器。
     */
    @Test
    void storageBeansShouldRegisterByDefault() {
        this.storageRunner.run(context -> {
            assertThat(context).hasSingleBean(StorageProperties.class);
            assertThat(context).hasSingleBean(StorageStrategyRouter.class);
            assertThat(context).hasSingleBean(StorageDecoratorManager.class);
            assertThat(context).hasSingleBean(FileStorageService.class);
            assertThat(context).hasSingleBean(FileRecorder.class);
            // 组合语义：Local 与 Oss 两个策略注册器并存
            assertThat(context).getBeans(StorageStrategyRegistrar.class).hasSize(2);
        });
    }

    /**
     * 本地存储配置 enabled=true 时应将对应策略装配进路由器，可按平台编码路由命中。
     * LocalStorageStrategy 构造仅创建临时目录，bucket 指向 JUnit 临时目录，无外部副作用。
     */
    @Test
    void localStrategyShouldRouteWhenEnabled() {
        this.storageRunner
            .withPropertyValues("continew-starter.storage.local[0].enabled=true",
                "continew-starter.storage.local[0].platform=contract-local",
                "continew-starter.storage.local[0].bucket-name=" + this.tempDir)
            .run(context -> {
                assertThat(context).hasSingleBean(StorageStrategyRouter.class);
                StorageStrategyRouter router = context.getBean(StorageStrategyRouter.class);
                assertInstanceOf(LocalStorageStrategy.class, router.route("contract-local"));
            });
    }

    /**
     * 用户提供 FileRecorder 时应整体回退，容器中保持用户实例（@ConditionalOnMissingBean 语义）。
     */
    @Test
    void fileRecorderShouldFallbackToUserProvidedBean() {
        FileRecorder custom = mock(FileRecorder.class);
        this.storageRunner.withBean(FileRecorder.class, () -> custom).run(context -> {
            assertThat(context).hasSingleBean(FileRecorder.class);
            assertSame(custom, context.getBean(FileRecorder.class));
        });
    }

    /**
     * 默认平台默认值为 local，覆盖配置后应绑定到 StorageProperties。
     */
    @Test
    void defaultPlatformShouldBindWithDefaultAndOverride() {
        this.storageRunner.run(context -> assertEquals(StorageConstant.DEFAULT_STORAGE_PLATFORM,
            context.getBean(StorageProperties.class).getDefaultPlatform()));
        this.storageRunner.withPropertyValues("continew-starter.storage.default-platform=oss")
            .run(context -> assertEquals("oss", context.getBean(StorageProperties.class)
                .getDefaultPlatform()));
    }

    /**
     * 缩略图处理器按 @ConditionalOnClass(thumbnailator) 装配：classpath 缺失时应跳过。
     */
    @Test
    void thumbnailProcessorShouldFollowConditionalOnClass() {
        this.storageRunner.run(context -> assertThat(context)
            .hasSingleBean(ThumbnailProcessor.class));
        this.storageRunner.withClassLoader(new FilteredClassLoader(Thumbnails.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ThumbnailProcessor.class);
            });
    }
}
