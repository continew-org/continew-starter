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

package top.charles7c.continew.starter.messaging.mail.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import top.charles7c.continew.starter.core.constant.StringConstants;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 邮件工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class MailUtils {

    private static final JavaMailSender MAIL_SENDER = SpringUtil.getBean(JavaMailSender.class);

    private MailUtils() {
    }

    /**
     * 发送文本邮件给单个人
     *
     * @param subject 主题
     * @param content 内容
     * @param to      收件人
     * @throws MessagingException /
     */
    public static void sendText(String to, String subject, String content) throws MessagingException {
        send(splitAddress(to), null, null, subject, content, false);
    }

    /**
     * 发送 HTML 邮件给单个人
     *
     * @param subject 主题
     * @param content 内容
     * @param to      收件人
     * @throws MessagingException /
     */
    public static void sendHtml(String to, String subject, String content) throws MessagingException {
        send(splitAddress(to), null, null, subject, content, true);
    }

    /**
     * 发送 HTML 邮件给单个人
     *
     * @param subject 主题
     * @param content 内容
     * @param to      收件人
     * @param files   附件列表
     * @throws MessagingException /
     */
    public static void sendHtml(String to, String subject, String content, File... files) throws MessagingException {
        send(splitAddress(to), null, null, subject, content, true, files);
    }

    /**
     * 发送 HTML 邮件给多个人
     *
     * @param subject 主题
     * @param content 内容
     * @param tos     收件人列表
     * @param files   附件列表
     * @throws MessagingException /
     */
    public static void sendHtml(Collection<String> tos,
                                String subject,
                                String content,
                                File... files) throws MessagingException {
        send(tos, null, null, subject, content, true, files);
    }

    /**
     * 发送 HTML 邮件给多个人
     *
     * @param subject 主题
     * @param content 内容
     * @param tos     收件人列表
     * @param ccs     抄送人列表
     * @param files   附件列表
     * @throws MessagingException /
     */
    public static void sendHtml(Collection<String> tos,
                                Collection<String> ccs,
                                String subject,
                                String content,
                                File... files) throws MessagingException {
        send(tos, ccs, null, subject, content, true, files);
    }

    /**
     * 发送 HTML 邮件给多个人
     *
     * @param subject 主题
     * @param content 内容
     * @param tos     收件人列表
     * @param ccs     抄送人列表
     * @param bccs    密送人列表
     * @param files   附件列表
     * @throws MessagingException /
     */
    public static void sendHtml(Collection<String> tos,
                                Collection<String> ccs,
                                Collection<String> bccs,
                                String subject,
                                String content,
                                File... files) throws MessagingException {
        send(tos, ccs, bccs, subject, content, true, files);
    }

    /**
     * 发送邮件给多个人
     *
     * @param tos     收件人列表
     * @param ccs     抄送人列表
     * @param bccs    密送人列表
     * @param subject 主题
     * @param content 内容
     * @param isHtml  是否是 HTML
     * @param files   附件列表
     * @throws MessagingException /
     */
    public static void send(Collection<String> tos,
                            Collection<String> ccs,
                            Collection<String> bccs,
                            String subject,
                            String content,
                            boolean isHtml,
                            File... files) throws MessagingException {
        Assert.isFalse(CollUtil.isEmpty(tos), "请至少指定一名收件人");
        MimeMessage mimeMessage = MAIL_SENDER.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8
            .displayName());
        // 设置基本信息
        messageHelper.setFrom(SpringUtil.getProperty("spring.mail.username"));
        messageHelper.setSubject(subject);
        messageHelper.setText(content, isHtml);
        // 设置收信人
        // 抄送人
        if (CollUtil.isNotEmpty(ccs)) {
            messageHelper.setCc(ccs.toArray(String[]::new));
        }
        // 密送人
        if (CollUtil.isNotEmpty(bccs)) {
            messageHelper.setBcc(bccs.toArray(String[]::new));
        }
        // 收件人
        messageHelper.setTo(tos.toArray(String[]::new));
        // 设置附件
        if (ArrayUtil.isNotEmpty(files)) {
            for (File file : files) {
                messageHelper.addAttachment(file.getName(), file);
            }
        }
        // 发送邮件
        MAIL_SENDER.send(mimeMessage);
    }

    /**
     * 将多个联系人转为列表，分隔符为逗号或者分号
     *
     * @param addresses 多个联系人，如果为空返回null
     * @return 联系人列表
     */
    private static List<String> splitAddress(String addresses) {
        if (CharSequenceUtil.isBlank(addresses)) {
            return new ArrayList<>(0);
        }
        List<String> result;
        if (CharSequenceUtil.contains(addresses, StringConstants.COMMA)) {
            result = CharSequenceUtil.splitTrim(addresses, StringConstants.COMMA);
        } else if (CharSequenceUtil.contains(addresses, StringConstants.SEMICOLON)) {
            result = CharSequenceUtil.splitTrim(addresses, StringConstants.SEMICOLON);
        } else {
            result = CollUtil.newArrayList(addresses);
        }
        return result;
    }

}
