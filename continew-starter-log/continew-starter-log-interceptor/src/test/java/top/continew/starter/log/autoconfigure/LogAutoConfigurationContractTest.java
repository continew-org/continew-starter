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

package top.continew.starter.log.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.interceptor.LogInterceptor;
import top.continew.starter.log.model.LogProperties;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import java.util.List;

/**
 * 日志自动配置（拦截器实现）装配契约测试
 *
 * <p>开关为 {@code continew-starter.log.enabled}（元注解 {@code @ConditionalOnEnabledLog}），
 * 配置类本身不限定 Web 环境：核心 Bean（拦截器、处理器、持久层）在任意环境装配，仅被导入的
 * {@code LogWebConfiguration}（拦截器注册）限定 Servlet 环境，两个层次分别用普通 runner 与
 * {@code WebApplicationContextRunner} 固化。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LogAutoConfigurationContractTest {

    private static final String BASE_PACKAGE = "top.continew.starter.log";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LogAutoConfiguration.class));

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 默认（Servlet 环境且未关闭开关）应装配拦截器、处理器与持久层默认实现。
     */
    @Test
    void shouldRegisterInterceptorByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LogInterceptor.class);
            assertThat(context).hasSingleBean(LogDao.class);
        });
    }

    /**
     * 显式关闭开关后应整体跳过装配。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.log.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LogInterceptor.class);
                assertThat(context).doesNotHaveBean(LogDao.class);
            });
    }

    /**
     * 非 Web 环境下核心 Bean 仍应装配，仅 Web 层拦截器注册（LogWebConfiguration）跳过。
     */
    @Test
    void shouldRegisterCoreBeansInNonWebEnvironment() {
        ApplicationContextRunner nonWebRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogAutoConfiguration.class));
        nonWebRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LogInterceptor.class);
            assertThat(context).hasSingleBean(LogDao.class);
        });
    }

    /**
     * 使用方自定义日志持久层时应替换默认实现（@ConditionalOnMissingBean 退让）。
     */
    @Test
    void shouldUseCustomLogDaoInsteadOfDefault() {
        LogDao customDao = mock(LogDao.class);
        this.contextRunner.withBean(LogDao.class, () -> customDao).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LogDao.class);
            assertThat(context.getBean(LogDao.class)).isSameAs(customDao);
        });
    }

    /**
     * excludePatterns 覆盖值应绑定到日志属性。
     */
    @Test
    void shouldBindExcludePatternsProperty() {
        this.contextRunner
            .withPropertyValues("continew-starter.log.exclude-patterns[0]=/public/**",
                "continew-starter.log.exclude-patterns[1]=/health")
            .run(context -> {
                assertThat(context).hasNotFailed();
                LogProperties properties = context.getBean(LogProperties.class);
                assertThat(properties.getExcludePatterns())
                    .containsExactlyInAnyOrderElementsOf(List.of("/public/**", "/health"));
            });
    }
}
