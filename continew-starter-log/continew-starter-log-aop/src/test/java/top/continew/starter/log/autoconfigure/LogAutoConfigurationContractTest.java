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
import top.continew.starter.log.aspect.AccessLogAspect;
import top.continew.starter.log.aspect.LogAspect;
import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.handler.AopLogHandler;
import top.continew.starter.log.handler.LogHandler;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 日志自动配置（AOP 实现）装配契约测试
 *
 * <p>本模块装配前提有二：{@code continew-starter.log.enabled}（元注解
 * {@code @ConditionalOnEnabledLog}，缺省视为启用）与 Servlet Web 环境，任一不满足时整体静默
 * 跳过，故分别以 {@code WebApplicationContextRunner} 与普通 runner 双向固化。</p>
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
     * 默认（Servlet 环境且未关闭开关）应装配 AopLogHandler、两个切面与持久层默认实现。
     */
    @Test
    void shouldRegisterAspectsByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBean(LogHandler.class).isInstanceOf(AopLogHandler.class);
            assertThat(context).hasSingleBean(LogAspect.class);
            assertThat(context).hasSingleBean(AccessLogAspect.class);
            assertThat(context).hasSingleBean(LogDao.class);
        });
    }

    /**
     * 使用方自定义日志处理器时应替换默认 AopLogHandler（@ConditionalOnMissingBean 退让）。
     */
    @Test
    void shouldUseCustomLogHandlerInsteadOfDefault() {
        LogHandler customHandler = mock(LogHandler.class);
        this.contextRunner.withBean(LogHandler.class, () -> customHandler)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(LogHandler.class);
                assertThat(context.getBean(LogHandler.class)).isSameAs(customHandler);
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
                assertThat(context).doesNotHaveBean(LogAspect.class);
                assertThat(context).doesNotHaveBean(LogDao.class);
            });
    }

    /**
     * 非 Servlet 环境下即使开关开启也应静默跳过，不得影响应用启动。
     */
    @Test
    void shouldSkipInNonServletEnvironment() {
        ApplicationContextRunner nonWebRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogAutoConfiguration.class));
        nonWebRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(LogAspect.class);
        });
    }

    /**
     * 使用方自定义日志持久层时应替换默认实现（@ConditionalOnMissingBean 退让）。
     */
    @Test
    void shouldUseCustomLogDaoInsteadOfDefault() {
        LogDao customDao = mock(LogDao.class);
        this.contextRunner.withBean(LogHandler.class, () -> mock(LogHandler.class))
            .withBean(LogDao.class, () -> customDao)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(LogDao.class);
                assertThat(context.getBean(LogDao.class)).isSameAs(customDao);
            });
    }
}
