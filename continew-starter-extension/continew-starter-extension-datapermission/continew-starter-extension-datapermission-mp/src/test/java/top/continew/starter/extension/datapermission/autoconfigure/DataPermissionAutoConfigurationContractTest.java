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

package top.continew.starter.extension.datapermission.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 数据权限自动配置契约测试。
 *
 * <p>固化装配契约：模块开关（continew-starter.data-permission.enabled）默认开启、处理器链
 * 的用户实现 fallback、用户数据提供者为强制前置（默认实现直接抛异常，缺失时启动失败以暴露
 * 缺 Bean）。处理器与提供者均以 Mockito mock 提供，仅验证装配条件与依赖关系。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DataPermissionAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataPermissionAutoConfiguration.class))
        .withBean(DataPermissionUserDataProvider.class,
            () -> mock(DataPermissionUserDataProvider.class));

    /**
     * imports 注册完整性：包内全部自动配置类均已登记进 AutoConfiguration.imports。
     */
    @Test
    void shouldRegisterAllAutoConfigurationClasses() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.extension.datapermission")).isEmpty();
    }

    /**
     * 开关默认开启（matchIfMissing）：拦截器与默认处理器均应注册。
     */
    @Test
    void shouldRegisterBeansByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DataPermissionInterceptor.class);
            assertThat(context).hasSingleBean(DataPermissionHandler.class);
        });
    }

    /**
     * 用户数据提供者缺失时应启动失败暴露缺 Bean，而非静默注册不可用的处理器链。
     */
    @Test
    void shouldFailFastWhenUserDataProviderAbsent() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataPermissionAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(BeanCreationException.class);
            });
    }

    /**
     * 用户已提供处理器时不再注册默认处理器，容器保留用户实例（同一性）。
     */
    @Test
    void shouldKeepUserDataPermissionHandlerWhenPresent() {
        DataPermissionHandler userHandler = mock(DataPermissionHandler.class);
        this.contextRunner.withBean(DataPermissionHandler.class, () -> userHandler)
            .run(context -> assertThat(context.getBean(DataPermissionHandler.class))
                .isSameAs(userHandler));
    }

    /**
     * 用户已提供拦截器时不再注册默认拦截器，容器保留用户实例（同一性）。
     */
    @Test
    void shouldKeepUserDataPermissionInterceptorWhenPresent() {
        DataPermissionInterceptor userInterceptor = mock(DataPermissionInterceptor.class);
        this.contextRunner.withBean(DataPermissionInterceptor.class, () -> userInterceptor)
            .run(context -> assertThat(context.getBean(DataPermissionInterceptor.class))
                .isSameAs(userInterceptor));
    }

    /**
     * 开关关闭时整体跳过，不注册处理器链且不影响应用启动。
     */
    @Test
    void shouldSkipSilentlyWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.data-permission.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(DataPermissionInterceptor.class);
                assertThat(context).doesNotHaveBean(DataPermissionHandler.class);
            });
    }
}
