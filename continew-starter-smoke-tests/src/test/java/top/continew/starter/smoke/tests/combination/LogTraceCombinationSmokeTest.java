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
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.filter.RequestContextFilter;

/**
 * log + trace 组合冒烟：日志切面与链路追踪上下文贯通。
 *
 * <p>trace 需显式开启（默认关闭），开启后 TraceResponseFilter 注册进 Servlet
 * 链；经真实 HTTP 请求验证 traceId 写入 MDC（log 的上下文贯通）且响应头可回传。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootTest(classes = LogTraceSmokeApp.class, properties = {
    "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,"
        + "org.redisson.spring.starter.RedissonAutoConfigurationV4,"
        + "top.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration,"
        + "top.continew.starter.auth.satoken.autoconfigure.SaTokenAutoConfiguration,"
        + "top.continew.starter.data.autoconfigure.MybatisPlusAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
        + "top.continew.starter.extension.tenant.autoconfigure.TenantAutoConfiguration,"
        + "top.continew.starter.extension.datapermission.autoconfigure.DataPermissionAutoConfiguration,"
        + "top.continew.starter.encrypt.field.autoconfigure.FieldEncryptAutoConfiguration,"
        + "top.continew.starter.storage.autoconfigure.StorageAutoConfiguration,"
        + "top.continew.starter.messaging.mail.autoconfigure.MailAutoConfiguration,"
        + "top.continew.starter.messaging.websocket.autoconfigure.WebSocketAutoConfiguration",
    "continew-starter.trace.enabled=true"},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DisplayName("log + trace 组合冒烟")
class LogTraceCombinationSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * TraceResponseFilter 应注册进 Servlet 过滤器链。
     */
    @Test
    @DisplayName("链路追踪过滤器注册")
    void shouldRegisterTraceResponseFilter() {
        assertThat(applicationContext.getBean("traceResponseFilter", FilterRegistrationBean.class))
            .isNotNull();
        assertThat(applicationContext.getBean(RequestContextFilter.class)).isNotNull();
    }

    /**
     * 真实 HTTP 请求的响应头应携带链路 ID（TraceResponseFilter 契约）。
     */
    @Test
    @DisplayName("HTTP 请求响应头贯通 traceId")
    void shouldPropagateTraceIdThroughHttpRequest() {
        ResponseEntity<String> response = restTemplate.getForEntity("/smoke/trace", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("traceId")).isNotBlank();
    }
}
