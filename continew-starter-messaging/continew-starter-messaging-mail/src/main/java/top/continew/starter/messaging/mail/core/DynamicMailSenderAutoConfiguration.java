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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件发送器
 *
 * @author KAI
 * @since 1.0.0
 */
public class DynamicMailSenderAutoConfiguration extends JavaMailSenderImpl {
    private static final Logger log = LoggerFactory.getLogger(DynamicMailSenderAutoConfiguration.class);

    // 邮件会话属性
    private final Properties javaMailProperties;

    /**
     * 构造函数，使用给定的邮件会话属性初始化邮件发送器。
     *
     * @param javaMailProperties 邮件会话属性
     */
    public DynamicMailSenderAutoConfiguration(Properties javaMailProperties) {
        super(); // 调用父类的构造函数
        this.javaMailProperties = javaMailProperties;

        // 设置用户名和密码
        setUsername(javaMailProperties.getProperty("mail.smtp.username"));
        setPassword(javaMailProperties.getProperty("mail.smtp.password"));

        // 设置默认编码
        String defaultEncoding = javaMailProperties.getProperty("mail.default-encoding", "utf-8");
        setDefaultEncoding(defaultEncoding);
    }

    /**
     * 获取邮件会话。
     *
     * @return 邮件会话
     */
    @Override
    public Session getSession() {
        if (ObjectUtil.isNotEmpty(javaMailProperties)) {
            // 使用提供的身份验证器创建邮件会话
            log.info("[ContiNew Starter] DynamicMailSenderAutoConfiguration creating session with properties: {}", javaMailProperties);
            return Session.getInstance(javaMailProperties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // 返回用户名和密码认证信息
                    return new PasswordAuthentication(javaMailProperties
                        .getProperty("mail.username"), javaMailProperties.getProperty("mail.password"));
                }
            });
        }
        // 如果没有提供属性，使用父类的会话
        return super.getSession();
    }

    /**
     * 创建默认的邮件发送器。
     *
     * @return 默认的邮件发送器
     */
    public static JavaMailSender createDefault() {
        return SpringUtil.getBean(JavaMailSender.class);
    }

    /**
     * 根据邮件配置构建邮件发送器。
     *
     * @param mailConfig 邮件配置对象
     * @return 构建的邮件发送器
     */
    public static JavaMailSender build(MailConfig mailConfig) {
        if (mailConfig != null) {
            Properties properties = mailConfig.toJavaMailProperties();
            log.info("[ContiNew Starter] DynamicMailSenderAutoConfiguration build with mailConfig");
            return new DynamicMailSenderAutoConfiguration(properties);
        } else {
            log.error("[ContiNew Starter] Mail configuration is null, using default mail configuration.");
            return createDefault();
        }
    }
}