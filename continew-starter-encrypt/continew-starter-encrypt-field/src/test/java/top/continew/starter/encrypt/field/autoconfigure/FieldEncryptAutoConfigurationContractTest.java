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

package top.continew.starter.encrypt.field.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.encrypt.enums.Algorithm;
import top.continew.starter.encrypt.field.interceptor.MyBatisDecryptInterceptor;
import top.continew.starter.encrypt.field.interceptor.MyBatisEncryptInterceptor;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 字段加密自动配置契约测试
 *
 * <p>固化 FieldEncryptAutoConfiguration 的装配契约：imports 登记完整性、开关默认启用、
 * 加密/解密两个拦截器各自的用户回退与默认算法属性绑定。两个拦截器分别按具体类型
 * {@code @ConditionalOnMissingBean} 回退，替换其一不影响另一者装配，用例分别固化。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class FieldEncryptAutoConfigurationContractTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FieldEncryptAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.encrypt.field").isEmpty());
    }

    /**
     * 开关默认启用（matchIfMissing），加密与解密拦截器均应注册。
     */
    @Test
    void shouldRegisterInterceptorsByDefault() {
        this.runner.run(context -> {
            assertThat(context).hasSingleBean(MyBatisEncryptInterceptor.class);
            assertThat(context).hasSingleBean(MyBatisDecryptInterceptor.class);
        });
    }

    /**
     * 开关关闭时应整体跳过，两个拦截器均不注册。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.runner.withPropertyValues("continew-starter.encrypt.field.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(MyBatisEncryptInterceptor.class);
                assertThat(context).doesNotHaveBean(MyBatisDecryptInterceptor.class);
            });
    }

    /**
     * 用户替换加密拦截器时，解密拦截器应照常装配（两者回退条件相互独立）。
     */
    @Test
    void shouldUseUserEncryptInterceptorAndKeepDecryptInterceptor() {
        MyBatisEncryptInterceptor userInterceptor = new MyBatisEncryptInterceptor();
        this.runner.withBean(MyBatisEncryptInterceptor.class, () -> userInterceptor)
            .run(context -> {
                assertThat(context).hasSingleBean(MyBatisEncryptInterceptor.class);
                assertSame(userInterceptor, context.getBean(MyBatisEncryptInterceptor.class));
                assertThat(context).hasSingleBean(MyBatisDecryptInterceptor.class);
            });
    }

    /**
     * 用户替换解密拦截器时，加密拦截器应照常装配。
     */
    @Test
    void shouldUseUserDecryptInterceptorAndKeepEncryptInterceptor() {
        MyBatisDecryptInterceptor userInterceptor = new MyBatisDecryptInterceptor();
        this.runner.withBean(MyBatisDecryptInterceptor.class, () -> userInterceptor)
            .run(context -> {
                assertThat(context).hasSingleBean(MyBatisDecryptInterceptor.class);
                assertSame(userInterceptor, context.getBean(MyBatisDecryptInterceptor.class));
                assertThat(context).hasSingleBean(MyBatisEncryptInterceptor.class);
            });
    }

    /**
     * 默认算法属性应支持默认值（AES）与覆盖值（RSA）绑定。
     */
    @Test
    void shouldBindAlgorithmProperty() {
        this.runner.run(context -> assertThat(context.getBean(FieldEncryptProperties.class)
            .getAlgorithm()).isEqualTo(Algorithm.AES));
        this.runner.withPropertyValues("continew-starter.encrypt.field.algorithm=RSA")
            .run(context -> assertThat(context.getBean(FieldEncryptProperties.class)
                .getAlgorithm()).isEqualTo(Algorithm.RSA));
    }
}
