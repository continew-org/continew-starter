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
import top.continew.starter.encrypt.field.interceptor.MyBatisDecryptInterceptor;
import top.continew.starter.encrypt.field.interceptor.MyBatisEncryptInterceptor;

/**
 * encrypt + web 组合冒烟：field 加密拦截器与 web 全局响应共存启动。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootTest(classes = EncryptWebSmokeApp.class, properties = {
    "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,"
        + "org.redisson.spring.starter.RedissonAutoConfigurationV4,"
        + "top.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration,"
        + "top.continew.starter.auth.satoken.autoconfigure.SaTokenAutoConfiguration,"
        + "top.continew.starter.data.autoconfigure.MybatisPlusAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
        + "top.continew.starter.extension.tenant.autoconfigure.TenantAutoConfiguration,"
        + "top.continew.starter.extension.datapermission.autoconfigure.DataPermissionAutoConfiguration,"
        + "top.continew.starter.log.autoconfigure.LogAutoConfiguration,"
        + "top.continew.starter.trace.autoconfigure.TraceAutoConfiguration,"
        + "top.continew.starter.storage.autoconfigure.StorageAutoConfiguration,"
        + "top.continew.starter.messaging.mail.autoconfigure.MailAutoConfiguration,"
        + "top.continew.starter.messaging.websocket.autoconfigure.WebSocketAutoConfiguration",
    "continew-starter.encrypt.field.password=sm0ke-aes-key-16"},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("encrypt + web 组合冒烟")
class EncryptWebCombinationSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * field 加密的加解密拦截器应装配就绪（无 mybatis 环境下仅注册为 Bean）。
     */
    @Test
    @DisplayName("加解密拦截器装配就绪")
    void shouldAssembleFieldEncryptInterceptors() {
        assertThat(applicationContext.getBean(MyBatisEncryptInterceptor.class)).isNotNull();
        assertThat(applicationContext.getBean(MyBatisDecryptInterceptor.class)).isNotNull();
    }
}
