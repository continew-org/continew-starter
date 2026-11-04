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

package top.continew.starter.web.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;
import top.continew.starter.web.annotation.EnableGlobalResponse;
import top.continew.starter.web.autoconfigure.cors.CorsAutoConfiguration;
import top.continew.starter.web.autoconfigure.mvc.WebMvcAutoConfiguration;
import top.continew.starter.web.autoconfigure.response.ConstraintViolationExceptionHandler;
import top.continew.starter.web.autoconfigure.response.ExceptionAliasRegister;
import top.continew.starter.web.autoconfigure.response.GlobalExceptionHandler;
import top.continew.starter.web.autoconfigure.response.GlobalResponseFactory;
import top.continew.starter.web.autoconfigure.response.GlobalResponseProperties;
import top.continew.starter.web.autoconfigure.server.ServerExtensionAutoConfiguration;
import top.continew.starter.web.autoconfigure.server.ServerExtensionProperties;

import jakarta.validation.ConstraintViolationException;
import java.util.List;

/**
 * Web 模块自动配置契约测试
 *
 * <p>覆盖 MVC、跨域、服务器扩展与全局响应四个装配入口：imports 登记完整性、开关装配、
 * 属性绑定与扩展点回退。</p>
 *
 * <p>GlobalResponseAutoConfiguration 为 @Configuration 类型，由 {@code @EnableGlobalResponse}
 * 通过 @Import 激活，不登记 imports 属有意设计；AutoConfigurationImportsAssert 仅扫描
 * {@code @AutoConfiguration} 注解类型，不会将其误报为缺口。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class WebAutoConfigurationContractTest {

    private final ApplicationContextRunner mvcRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class));

    private final WebApplicationContextRunner corsRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CorsAutoConfiguration.class));

    private final WebApplicationContextRunner serverRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ServerExtensionAutoConfiguration.class));

    private final ApplicationContextRunner responseRunner = new ApplicationContextRunner();

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源
     * （GlobalResponseAutoConfiguration 不参与扫描，见类注释）。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert.findUnregistered("top.continew.starter.web")
            .isEmpty());
    }

    /**
     * WebMvcAutoConfiguration 无装配开关，默认应注册自身与消息转换器定制器。
     */
    @Test
    void webMvcShouldRegisterByDefault() {
        this.mvcRunner.run(context -> {
            assertThat(context).hasSingleBean(WebMvcAutoConfiguration.class);
            assertThat(context).hasSingleBean(ServerHttpMessageConvertersCustomizer.class);
        });
    }

    /**
     * continew-starter.web.cors.enabled=true 时应注册 CorsFilter（@ConditionalOnWebApplication
     * 前提由 WebApplicationContextRunner 满足）。
     */
    @Test
    void corsShouldRegisterFilterWhenEnabled() {
        this.corsRunner.withPropertyValues(PropertiesConstants.WEB_CORS + ".enabled=true")
            .run(context -> assertThat(context).hasSingleBean(CorsFilter.class));
    }

    /**
     * cors.enabled 无 matchIfMissing，默认关闭，不得产生副作用 Bean。
     */
    @Test
    void corsShouldSkipByDefault() {
        this.corsRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(CorsFilter.class);
        });
    }

    /**
     * 用户提供 CorsFilter 时应整体回退，容器中保持用户实例（@ConditionalOnMissingBean 语义）。
     */
    @Test
    void corsFilterShouldFallbackToUserProvidedBean() {
        CorsFilter custom = new CorsFilter(new UrlBasedCorsConfigurationSource());
        this.corsRunner.withPropertyValues(PropertiesConstants.WEB_CORS + ".enabled=true")
            .withBean(CorsFilter.class, () -> custom)
            .run(context -> {
                assertThat(context).hasSingleBean(CorsFilter.class);
                assertSame(custom, context.getBean(CorsFilter.class));
            });
    }

    /**
     * server.extension.enabled 无 matchIfMissing：ServerExtensionProperties 默认 true 但开关未配置
     * 时不装配，固化「显式开启才生效」的语义。
     */
    @Test
    void serverExtensionShouldSkipByDefault() {
        this.serverRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
        });
    }

    /**
     * server.extension.enabled=true 时应注册禁用不安全 HTTP 方法的过滤器注册器，且
     * disallowed-methods 覆盖配置应绑定到 ServerExtensionProperties（默认 CONNECT/TRACE/TRACK）。
     */
    @Test
    void serverExtensionShouldRegisterAndBindWhenEnabled() {
        this.serverRunner
            .withPropertyValues("server.extension.enabled=true",
                "server.extension.disallowed-methods[0]=FOO")
            .run(context -> {
                assertThat(context).hasSingleBean(FilterRegistrationBean.class);
                ServerExtensionProperties properties =
                    context.getBean(ServerExtensionProperties.class);
                assertEquals(List.of("FOO"), properties.getDisallowedMethods());
            });
    }

    /**
     * 全局响应应经由 @EnableGlobalResponse 注解激活（而非 imports 登记），核心 Bean 齐备。
     */
    @Test
    void globalResponseShouldActivateViaEnableAnnotation() {
        this.responseRunner.withUserConfiguration(EnableGlobalResponseConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(GlobalResponseFactory.class);
                assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
                assertThat(context).hasSingleBean(ExceptionAliasRegister.class);
            });
    }

    /**
     * 响应配置属性默认成功响应码为 0，覆盖配置后应生效。
     */
    @Test
    void globalResponsePropertiesShouldBindWithDefaultAndOverride() {
        this.responseRunner.withUserConfiguration(EnableGlobalResponseConfiguration.class)
            .run(context -> {
                GlobalResponseProperties properties =
                    context.getBean(GlobalResponseProperties.class);
                assertEquals("0", properties.getDefaultSuccessCode());
            });
        this.responseRunner.withUserConfiguration(EnableGlobalResponseConfiguration.class)
            .withPropertyValues(PropertiesConstants.WEB_RESPONSE + ".default-success-code=200")
            .run(context -> {
                GlobalResponseProperties properties =
                    context.getBean(GlobalResponseProperties.class);
                assertEquals("200", properties.getDefaultSuccessCode());
            });
    }

    /**
     * 用户提供 GlobalResponseFactory 时应整体回退（@ConditionalOnMissingBean 语义）。
     */
    @Test
    void globalResponseFactoryShouldFallbackToUserProvidedBean() {
        GlobalResponseFactory custom = mock(GlobalResponseFactory.class);
        this.responseRunner.withUserConfiguration(EnableGlobalResponseConfiguration.class)
            .withBean(GlobalResponseFactory.class, () -> custom)
            .run(context -> {
                assertThat(context).hasSingleBean(GlobalResponseFactory.class);
                assertSame(custom, context.getBean(GlobalResponseFactory.class));
            });
    }

    /**
     * 参数校验异常处理器由嵌套 @ConditionalOnClass 控制：classpath 缺失
     * ConstraintViolationException 时应跳过，其余全局响应 Bean 不受影响。
     */
    @Test
    void constraintViolationHandlerShouldFollowConditionalOnClass() {
        this.responseRunner.withUserConfiguration(EnableGlobalResponseConfiguration.class)
            .run(context -> assertThat(context)
                .hasSingleBean(ConstraintViolationExceptionHandler.class));
        this.responseRunner.withUserConfiguration(EnableGlobalResponseConfiguration.class)
            .withClassLoader(new FilteredClassLoader(ConstraintViolationException.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ConstraintViolationExceptionHandler.class);
            });
    }

    /**
     * 模拟使用方以 {@code @EnableGlobalResponse} 标注的配置类。
     */
    @EnableGlobalResponse
    @Configuration(proxyBeanMethods = false)
    static class EnableGlobalResponseConfiguration {
    }
}
