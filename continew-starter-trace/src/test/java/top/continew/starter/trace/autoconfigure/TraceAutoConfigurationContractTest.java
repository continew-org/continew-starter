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

package top.continew.starter.trace.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 链路追踪自动配置契约测试
 *
 * <p>固化 TraceAutoConfiguration 的装配契约：imports 登记完整性、Web 环境前提、
 * 开关显式开启才装配、Tracer 缺失时装配路径快速失败与配置属性绑定。Tracer 为 NOOP
 * 时的快速失败语义与过滤器行为由 {@code TraceAutoConfigurationTest} 单元层覆盖，
 * 本类聚焦真实自动装配路径上的条件组合。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TraceAutoConfigurationContractTest {

    private final ApplicationContextRunner nonWebRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TraceAutoConfiguration.class));

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TraceAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.trace").isEmpty());
    }

    /**
     * 非 Web 环境下即使开关开启也应整体跳过（@ConditionalOnWebApplication 前提）。
     */
    @Test
    void shouldSkipInNonWebApplicationEvenIfEnabled() {
        this.nonWebRunner.withPropertyValues("continew-starter.trace.enabled=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
            });
    }

    /**
     * Web 环境下开关未配置时应整体跳过（havingValue="true" 且无 matchIfMissing）。
     */
    @Test
    void shouldSkipByDefaultInWebApplication() {
        this.webRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
        });
    }

    /**
     * Web 环境且开关开启、Tracer 可用时应注册链路追踪过滤器。
     */
    @Test
    void shouldRegisterFilterWhenEnabledWithTracer() {
        this.webRunner.withPropertyValues("continew-starter.trace.enabled=true")
            .withBean(Tracer.class, () -> mock(Tracer.class))
            .run(context -> assertThat(context).hasSingleBean(FilterRegistrationBean.class));
    }

    /**
     * Web 环境且开关开启但 Tracer 缺失时，自动装配路径应快速失败，避免追踪能力静默失效。
     *
     * <p>@Bean 工厂方法抛出的 IllegalStateException 在装配层被 Spring 包装为
     * BeanCreationException，故按真实契约断言根因异常类型。</p>
     */
    @Test
    void shouldFailFastWhenEnabledWithoutTracer() {
        this.webRunner.withPropertyValues("continew-starter.trace.enabled=true")
            .run(context -> assertThat(context).hasFailed()
                .getFailure().hasRootCauseInstanceOf(IllegalStateException.class));
    }

    /**
     * 响应头链路 ID 名称属性应支持默认值与覆盖值绑定。
     */
    @Test
    void shouldBindTraceIdNameProperty() {
        this.webRunner.withPropertyValues("continew-starter.trace.enabled=true")
            .withBean(Tracer.class, () -> mock(Tracer.class))
            .run(context -> assertThat(context.getBean(TraceProperties.class).getTraceIdName())
                .isEqualTo("traceId"));
        this.webRunner
            .withPropertyValues("continew-starter.trace.enabled=true",
                "continew-starter.trace.trace-id-name=x-trace-id")
            .withBean(Tracer.class, () -> mock(Tracer.class))
            .run(context -> assertThat(context.getBean(TraceProperties.class).getTraceIdName())
                .isEqualTo("x-trace-id"));
    }
}
