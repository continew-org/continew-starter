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

import top.continew.starter.core.util.validate.ValidationUtils;

import java.util.Properties;

/**
 * 邮件配置类
 *
 * @author KAI
 * @since 1.0.0
 */
public class MailConfig {

    /**
     * 邮件发送协议 (例如: smtp, smtps)
     */
    private String mailProtocol = "smtp";

    /**
     * SMTP 服务器地址
     */
    private String mailHost;

    /**
     * SMTP 服务器端口
     */
    private int mailPort;

    /**
     * SMTP 用户名
     */
    private String mailUsername;

    /**
     * SMTP 授权码
     */
    private String mailPassword;

    /**
     * 发件人邮箱地址
     */
    private String mailFrom;

    /**
     * 是否启用 SSL 连接
     */
    private boolean sslEnabled = false;

    /**
     * SSL 端口
     */
    private int sslPort;

    public String getMailProtocol() {
        return mailProtocol;
    }

    public void setMailProtocol(String mailProtocol) {
        this.mailProtocol = mailProtocol;
    }

    public String getMailHost() {
        return mailHost;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

    public int getMailPort() {
        return mailPort;
    }

    public void setMailPort(int mailPort) {
        this.mailPort = mailPort;
    }

    public String getMailUsername() {
        return mailUsername;
    }

    public void setMailUsername(String mailUsername) {
        this.mailUsername = mailUsername;
    }

    public String getMailPassword() {
        return mailPassword;
    }

    public void setMailPassword(String mailPassword) {
        this.mailPassword = mailPassword;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    /**
     * 将当前配置转换为 JavaMail 的 Properties 对象
     *
     * @return 配置好的 Properties 对象
     */
    public Properties toJavaMailProperties() {
        Properties javaMailProperties = new Properties();

        ValidationUtils.throwIf(!this.mailProtocol.equals("smtp"), "不支持的邮件发送协议: " + this.mailProtocol);
        // 设置邮件发送协议
        javaMailProperties.put("mail.transport.protocol", this.mailProtocol.toLowerCase());

        // 设置 SMTP 服务器地址
        ValidationUtils.throwIfBlank(this.mailHost, "SMTP服务器地址不能为空");
        javaMailProperties.put("mail.smtp.host", this.mailHost);

        // 设置 SMTP 服务器端口
        ValidationUtils.throwIfNull(this.mailPort, "SMTP服务端口不能为空");
        javaMailProperties.put("mail.smtp.port", this.mailPort);

        // 设置 SMTP 用户名
        ValidationUtils.throwIfBlank(this.mailUsername, "SMTP用户名不能为空");
        javaMailProperties.put("mail.smtp.user", this.mailUsername);

        // 设置 SMTP 授权码
        ValidationUtils.throwIfBlank(this.mailPassword, "SMTP授权码不能为空");
        javaMailProperties.put("mail.smtp.password", this.mailPassword);

        // 启用 SMTP 认证
        javaMailProperties.put("mail.smtp.auth", "true");

        // 设置 SSL 连接
        javaMailProperties.put("mail.smtp.ssl.enable", this.sslEnabled);

        // 设置默认发件人地址
        ValidationUtils.throwIfBlank(this.mailFrom, "默认发件人地址不能为空");
        javaMailProperties.put("mail.from", this.mailFrom);

        // 设置默认编码为 UTF-8
        javaMailProperties.put("mail.defaultEncoding", "utf-8");

        // 如果启用 SSL，设置 SSL Socket 工厂和端口
        if (sslEnabled) {
            ValidationUtils.throwIfNull(this.sslPort, "SSL端口不能为空");
            javaMailProperties.put("mail.smtp.socketFactory.port", this.sslPort);
            javaMailProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        return javaMailProperties;
    }
}