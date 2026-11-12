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

package top.continew.starter.extension.tenant.mp.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.extension.tenant.autoconfigure.TenantAutoConfiguration;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;
import top.continew.starter.extension.tenant.mp.it.controller.ItControllerService;
import top.continew.starter.extension.tenant.mp.it.entity.ItTenantUser;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantUserMapper;

import java.util.List;

/**
 * 数据源级隔离集成测试：DATASOURCE 隔离级关闭行级过滤，controller 包方法经真实 AOP
 * 切面执行数据源路由，缺省租户提供者装配失败。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class TenantDataSourceIsolationIT extends MySqlSupport {

    /**
     * 数据源级隔离：行级过滤关闭返回全量数据；无上下文时隔离级别兜底取配置默认行级。
     */
    @Test
    void shouldBypassLineFilterWhenDatasourceLevel() {
        this.runner().run(context -> {
            ItTenantUserMapper mapper = context.getBean(ItTenantUserMapper.class);
            executeUpdate(
                "INSERT INTO tenant_user (id, username, tenant_id) VALUES (1, 'junit_u1', 1), "
                    + "(2, 'junit_u2', 2)");
            applyTenant(1L, TenantIsolationLevel.DATASOURCE);

            List<ItTenantUser> rows = mapper.selectList(null);
            assertThat(rows).extracting(ItTenantUser::getId).containsExactlyInAnyOrder(1L, 2L);

            TenantContextHolder.clear();
            assertThat(TenantContextHolder.getIsolationLevel())
                .isEqualTo(TenantIsolationLevel.LINE);
        });
    }

    /**
     * 数据源路由切面：controller 包方法经真实 AOP 拦截，三种上下文分别走透传、行级透传
     * 与数据源切换路径，且路由退出后行级过滤恢复。
     */
    @Test
    void shouldRouteThroughDataSourceInterceptor() {
        this.runner().run(context -> {
            ItControllerService controllerService = context.getBean(ItControllerService.class);
            executeUpdate(
                "INSERT INTO tenant_user (id, username, tenant_id) VALUES (1, 'junit_u1', 1), "
                    + "(2, 'junit_u2', 2)");

            applyTenant(1L);
            List<ItTenantUser> lineRows = controllerService.listWithinTenant();
            assertThat(lineRows).extracting(ItTenantUser::getId).containsExactly(1L);

            applyTenant(DATASOURCE_TENANT_ID, TenantIsolationLevel.DATASOURCE);
            List<ItTenantUser> routedRows = controllerService.listWithinTenant();
            assertThat(routedRows).extracting(ItTenantUser::getId).containsExactlyInAnyOrder(1L,
                2L);

            applyTenant(1L, TenantIsolationLevel.DATASOURCE);
            TenantContextHolder.setIgnore(true);
            List<ItTenantUser> ignoredRows = controllerService.listWithinTenant();
            assertThat(ignoredRows).extracting(ItTenantUser::getId).containsExactlyInAnyOrder(1L,
                2L);
        });
    }

    /**
     * 缺省租户提供者：未提供扩展点时自动配置装配失败。
     */
    @Test
    void shouldFailWithoutTenantProvider() {
        ApplicationContextRunner bareRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class))
            .withUserConfiguration(MySqlSupport.BareConfig.class)
            .withBean(cn.hutool.extra.spring.SpringUtil.class);
        bareRunner.run(context -> assertThat(context).hasFailed());
    }
}
