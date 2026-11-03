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

package top.continew.starter.encrypt.api.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import top.continew.starter.encrypt.api.filter.ApiEncryptFilter;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * API 加密自动配置契约测试
 *
 * <p>固化 ApiEncryptAutoConfiguration 的装配契约：imports 登记完整性、开关默认启用、
 * 过滤器注册与配置属性绑定。过滤器 Bean 无条件注册（未标注 @ConditionalOnMissingBean），
 * 用户整体替换 FilterRegistrationBean 属于同类型覆盖而非条件回退，故不设 fallback 用例。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ApiEncryptAutoConfigurationContractTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ApiEncryptAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.encrypt.api").isEmpty());
    }

    /**
     * 开关默认启用（matchIfMissing），注册的过滤器应为 API 加密过滤器。
     */
    @Test
    void shouldRegisterEncryptFilterByDefault() {
        this.runner.run(context -> {
            assertThat(context).hasSingleBean(FilterRegistrationBean.class);
            assertThat(context.getBean(FilterRegistrationBean.class).getFilter())
                .isInstanceOf(ApiEncryptFilter.class);
        });
    }

    /**
     * 开关关闭时应整体跳过，不注册任何过滤器。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.runner.withPropertyValues("continew-starter.encrypt.api.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
            });
    }

    /**
     * AES 密钥请求头键名属性应支持默认值与覆盖值绑定。
     */
    @Test
    void shouldBindSecretKeyHeaderProperty() {
        this.runner.run(context -> assertThat(context.getBean(ApiEncryptProperties.class)
            .getSecretKeyHeader()).isEqualTo("X-Api-Encrypt"));
        this.runner.withPropertyValues("continew-starter.encrypt.api.secret-key-header=X-Enc-Key")
            .run(context -> assertThat(context.getBean(ApiEncryptProperties.class)
                .getSecretKeyHeader()).isEqualTo("X-Enc-Key"));
    }
}
