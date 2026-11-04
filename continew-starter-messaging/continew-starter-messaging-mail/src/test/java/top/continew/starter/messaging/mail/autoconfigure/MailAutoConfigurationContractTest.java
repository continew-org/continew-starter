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

package top.continew.starter.messaging.mail.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 邮件自动配置装配契约测试
 *
 * <p>本模块无启用开关，也无 {@code @ConditionalOnMissingBean} 扩展点，属于「引入即装配」形态：
 * 只要类在 classpath 上就注册 {@code JavaMailSender}，连接参数全部来自 Spring Boot 标准的
 * {@code spring.mail.*} 配置。这里固化 imports 注册完整性与配置传递两条契约，真实 SMTP 收发
 * 不属于装配层职责，不做网络连接。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MailAutoConfigurationContractTest {

    private static final String BASE_PACKAGE = "top.continew.starter.messaging.mail";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MailAutoConfiguration.class));

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 无任何前置条件时默认装配 JavaMailSender。
     */
    @Test
    void shouldRegisterJavaMailSenderByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JavaMailSender.class);
        });
    }

    /**
     * spring.mail.host 覆盖值应传递到 JavaMailSender 实例。
     */
    @Test
    void shouldBindMailHostPropertyToSender() {
        this.contextRunner.withPropertyValues("spring.mail.host=smtp.example.com",
            "spring.mail.port=465").run(context -> {
                assertThat(context).hasNotFailed();
                JavaMailSenderImpl sender = context.getBean(JavaMailSenderImpl.class);
                assertThat(sender.getHost()).isEqualTo("smtp.example.com");
                assertThat(sender.getPort()).isEqualTo(465);
            });
    }
}
