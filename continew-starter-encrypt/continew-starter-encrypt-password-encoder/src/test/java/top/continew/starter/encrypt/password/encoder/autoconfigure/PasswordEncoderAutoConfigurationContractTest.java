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

package top.continew.starter.encrypt.password.encoder.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import jakarta.validation.Validator;

/**
 * 密码编码器自动配置契约测试
 *
 * <p>固化 PasswordEncoderAutoConfiguration 的装配契约：imports 登记完整性、开关
 * 显式开启才装配（无 matchIfMissing）、PasswordEncoder 用户回退与默认算法属性绑定。
 * 默认算法经由 DelegatingPasswordEncoder 的 id 前缀体现，用编码输出前缀固化算法切换。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class PasswordEncoderAutoConfigurationContractTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PasswordEncoderAutoConfiguration.class))
        // Bean 方法经 CheckUtils 做算法校验，其父类校验器静态初始化依赖 Hutool SpringUtil
        // 上下文与容器中的 jakarta.validation.Validator，均按真实使用场景供给
        .withBean(SpringUtil.class)
        .withBean(Validator.class, () -> mock(Validator.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.encrypt.password.encoder").isEmpty());
    }

    /**
     * 开关未配置时应整体跳过（havingValue="true" 且无 matchIfMissing，默认不装配）。
     */
    @Test
    void shouldSkipByDefault() {
        this.runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PasswordEncoder.class);
        });
    }

    /**
     * 开关显式开启时应注册编码器，默认算法为 BCRYPT（编码输出携带 id 前缀）。
     */
    @Test
    void shouldRegisterPasswordEncoderWhenEnabled() {
        this.runner.withPropertyValues("continew-starter.encrypt.password-encoder.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(PasswordEncoder.class);
                assertThat(context.getBean(PasswordEncoder.class).encode("pwd"))
                    .startsWith("{bcrypt}");
            });
    }

    /**
     * 开关显式关闭时同样整体跳过，与未配置等价。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.runner.withPropertyValues("continew-starter.encrypt.password-encoder.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(PasswordEncoder.class);
            });
    }

    /**
     * 用户注册 PasswordEncoder 时应回退到用户实现，默认编码器不再创建。
     */
    @Test
    void shouldUseUserPasswordEncoderWhenProvided() {
        PasswordEncoder userEncoder = mock(PasswordEncoder.class);
        this.runner.withPropertyValues("continew-starter.encrypt.password-encoder.enabled=true")
            .withBean(PasswordEncoder.class, () -> userEncoder)
            .run(context -> {
                assertThat(context).hasSingleBean(PasswordEncoder.class);
                assertSame(userEncoder, context.getBean(PasswordEncoder.class));
            });
    }

    /**
     * 默认算法属性应支持覆盖切换：算法切换后编码输出前缀随 id 同步变化。
     */
    @Test
    void shouldBindAlgorithmProperty() {
        this.runner.withPropertyValues("continew-starter.encrypt.password-encoder.enabled=true",
            "continew-starter.encrypt.password-encoder.algorithm=PBKDF2").run(context -> {
                assertThat(context.getBean(PasswordEncoderProperties.class).getAlgorithm()
                    .name()).isEqualTo("PBKDF2");
                assertThat(context.getBean(PasswordEncoder.class).encode("pwd"))
                    .startsWith("{pbkdf2}");
            });
    }
}
