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
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.extension.tenant.mp.it.entity.ItTenantUser;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantUserMapper;
import top.continew.starter.extension.tenant.mp.it.service.ItIgnoreService;
import top.continew.starter.extension.tenant.util.TenantUtils;

import java.util.List;

/**
 * 租户忽略与切换链路集成测试：真实 MySQL 上验证 {@code @TenantIgnore} 切面、
 * {@code TenantUtils.executeIgnore} 与 {@code TenantUtils.execute} 租户切换/恢复。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class TenantIgnoreAndSwitchIT extends MySqlSupport {

    /**
     * 注解忽略：经真实 AOP 切面进入忽略态返回全量数据，切面退出后恢复行级隔离；
     * 已处于忽略态时直接透传执行。
     */
    @Test
    void shouldIgnoreViaTenantIgnoreAspect() {
        this.runner().run(context -> {
            ItIgnoreService ignoreService = context.getBean(ItIgnoreService.class);
            seedTwoTenants();
            applyTenant(1L);

            List<ItTenantUser> ignoredRows = ignoreService.listAllIgnoringTenant();
            assertThat(ignoredRows).extracting(ItTenantUser::getId).containsExactlyInAnyOrder(1L,
                2L);
            List<ItTenantUser> tenantRows = ignoreService.listWithinTenant();
            assertThat(tenantRows).extracting(ItTenantUser::getId).containsExactly(1L);

            java.util.concurrent.atomic.AtomicReference<List<ItTenantUser>> nestedHolder =
                new java.util.concurrent.atomic.AtomicReference<>();
            TenantUtils
                .executeIgnore(() -> nestedHolder.set(ignoreService.listAllIgnoringTenant()));
            assertThat(nestedHolder.get()).extracting(ItTenantUser::getId)
                .containsExactlyInAnyOrder(1L,
                    2L);
        });
    }

    /**
     * 工具忽略：executeIgnore 内部查询不受租户条件约束，退出后恢复。
     */
    @Test
    void shouldIgnoreViaExecuteIgnore() {
        this.runner().run(context -> {
            ItTenantUserMapper mapper = context.getBean(ItTenantUserMapper.class);
            seedTwoTenants();
            applyTenant(1L);

            java.util.concurrent.atomic.AtomicReference<List<ItTenantUser>> ignoredHolder =
                new java.util.concurrent.atomic.AtomicReference<>();
            TenantUtils.executeIgnore(() -> ignoredHolder.set(mapper.selectList(null)));
            assertThat(ignoredHolder.get()).extracting(ItTenantUser::getId)
                .containsExactlyInAnyOrder(1L,
                    2L);
            List<ItTenantUser> restoredRows = mapper.selectList(null);
            assertThat(restoredRows).extracting(ItTenantUser::getId).containsExactly(1L);
        });
    }

    /**
     * 租户切换与恢复：execute 按提供者切换租户上下文并在退出后恢复原上下文；
     * 数据源级租户切换时真实执行数据源路由（master 池）。
     */
    @Test
    void shouldSwitchTenantAndRestore() {
        this.runner().run(context -> {
            ItTenantUserMapper mapper = context.getBean(ItTenantUserMapper.class);
            seedTwoTenants();
            applyTenant(1L);

            TenantUtils.execute(2L, () -> {
                List<ItTenantUser> switchedRows = mapper.selectList(null);
                assertThat(switchedRows).extracting(ItTenantUser::getId).containsExactly(2L);
                ItTenantUser fresh = new ItTenantUser();
                fresh.setId(3L);
                fresh.setUsername("junit_u3");
                assertThat(mapper.insert(fresh)).isEqualTo(1);
            });
            assertThat(rawUserRow(3L)).isNotEmpty();
            List<ItTenantUser> restoredRows = mapper.selectList(null);
            assertThat(restoredRows).extracting(ItTenantUser::getId).containsExactly(1L);

            TenantUtils.execute(DATASOURCE_TENANT_ID, () -> {
                List<ItTenantUser> routedRows = mapper.selectList(null);
                assertThat(routedRows).extracting(ItTenantUser::getId).containsExactlyInAnyOrder(1L,
                    2L, 3L);
            });
        });
    }

    /**
     * 预置两个租户的行级隔离种子数据：ID 1 属租户 1，ID 2 属租户 2。
     */
    private static void seedTwoTenants() {
        executeUpdate(
            "INSERT INTO tenant_user (id, username, tenant_id) VALUES (1, 'junit_u1', 1), "
                + "(2, 'junit_u2', 2)");
    }
}
