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
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.mp.it.entity.ItTenantConfig;
import top.continew.starter.extension.tenant.mp.it.entity.ItTenantUser;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantConfigMapper;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantUserMapper;

import java.util.List;
import java.util.Map;

/**
 * 租户行级隔离集成测试：真实 MySQL 上验证租户 ID 注入 INSERT/SELECT/UPDATE/DELETE 的
 * SQL 效果与配置忽略表。
 *
 * <p>实体不含租户列字段，INSERT 的 tenant_id 列值由 {@code TenantLineInnerInterceptor}
 * 真实注入，断言直接核对真实表行与行级隔离效果。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class TenantLineIsolationIT extends MySqlSupport {

    /**
     * 插入与查询：租户列由拦截器真实注入落库，行级过滤仅命中当前租户数据。
     */
    @Test
    void shouldInjectTenantIdOnInsertAndSelect() {
        this.runner().run(context -> {
            ItTenantUserMapper mapper = context.getBean(ItTenantUserMapper.class);
            applyTenant(1L);
            executeUpdate(
                "INSERT INTO tenant_user (id, username, tenant_id) VALUES (2, 'junit_u2', 2)");
            assertThat(mapper.insert(user(1L, "junit_u1"))).isEqualTo(1);
            Map<String, Object> raw = rawUserRow(1L);
            assertThat(((Number) raw.get("tenant_id")).longValue()).isEqualTo(1L);

            List<ItTenantUser> rows = mapper.selectList(null);
            assertThat(rows).extracting(ItTenantUser::getId).containsExactly(1L);
        });
    }

    /**
     * 更新与删除：语句自动追加租户条件，跨租户行不受影响。
     */
    @Test
    void shouldInjectTenantIdOnUpdateAndDelete() {
        this.runner().run(context -> {
            ItTenantUserMapper mapper = context.getBean(ItTenantUserMapper.class);
            applyTenant(1L);
            executeUpdate(
                "INSERT INTO tenant_user (id, username, tenant_id) VALUES (1, 'junit_u1', 1), "
                    + "(2, 'junit_u2', 2)");

            ItTenantUser rename = user(1L, "junit_u1改");
            assertThat(mapper.updateById(rename)).isEqualTo(1);
            assertThat(rawUserRow(1L).get("username")).isEqualTo("junit_u1改");
            assertThat(rawUserRow(2L).get("username")).isEqualTo("junit_u2");

            assertThat(mapper.deleteById(2L)).isZero();
            assertThat(rawUserRow(2L)).isNotEmpty();
            assertThat(mapper.deleteById(1L)).isEqualTo(1);
            assertThat(rawUserRow(1L)).isEmpty();
        });
    }

    /**
     * 忽略表：配置忽略的表不注入租户列也不追加租户条件（该表无 tenant_id 列，注入即报错）。
     */
    @Test
    void shouldIgnoreConfiguredTable() {
        this.runner().run(context -> {
            assertThat(context.getBean(TenantProperties.class).getIgnoreTables())
                .containsExactly("tenant_config");
            ItTenantConfigMapper mapper = context.getBean(ItTenantConfigMapper.class);
            applyTenant(1L);
            executeUpdate("INSERT INTO tenant_config (id, config_key, config_value) VALUES "
                + "(1, 'key1', 'v1'), (2, 'key2', 'v2')");

            ItTenantConfig config = new ItTenantConfig();
            config.setId(3L);
            config.setConfigKey("key3");
            config.setConfigValue("v3");
            assertThat(mapper.insert(config)).isEqualTo(1);
            assertThat(queryForList("SELECT * FROM tenant_config")).hasSize(3);

            List<ItTenantConfig> rows = mapper.selectList(null);
            assertThat(rows).extracting(ItTenantConfig::getId).containsExactly(1L, 2L, 3L);
        });
    }

    private static ItTenantUser user(Long id, String username) {
        ItTenantUser user = new ItTenantUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
