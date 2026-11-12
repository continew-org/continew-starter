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

package top.continew.starter.messaging.mail.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.messaging.mail.core.MailConfig;
import top.continew.starter.messaging.mail.core.MailConfigurer;
import top.continew.starter.messaging.mail.util.MailUtils;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * {@link MailUtils} 发信真实 GreenMail 集成测试：发送链路走 SMTP/SMTPS，断言经独立
 * IMAP 探针核对服务端实际收到的邮件（收件人/主题/正文/附件），不依赖发送方返回值自证。
 *
 * <p>{@link MailConfigurer} 配置由 SpringUtil 取回 Sender 后逐次重放，覆盖邮件配置的
 * 动态生效路径；密送收件人仅经信封投递、不出现在头部，由真实服务器裁决。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("MailUtils 发信真实 GreenMail 集成测试")
@SpringBootTest(classes = MailSendIT.MailTestApp.class)
class MailSendIT extends GreenMailContainerSupport {

    private static final String SENDER_EMAIL = "sender@it.local";
    private static final String SENDER_PASSWORD = "sender-pass";

    /**
     * MailUtils 每次发送都会经 getMailSender() 重放该配置；SSL 用例切换后须还原。
     */
    private static volatile MailConfig mailConfig;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        GreenMailContainerSupport.ensureStarted();
        // Boot 4 自带 MailSenderAutoConfiguration 先注册同名 mailSender，与模块自动配置冲突
        // （产品线索待裁决），测试侧排除 Boot 配置以保留模块真实装配路径
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.mail"
            + ".autoconfigure.MailSenderAutoConfiguration");
        registry.add("spring.mail.host", GreenMailContainerSupport::smtpHost);
        registry.add("spring.mail.port", GreenMailContainerSupport::smtpPort);
        registry.add("spring.mail.username", () -> SENDER_EMAIL);
        registry.add("spring.mail.password", () -> SENDER_PASSWORD);
        registry.add("spring.mail.default-encoding", () -> "UTF-8");
    }

    /**
     * 构造基线邮件配置（properties 非空才会写入 mail.from，触发 setFrom 的发件人覆盖分支）。
     */
    @BeforeAll
    static void initMailConfig() {
        // 静态 @BeforeAll 早于 Spring 上下文装配，容器可能尚未由 @DynamicPropertySource 拉起
        GreenMailContainerSupport.ensureStarted();
        // 发件账号先于任何发信创建，避免 SMTP 认证路径对未知用户拒绝
        GreenMailContainerSupport.createUser(SENDER_EMAIL, SENDER_EMAIL, SENDER_PASSWORD);
        mailConfig = plainConfig();
    }

    private static MailConfig plainConfig() {
        MailConfig config = new MailConfig();
        config.setHost(GreenMailContainerSupport.smtpHost());
        config.setPort(GreenMailContainerSupport.smtpPort());
        config.setUsername(SENDER_EMAIL);
        config.setPassword(SENDER_PASSWORD);
        config.setFrom(SENDER_EMAIL);
        config.getProperties().put("mail.smtp.connectiontimeout", "5000");
        config.setDefaultEncoding(StandardCharsets.UTF_8);
        return config;
    }

    @Test
    @DisplayName("sendText 经 SMTP 送达：服务端邮件的发件人、收件人、主题、正文一致")
    void shouldSendTextMailDeliveredByServer() throws Exception {
        String subject = "text-" + UUID.randomUUID();
        String content = "plain 内容 " + UUID.randomUUID();
        Mailbox recipient = createMailbox("to");

        MailUtils.sendText(recipient.email(), subject, content);

        MimeMessage message = GreenMailContainerSupport.awaitMessage(recipient.login(),
            recipient.password(), subject);
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals(SENDER_EMAIL, from.getAddress());
        assertEquals(1, message.getRecipients(RecipientType.TO).length);
        assertEquals(recipient.email(), ((InternetAddress) message
            .getRecipients(RecipientType.TO)[0]).getAddress());
        assertEquals(content, textContent(message));
        assertTrue(locateTextPart(message).getDataHandler()
            .getContentType().startsWith("text/plain"));
    }

    @Test
    @DisplayName("sendHtml 送达正文为 HTML 内容")
    void shouldSendHtmlMailWithHtmlContentType() throws Exception {
        String subject = "html-" + UUID.randomUUID();
        String html = "<html><body><h1>" + UUID.randomUUID() + "</h1></body></html>";
        Mailbox recipient = createMailbox("html");

        MailUtils.sendHtml(recipient.email(), subject, html);

        MimeMessage message = GreenMailContainerSupport.awaitMessage(recipient.login(),
            recipient.password(), subject);
        assertEquals(html, textContent(message));
        assertTrue(locateTextPart(message).getDataHandler()
            .getContentType().startsWith("text/html"));
    }

    @Test
    @DisplayName("sendHtml 附件随正文送达：附件名与字节内容一致")
    void shouldSendHtmlMailWithAttachment(@TempDir Path tempDir) throws Exception {
        String subject = "attach-" + UUID.randomUUID();
        String html = "<html><body>attachment-" + UUID.randomUUID() + "</body></html>";
        byte[] attachmentBytes = ("附件字节 " + UUID.randomUUID())
            .getBytes(StandardCharsets.UTF_8);
        Path attachmentPath = tempDir.resolve("it-attachment.txt");
        Files.write(attachmentPath, attachmentBytes);
        Mailbox recipient = createMailbox("attach");

        MailUtils.sendHtml(recipient.email(), subject, html, attachmentPath.toFile());

        MimeMessage message = GreenMailContainerSupport.awaitMessage(recipient.login(),
            recipient.password(), subject);
        assertEquals(html, textContent(message));
        Multipart multipart = (Multipart) message.getContent();
        assertEquals(2, multipart.getCount());
        Part attachment = multipart.getBodyPart(1);
        assertEquals("it-attachment.txt", attachment.getFileName());
        assertTrue(attachment.getDisposition().equalsIgnoreCase(Part.ATTACHMENT));
        assertArrayEquals(attachmentBytes, attachment.getInputStream().readAllBytes());
    }

    @Test
    @DisplayName("多收件人/抄送/密送：头部只含 TO 与 CC，密送经信封投递但不出现在头部")
    void shouldDeliverToTosCcsAndBlindlyToBccs() throws Exception {
        String subject = "multi-" + UUID.randomUUID();
        String html = "<html><body>multi-" + UUID.randomUUID() + "</body></html>";
        Mailbox to1 = createMailbox("to1");
        Mailbox to2 = createMailbox("to2");
        Mailbox cc = createMailbox("cc");
        Mailbox bcc = createMailbox("bcc");

        MailUtils.sendHtml(List.of(to1.email(), to2.email()), List.of(cc.email()), List
            .of(bcc.email()), subject, html);

        MimeMessage message = GreenMailContainerSupport.awaitMessage(to1.login(), to1
            .password(), subject);
        List<String> tos = addresses(message, RecipientType.TO);
        assertEquals(List.of(to1.email(), to2.email()), tos);
        assertEquals(List.of(cc.email()), addresses(message, RecipientType.CC));
        // 密送人不出现在任何投递头部，但真实服务器仍按信封收件人投递
        assertNull(message.getRecipients(RecipientType.BCC));
        GreenMailContainerSupport.awaitMessage(bcc.login(), bcc.password(), subject);
        GreenMailContainerSupport.awaitMessage(to2.login(), to2.password(), subject);
        GreenMailContainerSupport.awaitMessage(cc.login(), cc.password(), subject);
    }

    @Test
    @DisplayName("sslEnabled 配置经真实 SMTPS 送达")
    void shouldSendOverSmtpsSocketFactory() throws Exception {
        MailConfig sslConfig = plainConfig();
        sslConfig.setSslEnabled(true);
        sslConfig.setSslPort(GreenMailContainerSupport.smtpsPort());
        // GreenMail 使用自签证书，主机名校验无法对容器地址通过，预置关闭该检查
        sslConfig.getProperties().put("mail.smtp.ssl.checkserveridentity", "false");
        String subject = "smtps-" + UUID.randomUUID();
        String content = "smtps 内容 " + UUID.randomUUID();
        Mailbox recipient = createMailbox("smtps");

        mailConfig = sslConfig;
        try {
            MailUtils.sendText(recipient.email(), subject, content);
            MimeMessage message = GreenMailContainerSupport.awaitMessage(recipient.login(),
                recipient.password(), subject);
            assertEquals(content, textContent(message));
        } finally {
            mailConfig = plainConfig();
        }
    }

    /**
     * 逐层剥开 MIME 包装提取文本正文：MimeMessageHelper 的 multipart 模式会把纯文本
     * 包进 mixed，HTML 再嵌一层 related。
     *
     * @param message 服务端实际收到的邮件
     * @return 文本正文内容
     */
    private static String textContent(MimeMessage message) throws Exception {
        return (String) locateTextPart(message).getContent();
    }

    /**
     * 定位承载文本正文的最内层 MIME 部件。
     *
     * @param message 服务端实际收到的邮件
     * @return 承载文本正文的部件
     */
    private static Part locateTextPart(MimeMessage message) throws Exception {
        Part part = message;
        Object content = message.getContent();
        while (content instanceof Multipart multipart) {
            part = multipart.getBodyPart(0);
            content = part.getContent();
        }
        Object resolved = content;
        assertTrue(resolved instanceof String, () -> "正文应为文本内容，实际 " + resolved);
        return part;
    }

    private static List<String> addresses(MimeMessage message, RecipientType type)
        throws Exception {
        return Arrays.stream(message.getRecipients(type))
            .map(address -> ((InternetAddress) address).getAddress())
            .toList();
    }

    private static Mailbox createMailbox(String tag) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Mailbox mailbox = new Mailbox(tag + "-" + suffix + "@it.local", tag + "-" + suffix,
            "pass-" + suffix);
        GreenMailContainerSupport.createUser(mailbox.email(), mailbox.login(), mailbox
            .password());
        return mailbox;
    }

    /**
     * 邮件测试应用：仅依赖真实自动配置装配 {@link JavaMailSenderImpl}。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class MailTestApp {
    }

    private record Mailbox(String email, String login, String password) {
    }
}
