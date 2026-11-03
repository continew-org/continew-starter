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

package top.continew.starter.data.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.data.datapermission.DataPermissionFilter;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * MyBatis Flex 自动配置契约测试。
 *
 * <p>固化装配契约：开关（mybatis-flex.extension.enabled）默认关闭须显式开启、
 * DataPermissionFilter 为字段注入的硬前置（缺失时启动失败而非静默跳过）、代表性配置属性
 * 绑定。DataPermissionFilter 以 Mockito mock 提供，仅验证装配条件；方言注册为全局静态
 * 状态（DialectFactory），不影响容器装配断言。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MybatisFlexAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MybatisFlexAutoConfiguration.class));

    /**
     * imports 注册完整性：包内全部自动配置类均已登记进 AutoConfiguration.imports。
     */
    @Test
    void shouldRegisterAllAutoConfigurationClasses() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered("top.continew.starter.data"))
            .isEmpty();
    }

    /**
     * 开关默认关闭（havingValue 无 matchIfMissing）：未显式开启时静默跳过，不注册任何 Bean。
     */
    @Test
    void shouldSkipSilentlyByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(MybatisFlexAutoConfiguration.class);
            assertThat(context).doesNotHaveBean(MyBatisFlexExtensionProperties.class);
        });
    }

    /**
     * 显式开启且提供前置过滤器后装配：自动配置类注册且开关属性绑定生效。
     */
    @Test
    void shouldRegisterWhenEnabledWithFilterPresent() {
        this.contextRunner.withPropertyValues("mybatis-flex.extension.enabled=true")
            .withBean(DataPermissionFilter.class, () -> mock(DataPermissionFilter.class))
            .run(context -> {
                assertThat(context).hasSingleBean(MybatisFlexAutoConfiguration.class);
                assertThat(context.getBean(MyBatisFlexExtensionProperties.class).isEnabled())
                    .isTrue();
            });
    }

    /**
     * 前置过滤器缺失时应快速失败暴露缺 Bean，而非静默注册一个不可用的配置类。
     */
    @Test
    void shouldFailFastWhenFilterAbsent() {
        this.contextRunner.withPropertyValues("mybatis-flex.extension.enabled=true")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(BeanCreationException.class);
            });
    }

    /**
     * 属性绑定：分页配置默认未设置（null），覆盖值生效。
     */
    @Test
    void shouldBindPaginationProperties() {
        this.contextRunner.withPropertyValues("mybatis-flex.extension.enabled=true")
            .withBean(DataPermissionFilter.class, () -> mock(DataPermissionFilter.class))
            .run(context -> assertThat(context.getBean(MyBatisFlexExtensionProperties.class)
                .getPagination()).isNull());
        this.contextRunner
            .withPropertyValues("mybatis-flex.extension.enabled=true",
                "mybatis-flex.extension.pagination.max-limit=100")
            .withBean(DataPermissionFilter.class, () -> mock(DataPermissionFilter.class))
            .run(context -> {
                MyBatisFlexExtensionProperties properties = context
                    .getBean(MyBatisFlexExtensionProperties.class);
                assertThat(properties.getPagination()).isNotNull();
                assertThat(properties.getPagination().getMaxLimit()).isEqualTo(100L);
            });
    }
}
