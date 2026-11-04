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

package top.continew.starter.openapi.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.openapi.audit.OpenApiSignAuditSink;
import top.continew.starter.openapi.filter.OpenApiSignFilter;
import top.continew.starter.openapi.spi.AuthorizationChecker;
import top.continew.starter.openapi.spi.CredentialResolver;
import top.continew.starter.openapi.spi.ReplayGuard;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import java.time.Duration;
import java.util.Optional;

/**
 * 开放接口验签自动配置装配契约测试
 *
 * <p>固化两条契约：{@code continew-starter.openapi.enabled} 无 {@code matchIfMissing}，缺省视为
 * 关闭，使用方必须显式开启；三个扩展点（凭证查询、授权判定、重放防护）与审计接收均提供默认
 * Bean，使用方定义同名 Bean 后默认实现退位。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class OpenApiSignAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OpenApiSignAutoConfiguration.class));

    /**
     * imports 资源必须登记本模块包内全部自动配置类，防止新增配置类漏注册后静默失效。
     */
    @Test
    void importsShouldBeComplete() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered("top.continew.starter.openapi"))
            .isEmpty();
    }

    /**
     * 开关缺省视为关闭，不得装配任何 Bean 且不影响应用启动。
     */
    @Test
    void shouldSkipByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(OpenApiSignFilter.class);
            assertThat(context).doesNotHaveBean(CredentialResolver.class);
            assertThat(context).doesNotHaveBean(AuthorizationChecker.class);
            assertThat(context).doesNotHaveBean(ReplayGuard.class);
            assertThat(context).doesNotHaveBean(OpenApiSignAuditSink.class);
        });
    }

    /**
     * 显式开启后装配过滤器与全部默认扩展点，且时间窗口、nonce 有效期、body 上限、拦截路径绑定设计默认值。
     */
    @Test
    void shouldRegisterBeansWithDefaultPropertiesWhenEnabled() {
        this.contextRunner.withPropertyValues("continew-starter.openapi.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(OpenApiSignProperties.class);
                OpenApiSignProperties properties = context.getBean(OpenApiSignProperties.class);
                assertThat(properties.getSign().getTimestampWindow()).isEqualTo(300);
                assertThat(properties.getReplay().getNonceTtl()).isEqualTo(600);
                assertThat(properties.getSign().getMaxBodySize()).isEqualTo(1048576L);
                assertThat(properties.getSign().getClockSkew()).isZero();
                assertThat(properties.getSign().getMaxHeaderSize()).isEqualTo(4096);
                assertThat(properties.getSign().getMaxQuerySize()).isEqualTo(8192);
                assertThat(properties.getAlgorithm()).isEqualTo("OCN1-HMAC-SHA256");
                assertThat(properties.getIncludePaths()).containsExactly("/open/*");
                assertThat(context).hasSingleBean(CredentialResolver.class);
                assertThat(context).hasSingleBean(AuthorizationChecker.class);
                assertThat(context).hasSingleBean(ReplayGuard.class);
                assertThat(context).hasSingleBean(OpenApiSignAuditSink.class);
            });
    }

    /**
     * 过滤器注册顺序为 OrderedConstants.Filter.OPENAPI_SIGN_FILTER，默认拦截路径为 /open/*。
     */
    @Test
    void shouldRegisterFilterWithConfiguredOrderAndPaths() {
        this.contextRunner.withPropertyValues("continew-starter.openapi.enabled=true")
            .run(context -> {
                FilterRegistrationBean<?> bean =
                    context.getBean("openApiSignFilter", FilterRegistrationBean.class);
                assertThat(bean.getOrder()).isEqualTo(OrderedConstants.Filter.OPENAPI_SIGN_FILTER);
                assertThat(bean.getUrlPatterns()).containsExactly("/open/*");
            });
    }

    /**
     * include-paths 可自定义且逐条生效。
     */
    @Test
    void shouldRegisterFilterWithCustomizedPaths() {
        this.contextRunner.withPropertyValues("continew-starter.openapi.enabled=true",
            "continew-starter.openapi.include-paths=/api/open/*").run(context -> {
                FilterRegistrationBean<?> bean =
                    context.getBean("openApiSignFilter", FilterRegistrationBean.class);
                assertThat(bean.getUrlPatterns()).containsExactly("/api/open/*");
            });
    }

    /**
     * 默认凭证查询返回空（拒绝），授权判定默认拒绝全部，安全默认不因缺省配置放行。
     */
    @Test
    void defaultSpiBeansShouldDenyByDefault() {
        this.contextRunner.withPropertyValues("continew-starter.openapi.enabled=true")
            .run(context -> {
                assertThat(context.getBean(CredentialResolver.class).resolve("any-access-key"))
                    .isEmpty();
                assertThat(
                    context.getBean(AuthorizationChecker.class).check(1L, "GET", "/open/api/any"))
                    .isFalse();
            });
    }

    /**
     * 使用方注册同名 SPI Bean 后，默认实现退位，容器保留使用方实例。
     */
    @Test
    void shouldBackOffWhenCustomSpiBeansPresent() {
        CredentialResolver customResolver = accessKey -> Optional.empty();
        AuthorizationChecker customChecker = (appId, method, path) -> true;
        ReplayGuard customGuard = new ReplayGuard() {

            @Override
            public boolean tryAcquire(String nonceKey, Duration ttl) {
                return true;
            }

            @Override
            public void release(String nonceKey) {
            }
        };
        OpenApiSignAuditSink customSink = event -> {
        };
        this.contextRunner.withPropertyValues("continew-starter.openapi.enabled=true")
            .withBean("credentialResolver", CredentialResolver.class, () -> customResolver)
            .withBean("authorizationChecker", AuthorizationChecker.class, () -> customChecker)
            .withBean("replayGuard", ReplayGuard.class, () -> customGuard)
            .withBean("openApiSignAuditSink", OpenApiSignAuditSink.class, () -> customSink)
            .run(context -> {
                assertThat(context.getBean(CredentialResolver.class)).isSameAs(customResolver);
                assertThat(context.getBean(AuthorizationChecker.class)).isSameAs(customChecker);
                assertThat(context.getBean(ReplayGuard.class)).isSameAs(customGuard);
                assertThat(context.getBean(OpenApiSignAuditSink.class)).isSameAs(customSink);
            });
    }
}
