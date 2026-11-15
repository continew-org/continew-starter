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

package top.continew.starter.smoke.tests.combination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import top.continew.starter.messaging.websocket.core.DefaultWebSocketHandler;
import top.continew.starter.storage.core.FileStorageService;

/**
 * messaging + storage 组合冒烟：mail/websocket 装配与本地存储共存。
 *
 * <p>mail 只装配 Sender 模板不发起连接；websocket 注册 handler 与端点；
 * storage 配置本地目录策略——三者共存启动且核心 Bean 就绪。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootTest(classes = MessagingStorageSmokeApp.class, properties = {
    "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,"
        + "org.redisson.spring.starter.RedissonAutoConfigurationV4,"
        + "top.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration,"
        + "top.continew.starter.auth.satoken.autoconfigure.SaTokenAutoConfiguration,"
        + "top.continew.starter.data.autoconfigure.MybatisPlusAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
        + "top.continew.starter.extension.tenant.autoconfigure.TenantAutoConfiguration,"
        + "top.continew.starter.extension.datapermission.autoconfigure.DataPermissionAutoConfiguration,"
        + "top.continew.starter.encrypt.field.autoconfigure.FieldEncryptAutoConfiguration,"
        + "top.continew.starter.log.autoconfigure.LogAutoConfiguration,"
        + "top.continew.starter.trace.autoconfigure.TraceAutoConfiguration",
    "continew-starter.storage.local[0].enabled=true",
    "continew-starter.storage.local[0].platform=local",
    "continew-starter.storage.local[0].bucketName=smoke",
    "continew-starter.storage.local[0].endpoint=${java.io.tmpdir}/smoke-storage"},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("messaging + storage 组合冒烟")
class MessagingStorageCombinationSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * mail/websocket/storage 三方核心 Bean 应共存就绪。
     */
    @Test
    @DisplayName("三方共存核心 Bean 就绪")
    void shouldAssembleMessagingAndStorageTogether() {
        assertThat(applicationContext.getBean(JavaMailSenderImpl.class)).isNotNull();
        assertThat(applicationContext.getBean(DefaultWebSocketHandler.class)).isNotNull();
        assertThat(applicationContext.getBean(WebSocketConfigurer.class)).isNotNull();
        assertThat(applicationContext.getBean(FileStorageService.class)).isNotNull();
    }
}
