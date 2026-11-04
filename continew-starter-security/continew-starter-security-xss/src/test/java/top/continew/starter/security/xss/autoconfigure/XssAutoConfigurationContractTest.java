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

package top.continew.starter.security.xss.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import top.continew.starter.security.xss.enums.XssMode;
import top.continew.starter.security.xss.filter.XssFilter;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * XSS 过滤自动配置装配契约测试
 *
 * <p>开关为 {@code continew-starter.security.xss.enabled} 且未声明 {@code matchIfMissing}：
 * 属性默认值虽为启用，但环境未声明该属性时条件不匹配、整体不装配，「注解语义优先于属性默认值」
 * 这条容易误判的契约在此固化。模块同时限定 Web 环境，非 Web 应用即使显式开启也应静默跳过。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class XssAutoConfigurationContractTest {

    private static final String BASE_PACKAGE = "top.continew.starter.security.xss";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(XssAutoConfiguration.class));

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 未声明 enabled 属性时应整体跳过装配（无 matchIfMissing，注解语义优先于属性默认值）。
     *
     * <p>负向断言取过滤器注册 Bean：生产侧仅注册包装对象，XssFilter 本身不是独立 Bean，
     * 断言它不存在无法证明过滤器未被注册。</p>
     */
    @Test
    void shouldSkipByDefaultWithoutEnabledProperty() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
        });
    }

    /**
     * 显式关闭后即使处于 Web 环境也应整体跳过装配。
     */
    @Test
    void shouldSkipWhenExplicitlyDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.security.xss.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
            });
    }

    /**
     * 显式开启后应注册 XSS 过滤器（公开形态为过滤器注册 Bean，内持 XssFilter 实例）。
     */
    @Test
    void shouldRegisterFilterWhenEnabled() {
        this.contextRunner.withPropertyValues("continew-starter.security.xss.enabled=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(FilterRegistrationBean.class);
                FilterRegistrationBean<?> registration =
                    context.getBean(FilterRegistrationBean.class);
                assertThat(registration.getFilter()).isInstanceOf(XssFilter.class);
            });
    }

    /**
     * 非 Web 环境下即使显式开启也应静默跳过，不得影响应用启动。
     */
    @Test
    void shouldSkipInNonWebEnvironmentEvenIfEnabled() {
        ApplicationContextRunner nonWebRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(XssAutoConfiguration.class))
            .withPropertyValues("continew-starter.security.xss.enabled=true");
        nonWebRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
        });
    }

    /**
     * mode 覆盖值应绑定到 XSS 配置属性。
     */
    @Test
    void shouldBindModeProperty() {
        this.contextRunner
            .withPropertyValues("continew-starter.security.xss.enabled=true",
                "continew-starter.security.xss.mode=ESCAPE")
            .run(context -> {
                assertThat(context).hasNotFailed();
                XssProperties properties = context.getBean(XssProperties.class);
                assertThat(properties.getMode()).isEqualTo(XssMode.ESCAPE);
            });
    }
}
