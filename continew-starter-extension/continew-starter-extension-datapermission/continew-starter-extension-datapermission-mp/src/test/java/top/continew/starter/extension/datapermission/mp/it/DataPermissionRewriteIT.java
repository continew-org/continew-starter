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

package top.continew.starter.extension.datapermission.mp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.exception.DataPermissionException;
import top.continew.starter.extension.datapermission.model.RoleData;
import top.continew.starter.extension.datapermission.model.UserData;
import top.continew.starter.extension.datapermission.mp.it.entity.ItDpUser;
import top.continew.starter.extension.datapermission.mp.it.mapper.ItDpUserMapper;

import java.util.List;
import java.util.Set;

/**
 * 数据权限 MP 扩展集成测试：{@code @DataPermission} 注解经真实 MySQL 上的
 * {@code DefaultDataPermissionHandler} 改写为带权限条件的 SQL 并过滤数据行。
 *
 * <p>种子数据：用户分属部门 10、10、20、30、11，部门 11 为部门 10 的子节点（ancestors
 * 含 10）；角色 100 关联部门 10、20；第 3 行创建人为 3。各数据权限范围的期望行集互不相同，
 * 可据此判定真实改写语义。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataPermissionRewriteIT extends MySqlSupport {

    /**
     * 过滤关闭或方法无注解：语句保持原样，返回全部数据行。
     */
    @Test
    void shouldNotRewriteWithoutAnnotationOrFilter() {
        this.runner().run(context -> {
            ItDpUserMapper mapper = context.getBean(ItDpUserMapper.class);
            seedPermissionData();
            applyUserData(true, new UserData(1L, 10L, Set.of(new RoleData(100L, DataScope.SELF))));
            List<ItDpUser> unannotated = mapper.selectUnrestrictedUsers();
            assertThat(unannotated).extracting(ItDpUser::getId).containsExactlyInAnyOrder(1L, 2L,
                3L, 4L, 5L);

            applyUserData(false, new UserData(1L, 10L, Set.of(new RoleData(100L, DataScope.SELF))));
            List<ItDpUser> filterDisabled = mapper.selectPermissionUsers();
            assertThat(filterDisabled).extracting(ItDpUser::getId).containsExactlyInAnyOrder(1L, 2L,
                3L, 4L, 5L);
        });
    }

    /**
     * 本部门与本部门及以下：前者仅命中当前部门，后者经 find_in_set 子查询（真实 MySQL 数据库
     * 类型检测）连同子部门行一并命中。
     */
    @Test
    void shouldRewriteDeptScopes() {
        this.runner().run(context -> {
            ItDpUserMapper mapper = context.getBean(ItDpUserMapper.class);
            seedPermissionData();
            applyUserData(true, new UserData(1L, 10L, Set.of(new RoleData(100L, DataScope.DEPT))));
            List<ItDpUser> deptRows = mapper.selectPermissionUsers();
            assertThat(deptRows).extracting(ItDpUser::getId).containsExactlyInAnyOrder(1L, 2L);

            applyUserData(true,
                new UserData(1L, 10L, Set.of(new RoleData(100L, DataScope.DEPT_AND_CHILD))));
            List<ItDpUser> deptAndChildRows = mapper.selectPermissionUsers();
            assertThat(deptAndChildRows).extracting(ItDpUser::getId).containsExactlyInAnyOrder(1L,
                2L, 5L);
        });
    }

    /**
     * 多角色组合：部门与仅本人两个范围以 OR 关系合并命中。
     */
    @Test
    void shouldRewriteMultiRoleOrCombination() {
        this.runner().run(context -> {
            ItDpUserMapper mapper = context.getBean(ItDpUserMapper.class);
            seedPermissionData();
            applyUserData(true, new UserData(3L, 30L,
                Set.of(new RoleData(100L, DataScope.DEPT), new RoleData(101L, DataScope.SELF))));
            List<ItDpUser> combined = mapper.selectPermissionUsers();
            assertThat(combined).extracting(ItDpUser::getId).containsExactlyInAnyOrder(3L, 4L);
        });
    }

    /**
     * 自定义与全部范围：自定义经角色部门关联子查询命中关联部门行，全部范围不做改写。
     */
    @Test
    void shouldRewriteCustomAndAllScopes() {
        this.runner().run(context -> {
            ItDpUserMapper mapper = context.getBean(ItDpUserMapper.class);
            seedPermissionData();
            applyUserData(true,
                new UserData(1L, 10L, Set.of(new RoleData(100L, DataScope.CUSTOM))));
            List<ItDpUser> customRows = mapper.selectPermissionUsers();
            assertThat(customRows).extracting(ItDpUser::getId).containsExactlyInAnyOrder(1L, 2L,
                3L);

            applyUserData(true, new UserData(1L, 10L, Set.of(new RoleData(100L, DataScope.ALL))));
            List<ItDpUser> allRows = mapper.selectPermissionUsers();
            assertThat(allRows).extracting(ItDpUser::getId).containsExactlyInAnyOrder(1L, 2L, 3L,
                4L, 5L);
        });
    }

    /**
     * 用户数据无效：构建数据范围前校验失败并抛出数据权限异常，语句不执行。
     */
    @Test
    void shouldThrowOnInvalidUserData() {
        this.runner().run(context -> {
            ItDpUserMapper mapper = context.getBean(ItDpUserMapper.class);
            seedPermissionData();
            applyUserData(true, new UserData(1L, null, Set.of(new RoleData(100L, DataScope.DEPT))));
            assertThatThrownBy(mapper::selectPermissionUsers)
                .hasRootCauseInstanceOf(DataPermissionException.class);
        });
    }

    /**
     * 预置数据权限种子数据：5 名用户、4 个部门（11 为 10 的子节点）、角色 100 关联部门 10 与 20。
     */
    private static void seedPermissionData() {
        executeUpdate("INSERT INTO dp_it_user (id, username, dept_id, create_user) VALUES "
            + "(1, 'junit_u1', 10, 1), (2, 'junit_u2', 10, 2), (3, 'junit_u3', 20, 3), "
            + "(4, 'junit_u4', 30, 4), (5, 'junit_u5', 11, 5)");
        executeUpdate(
            "INSERT INTO dp_it_dept (id, ancestors) VALUES (10, '0'), (11, '0,10'), (20, '0'), (30, '0')");
        executeUpdate("INSERT INTO dp_it_role_dept (role_id, dept_id) VALUES (100, 10), (100, 20)");
    }
}
