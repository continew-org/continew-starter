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

package top.continew.starter.extension.tenant.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.extension.tenant.TenantDataSourceHandler;
import top.continew.starter.extension.tenant.aop.TenantIgnoreAspect;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.interceptor.TenantInterceptor;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import javax.sql.DataSource;

/**
 * 租户自动配置契约测试。
 *
 * <p>固化装配契约：模块开关（continew-starter.tenant.enabled）默认开启、动态数据源依赖缺失
 * 时仅跳过数据源级隔离 Bean（行级隔离不受影响）、行级处理器用户实现 fallback、代表性配置
 * 属性绑定。TenantProvider 为强制前置以 Mockito mock 提供；数据源级隔离前置分别以 mock
 * DynamicRoutingDataSource、DefaultDataSourceCreator 及 hutool SpringUtil 满足。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class))
        .withBean(SpringUtil.class, SpringUtil::new)
        .withBean(TenantProvider.class, () -> mock(TenantProvider.class))
        .withBean(DataSource.class, () -> mock(DynamicRoutingDataSource.class))
        .withBean(DefaultDataSourceCreator.class, () -> mock(DefaultDataSourceCreator.class));

    /**
     * imports 注册完整性：包内全部自动配置类均已登记进 AutoConfiguration.imports。
     */
    @Test
    void shouldRegisterAllAutoConfigurationClasses() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.extension.tenant")).isEmpty();
    }

    /**
     * 开关默认开启（matchIfMissing）：行级隔离、Web 拦截器、忽略切面与数据源级隔离均应注册。
     */
    @Test
    void shouldRegisterBeansByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantLineInnerInterceptor.class);
            assertThat(context).hasSingleBean(TenantInterceptor.class);
            assertThat(context).hasSingleBean(TenantIgnoreAspect.class);
            assertThat(context).hasSingleBean(TenantDataSourceHandler.class);
        });
    }

    /**
     * 开关关闭时整体跳过，不注册任何租户 Bean 且不影响应用启动。
     */
    @Test
    void shouldSkipSilentlyWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.tenant.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TenantLineInnerInterceptor.class);
                assertThat(context).doesNotHaveBean(TenantInterceptor.class);
            });
    }

    /**
     * 动态数据源类缺失时仅跳过数据源级隔离 Bean，行级隔离不受影响（可选依赖契约）。
     */
    @Test
    void shouldSkipDataSourceLevelBeansWhenDynamicDataSourceAbsent() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class))
            .withBean(TenantProvider.class, () -> mock(TenantProvider.class))
            .withClassLoader(new FilteredClassLoader(DynamicRoutingDataSource.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TenantDataSourceHandler.class);
                assertThat(context).hasSingleBean(TenantLineInnerInterceptor.class);
            });
    }

    /**
     * 用户已提供行级处理器时不再注册默认实现，容器保留用户实例（同一性）。
     */
    @Test
    void shouldKeepUserTenantLineHandlerWhenPresent() {
        TenantLineHandler userHandler = mock(TenantLineHandler.class);
        this.contextRunner.withBean(TenantLineHandler.class, () -> userHandler)
            .run(context -> assertThat(context.getBean(TenantLineHandler.class))
                .isSameAs(userHandler));
    }

    /**
     * 属性绑定：租户列名默认 tenant_id，覆盖值生效。
     */
    @Test
    void shouldBindTenantIdColumnProperty() {
        this.contextRunner.run(context -> assertThat(context.getBean(TenantProperties.class)
            .getTenantIdColumn()).isEqualTo("tenant_id"));
        this.contextRunner
            .withPropertyValues("continew-starter.tenant.tenant-id-column=tenant_no")
            .run(context -> assertThat(context.getBean(TenantProperties.class)
                .getTenantIdColumn()).isEqualTo("tenant_no"));
    }
}
