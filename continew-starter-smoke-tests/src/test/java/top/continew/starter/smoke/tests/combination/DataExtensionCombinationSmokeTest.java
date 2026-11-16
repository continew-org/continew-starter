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

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import javax.sql.DataSource;

/**
 * data + extension 组合冒烟：mp 与 tenant/datapermission 装配互斥与共存。
 *
 * <p>H2 内存库满足 baomidou 装配的 {@code ConditionalOnSingleCandidate(DataSource)}
 * 门槛；断言 MybatisPlusInterceptor 将租户与数据权限两个 InnerInterceptor 一并
 * 收编（SpringUtil.getBeansOfType 收集），即两个扩展共存且互不覆盖。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootTest(classes = DataExtensionSmokeApp.class, properties = {
    "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,"
        + "org.redisson.spring.starter.RedissonAutoConfigurationV4,"
        + "top.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration,"
        + "top.continew.starter.auth.satoken.autoconfigure.SaTokenAutoConfiguration,"
        + "top.continew.starter.encrypt.field.autoconfigure.FieldEncryptAutoConfiguration,"
        + "top.continew.starter.log.autoconfigure.LogAutoConfiguration,"
        + "top.continew.starter.trace.autoconfigure.TraceAutoConfiguration,"
        + "top.continew.starter.storage.autoconfigure.StorageAutoConfiguration,"
        + "top.continew.starter.messaging.mail.autoconfigure.MailAutoConfiguration,"
        + "top.continew.starter.messaging.websocket.autoconfigure.WebSocketAutoConfiguration",
    "mybatis-plus.extension.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:smoke-data;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("data + extension 组合冒烟")
class DataExtensionCombinationSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 租户与数据权限拦截器应同时被 MybatisPlusInterceptor 收编。
     */
    @Test
    @DisplayName("租户与数据权限 InnerInterceptor 共存收编")
    void shouldCollectTenantAndDataPermissionInterceptors() {
        MybatisPlusInterceptor interceptor =
            applicationContext.getBean(MybatisPlusInterceptor.class);
        List<InnerInterceptor> innerInterceptors = interceptor.getInterceptors();
        assertThat(innerInterceptors).hasAtLeastOneElementOfType(TenantLineInnerInterceptor.class)
            .hasAtLeastOneElementOfType(DataPermissionInterceptor.class);
    }

    /**
     * MyBatis 核心装配应就绪（SqlSessionTemplate 由 baomidou 装配链产出）。
     */
    @Test
    @DisplayName("SqlSessionTemplate 装配就绪")
    void shouldAssembleSqlSessionTemplate() {
        assertThat(applicationContext.getBean(SqlSessionTemplate.class)).isNotNull();
        assertThat(applicationContext.getBean(DataSource.class)).isNotNull();
    }
}
