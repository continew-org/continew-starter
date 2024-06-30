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

import org.springframework.mail.javamail.JavaMailSenderImpl;
import top.continew.starter.core.util.validate.ValidationUtils;

/**
 * 邮件配置
 *
 * @author Charles7c
 * @since 2.1.0
 */
public interface MailConfigurer {

    /**
     * 获取邮件配置
     *
     * @return 邮件配置
     */
    MailConfig getMailConfig();

    /**
     * 应用配置
     *
     * @param mailConfig 邮件配置
     * @param sender     邮件 Sender
     */
    default void apply(MailConfig mailConfig, JavaMailSenderImpl sender) {
        String protocolLowerCase = mailConfig.getProtocol().toLowerCase();
        ValidationUtils.throwIfNotEqual(MailConfig.DEFAULT_PROTOCOL, protocolLowerCase, "邮件配置错误：不支持的邮件发送协议: %s"
            .formatted(mailConfig.getProtocol()));
        sender.setProtocol(mailConfig.getProtocol());

        ValidationUtils.throwIfBlank(mailConfig.getHost(), "邮件配置错误：服务器地址不能为空");
        sender.setHost(mailConfig.getHost());

        ValidationUtils.throwIfNull(mailConfig.getPort(), "邮件配置错误：服务器端口不能为空");
        sender.setPort(mailConfig.getPort());

        ValidationUtils.throwIfBlank(mailConfig.getUsername(), "邮件配置错误：用户名不能为空");
        sender.setUsername(mailConfig.getUsername());

        ValidationUtils.throwIfBlank(mailConfig.getPassword(), "邮件配置错误：密码不能为空");
        sender.setPassword(mailConfig.getPassword());

        if (mailConfig.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(mailConfig.getDefaultEncoding().name());
        }

        if (!mailConfig.getProperties().isEmpty()) {
            sender.setJavaMailProperties(mailConfig.toJavaMailProperties());
        }
    }
}
