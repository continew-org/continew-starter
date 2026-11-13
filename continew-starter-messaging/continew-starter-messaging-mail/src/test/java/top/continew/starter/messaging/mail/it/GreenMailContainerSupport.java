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

import org.awaitility.Awaitility;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.test.support.ContainerImages;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SubjectTerm;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 邮件集成测试基座：整个 Failsafe JVM 共享一个真实 GreenMail standalone 容器。
 *
 * <p>容器启动采用幂等懒启动，由首个 Spring 上下文装配（@DynamicPropertySource）时触发，
 * 保证 Docker 缺失时类级条件跳过不会误启动容器；收信断言一律经 IMAP 探针直连服务端，
 * 与被测的 SMTP 发送链路相互独立。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
abstract class GreenMailContainerSupport {

    private static final GenericContainer<?> GREENMAIL = new GenericContainer<>(
        DockerImageName.parse(ContainerImages.GREENMAIL_IMAGE)).withExposedPorts(3025, 3465, 3110,
            3143, 8080);

    private static volatile boolean started;

    /**
     * SMTP 测试端口
     */
    private static final int SMTP_PORT = 3025;

    /**
     * SMTPS 测试端口
     */
    private static final int SMTPS_PORT = 3465;

    /**
     * IMAP 测试端口
     */
    private static final int IMAP_PORT = 3143;

    /**
     * 管理 REST API 端口
     */
    private static final int API_PORT = 8080;

    /**
     * 启动共享的真实 GreenMail 容器（幂等）。
     */
    static void ensureStarted() {
        if (!started) {
            synchronized (GreenMailContainerSupport.class) {
                if (!started) {
                    GREENMAIL.start();
                    started = true;
                }
            }
        }
    }

    /**
     * 获取 SMTP 服务器地址。
     *
     * @return 主机名
     */
    static String smtpHost() {
        return GREENMAIL.getHost();
    }

    /**
     * 获取映射后的 SMTP 端口。
     *
     * @return 端口
     */
    static Integer smtpPort() {
        return GREENMAIL.getMappedPort(SMTP_PORT);
    }

    /**
     * 获取映射后的 SMTPS 端口。
     *
     * @return 端口
     */
    static Integer smtpsPort() {
        return GREENMAIL.getMappedPort(SMTPS_PORT);
    }

    /**
     * 获取映射后的 IMAP 端口。
     *
     * @return 端口
     */
    static Integer imapPort() {
        return GREENMAIL.getMappedPort(IMAP_PORT);
    }

    /**
     * 经管理 REST API 创建邮箱用户（幂等创建前的唯一邮箱地址由各用例保证）。
     *
     * @param email    邮箱地址
     * @param login    登录名
     * @param password 密码
     */
    static void createUser(String email, String login, String password) {
        String body = "{\"email\":\"%s\",\"login\":\"%s\",\"password\":\"%s\"}".formatted(email,
            login, password);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + GREENMAIL.getHost() + ":" + GREENMAIL
                .getMappedPort(API_PORT) + "/api/user"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("GreenMail 用户创建失败: " + response.statusCode()
                    + " " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GreenMail 用户创建请求被中断", e);
        } catch (IOException e) {
            throw new IllegalStateException("GreenMail 用户创建请求失败", e);
        }
    }

    /**
     * 轮询等待 IMAP 探针在收件人邮箱中发现指定主题的邮件并返回副本。
     *
     * <p>探针在轮询期内可能遇到瞬时连接抖动，返回空集进入下一轮；超时即失败，
     * 不静默吞掉。</p>
     *
     * @param login    收件人登录名
     * @param password 收件人密码
     * @param subject  邮件主题
     * @return 服务端实际收到的邮件
     */
    static MimeMessage awaitMessage(String login, String password, String subject) {
        AtomicReference<MimeMessage> matched = new AtomicReference<>();
        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(200))
            .until(() -> {
                Optional<MimeMessage> found = findFirstBySubject(login, password, subject);
                found.ifPresent(matched::set);
                return found.isPresent();
            });
        return matched.get();
    }

    private static Optional<MimeMessage> findFirstBySubject(String login, String password,
        String subject) {
        Store store = null;
        Folder inbox = null;
        try {
            // 探针会话不加载任何配置，仅使用 jakarta.mail 默认 IMAP 提供者直连服务端
            Session session = Session.getInstance(new Properties());
            store = session.getStore("imap");
            store.connect(GREENMAIL.getHost(), GREENMAIL.getMappedPort(IMAP_PORT), login,
                password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            Message[] messages = inbox.search(new SubjectTerm(subject));
            if (messages.length == 0) {
                return Optional.empty();
            }
            // 拷贝副本，避免文件夹关闭后消息句柄失效
            return Optional.of(new MimeMessage((MimeMessage) messages[0]));
        } catch (MessagingException e) {
            return Optional.empty();
        } finally {
            closeQuietly(inbox, store);
        }
    }

    private static void closeQuietly(Folder inbox, Store store) {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null) {
                store.close();
            }
        } catch (MessagingException ignored) {
            // 探针资源清理失败不影响主断言链路
        }
    }
}
