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

package top.continew.starter.messaging.mail.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.continew.starter.core.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * {@link MailConfigurer#apply} 默认装配逻辑单元测试：配置项到 JavaMailSender 的映射与校验。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MailConfigApplyTest {

    /**
     * ValidationUtils 的静态初始化经 SpringUtil 定位 jakarta Validator，
     * 测试侧用最小上下文满足该约定。
     */
    @BeforeAll
    static void setUpSpringContext() {
        if (SpringUtil.getApplicationContext() != null) {
            return;
        }
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(SpringUtil.class);
        context.registerBean(jakarta.validation.Validator.class, LocalValidatorFactoryBean::new);
        context.refresh();
    }

    /**
     * 构建合法配置。
     *
     * @return MailConfig
     */
    private MailConfig validConfig() {
        MailConfig config = new MailConfig();
        config.setHost("smtp.example.com");
        config.setPort(465);
        config.setUsername("user@example.com");
        config.setPassword("secret");
        return config;
    }

    /**
     * 合法配置应完整映射协议/主机/端口/账号。
     */
    @Test
    @DisplayName("apply 完整映射基础配置")
    void shouldApplyBasicConfig() {
        MailConfig config = validConfig();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        new MailConfigurer() {

            @Override
            public MailConfig getMailConfig() {
                return null;
            }
        }.apply(config, sender);

        assertThat(sender.getProtocol()).isEqualTo("smtp");
        assertThat(sender.getHost()).isEqualTo("smtp.example.com");
        assertThat(sender.getPort()).isEqualTo(465);
        assertThat(sender.getUsername()).isEqualTo("user@example.com");
        assertThat(sender.getPassword()).isEqualTo("secret");
    }

    /**
     * 非 smtp 协议应抛 400 异常。
     */
    @Test
    @DisplayName("apply 拒绝非 smtp 协议")
    void shouldRejectNonSmtpProtocol() {
        MailConfig config = validConfig();
        config.setProtocol("pop3");

        assertThatThrownBy(() -> new MailConfigurer() {

            @Override
            public MailConfig getMailConfig() {
                return null;
            }
        }.apply(config, new JavaMailSenderImpl())).isInstanceOf(BadRequestException.class)
            .hasMessageContaining("不支持的邮件发送协议");
    }

    /**
     * host/username/password 缺失应逐项抛 400 异常。
     */
    @Test
    @DisplayName("apply 校验必填项")
    void shouldValidateRequiredFields() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        MailConfig noHost = new MailConfig();
        noHost.setProtocol("smtp");
        noHost.setPort(465);
        noHost.setUsername("user");
        noHost.setPassword("secret");
        assertThatThrownBy(() -> new MailConfigurer() {

            @Override
            public MailConfig getMailConfig() {
                return null;
            }
        }.apply(noHost, sender)).isInstanceOf(BadRequestException.class)
            .hasMessageContaining("服务器地址不能为空");

        MailConfig noUser = validConfig();
        noUser.setUsername("");
        assertThatThrownBy(() -> new MailConfigurer() {

            @Override
            public MailConfig getMailConfig() {
                return null;
            }
        }.apply(noUser, sender)).isInstanceOf(BadRequestException.class)
            .hasMessageContaining("用户名不能为空");
    }

    /**
     * SSL 开启时应写入信任与校验服务端身份属性并要求 sslPort。
     */
    @Test
    @DisplayName("apply SSL 属性装配")
    void shouldApplySslProperties() {
        MailConfig config = validConfig();
        config.setSslEnabled(true);
        config.setSslPort(465);
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        new MailConfigurer() {

            @Override
            public MailConfig getMailConfig() {
                return null;
            }
        }.apply(config, sender);

        Properties properties = sender.getJavaMailProperties();
        assertThat(properties.get("mail.smtp.auth")).isEqualTo(Boolean.TRUE);
        assertThat(properties.getProperty("mail.smtp.socketFactory.class"))
            .isEqualTo("javax.net.ssl.SSLSocketFactory");
        assertThat(properties.get("mail.smtp.socketFactory.port")).isEqualTo(465);
    }

    /**
     * SSL 开启但未配置 sslPort 应抛 400 异常。
     */
    @Test
    @DisplayName("apply 校验 SSL 端口")
    void shouldValidateSslPort() {
        MailConfig config = validConfig();
        config.setSslEnabled(true);

        assertThatThrownBy(() -> new MailConfigurer() {

            @Override
            public MailConfig getMailConfig() {
                return null;
            }
        }.apply(config, new JavaMailSenderImpl())).isInstanceOf(BadRequestException.class)
            .hasMessageContaining("SSL端口不能为空");
    }

    /**
     * defaultEncoding 应映射为编码名称。
     */
    @Test
    @DisplayName("apply 默认编码映射")
    void shouldApplyDefaultEncoding() {
        MailConfig config = validConfig();
        config.setDefaultEncoding(StandardCharsets.UTF_8);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        new MailConfigurer() {

            @Override
            public MailConfig getMailConfig() {
                return null;
            }
        }.apply(config, sender);

        assertThat(sender.getDefaultEncoding()).isEqualTo("UTF-8");
    }
}
