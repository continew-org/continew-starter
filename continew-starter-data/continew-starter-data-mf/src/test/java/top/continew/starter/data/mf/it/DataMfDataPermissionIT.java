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

package top.continew.starter.data.mf.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.data.datapermission.DataPermissionCurrentUser;
import top.continew.starter.data.datapermission.DataScope;
import top.continew.starter.data.mf.it.entity.ItUser;
import top.continew.starter.data.mf.it.service.ItUserService;

import java.util.List;
import java.util.Set;

/**
 * MyBatis Flex 数据权限方言集成测试：{@code @DataPermission} 注解经真实 AOP 切面与
 * {@code DataPermissionDialect} 改写为带权限条件的 SQL，在真实 MySQL 上过滤数据行。
 *
 * <p>种子数据：用户分属部门 10、10、20、30、11，部门 11 为部门 10 的子节点（ancestors
 * 含 10）；角色 R1 关联部门 10、20。各数据权限范围的期望行集互不相同，可据此判定真实
 * 改写语义。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMfDataPermissionIT extends MySqlSupport {

    /**
     * 过滤关闭或无注解上下文：方言不改写 SQL，返回全部数据行。
     */
    @Test
    void shouldNotRewriteWhenFilterDisabledOrNoAnnotation() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserService service = context.getBean(ItUserService.class);
            seedPermissionData();
            List<ItUser> all = service.listWithPermission(QueryWrapper.create().from(ItUser.class));
            assertThat(all).extracting(ItUser::getId).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);

            // 过滤开启但查询未经过 @DataPermission 方法，切面未注入注解上下文，SQL 同样不改写
            applyDataPermission(true, user("1", "10", role("R1", DataScope.SELF)));
            List<ItUser> unannotated = service.list(QueryWrapper.create().from(ItUser.class));
            assertThat(unannotated).extracting(ItUser::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
        });
    }

    /**
     * 本部门与本部门及以下：前者仅命中当前部门，后者经 find_in_set 子查询连同子部门行一并命中。
     */
    @Test
    void shouldRewriteDeptScopes() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserService service = context.getBean(ItUserService.class);
            seedPermissionData();
            applyDataPermission(true, user("1", "10", role("R1", DataScope.DEPT)));
            List<ItUser> deptRows =
                service.listWithPermission(QueryWrapper.create().from(ItUser.class));
            assertThat(deptRows).extracting(ItUser::getId).containsExactlyInAnyOrder(1L, 2L);

            applyDataPermission(true, user("1", "10", role("R1", DataScope.DEPT_AND_CHILD)));
            List<ItUser> deptAndChildRows = service.listWithPermission(QueryWrapper.create()
                .from(ItUser.class));
            assertThat(deptAndChildRows).extracting(ItUser::getId).containsExactlyInAnyOrder(1L, 2L,
                5L);
        });
    }

    /**
     * 仅本人：SQL 追加创建人等值条件，只命中本人创建的行。
     */
    @Test
    void shouldRewriteSelfScope() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserService service = context.getBean(ItUserService.class);
            seedPermissionData();
            applyDataPermission(true, user("1", "10", role("R1", DataScope.SELF)));
            List<ItUser> selfRows =
                service.listWithPermission(QueryWrapper.create().from(ItUser.class));
            assertThat(selfRows).extracting(ItUser::getId).containsExactly(1L);
        });
    }

    /**
     * 自定义与全部范围：自定义经角色部门关联子查询命中关联部门行，全部范围不做改写。
     */
    @Test
    void shouldRewriteCustomAndAllScopes() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserService service = context.getBean(ItUserService.class);
            seedPermissionData();
            applyDataPermission(true, user("1", "10", role("R1", DataScope.CUSTOM)));
            List<ItUser> customRows =
                service.listWithPermission(QueryWrapper.create().from(ItUser.class));
            assertThat(customRows).extracting(ItUser::getId).containsExactlyInAnyOrder(1L, 2L, 3L);

            applyDataPermission(true, user("1", "10", role("R1", DataScope.ALL)));
            List<ItUser> allRows =
                service.listWithPermission(QueryWrapper.create().from(ItUser.class));
            assertThat(allRows).extracting(ItUser::getId).containsExactlyInAnyOrder(1L, 2L, 3L, 4L,
                5L);
        });
    }

    /**
     * 预置数据权限种子数据：5 名用户、4 个部门（11 为 10 的子节点）、角色 R1 关联部门 10 与 20。
     */
    private static void seedPermissionData() {
        jdbc().update(
            "INSERT INTO mf_it_user (id, username, nickname, dept_id, create_user, deleted) VALUES "
                + "(1, 'junit_u1', '单元一号', 10, 1, 0), (2, 'junit_u2', '单元二号', 10, 2, 0), "
                + "(3, 'junit_u3', '管理一号', 20, 3, 0), (4, 'junit_u4', '管理二号', 30, 4, 0), "
                + "(5, 'junit_u5', '子部门号', 11, 5, 0)");
        jdbc().update(
            "INSERT INTO mf_it_dept (id, ancestors) VALUES (10, '0'), (11, '0,10'), (20, '0'), "
                + "(30, '0')");
        jdbc()
            .update("INSERT INTO mf_it_role_dept (role_id, dept_id) VALUES ('R1', 10), ('R1', 20)");
    }

    private static DataPermissionCurrentUser user(String userId, String deptId,
        DataPermissionCurrentUser.CurrentUserRole... roles) {
        DataPermissionCurrentUser user = new DataPermissionCurrentUser();
        user.setUserId(userId);
        user.setDeptId(deptId);
        user.setRoles(Set.of(roles));
        return user;
    }

    private static DataPermissionCurrentUser.CurrentUserRole role(String roleId,
        DataScope dataScope) {
        return new DataPermissionCurrentUser.CurrentUserRole(roleId, dataScope);
    }
}
