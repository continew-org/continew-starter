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

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.data.mf.it.entity.ItUser;
import top.continew.starter.data.mf.it.service.ItUserService;

import java.util.Map;

/**
 * MyBatis Flex 通用业务实现集成测试：真实 MySQL 上的单条 CRUD、分页与逻辑删除。
 *
 * <p>断言直接核对真实表数据（列值、行数、逻辑删除标记位），不以实体回读自证。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMfServiceImplIT extends MySqlSupport {

    /**
     * 插入与按 ID 查询：插入真实落库、原始列值与实体回读一致，缺省记录返回空 Optional。
     */
    @Test
    void shouldInsertAndSelectById() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            ItUser user = user(1L, "junit_u1", "单元一号", 10L, 1L);
            assertThat(service.save(user)).isTrue();
            Map<String, Object> raw = rawUserRow(1L);
            assertThat(raw).containsEntry("username", "junit_u1");
            assertThat(((Number) raw.get("dept_id")).longValue()).isEqualTo(10L);
            assertThat(((Number) raw.get("deleted")).intValue()).isZero();

            ItUser loaded = service.getById(1L);
            assertThat(loaded.getUsername()).isEqualTo("junit_u1");
            assertThat(loaded.getDeptId()).isEqualTo(10L);
            assertThat(service.getByIdOpt(99L)).isEmpty();
            assertThat(service.count()).isEqualTo(1L);
        });
    }

    /**
     * 更新：updateById 真实改列，未设置字段保持原值。
     */
    @Test
    void shouldUpdateById() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            service.save(user(1L, "junit_u1", "单元一号", 10L, 1L));

            ItUser update = user(1L, "junit_u1", "单元一号改", 30L, 1L);
            assertThat(service.updateById(update)).isTrue();
            Map<String, Object> raw = rawUserRow(1L);
            assertThat(raw).containsEntry("nickname", "单元一号改");
            assertThat(((Number) raw.get("dept_id")).longValue()).isEqualTo(30L);
            assertThat(((Number) raw.get("create_user")).longValue()).isEqualTo(1L);
        });
    }

    /**
     * 分页：Flex 方言生成真实 LIMIT，总数与各页记录按排序条件确定，越界页返回空记录。
     */
    @Test
    void shouldPageWithDialectLimit() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserService service = context.getBean(ItUserService.class);
            for (long i = 1; i <= 5; i++) {
                service.save(user(i, "junit_u" + i, "单元" + i + "号", 10L + i, i));
            }
            QueryWrapper wrapper = QueryWrapper.create().orderBy("id", true);
            Page<ItUser> first = service.page(new Page<>(1, 2), wrapper);
            assertThat(first.getTotalRow()).isEqualTo(5L);
            assertThat(first.getTotalPage()).isEqualTo(3L);
            assertThat(first.getRecords()).hasSize(2);
            assertThat(first.getRecords().get(0).getId()).isEqualTo(1L);
            assertThat(first.getRecords().get(1).getId()).isEqualTo(2L);

            Page<ItUser> last = service.page(new Page<>(3, 2), wrapper);
            assertThat(last.getRecords()).hasSize(1);
            assertThat(last.getRecords().get(0).getId()).isEqualTo(5L);

            Page<ItUser> overflow = service.page(new Page<>(9, 2), wrapper);
            assertThat(overflow.getRecords()).isEmpty();
            assertThat(overflow.getTotalRow()).isEqualTo(5L);
        });
    }

    /**
     * 逻辑删除：删除后查询侧不可见、数据库行仍在且标记位为 1，行数扣除真实生效。
     */
    @Test
    void shouldRemoveByIdWithLogicDelete() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserService service = context.getBean(ItUserService.class);
            service.save(user(1L, "junit_u1", "单元一号", 10L, 1L));
            service.save(user(2L, "junit_u2", "单元二号", 20L, 2L));

            assertThat(service.removeById(1L)).isTrue();
            assertThat(service.getByIdOpt(1L)).isEmpty();
            assertThat(((Number) rawUserRow(1L).get("deleted")).intValue()).isEqualTo(1);
            assertThat(rawUserCount()).isEqualTo(2L);

            assertThat(service.removeById(2L)).isTrue();
            assertThat(((Number) rawUserRow(2L).get("deleted")).intValue()).isEqualTo(1);
            assertThat(service.count()).isZero();
        });
    }

    private static ItUser user(Long id, String username, String nickname, Long deptId,
        Long createUser) {
        ItUser user = new ItUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setDeptId(deptId);
        user.setCreateUser(createUser);
        user.setDeleted(0);
        return user;
    }
}
