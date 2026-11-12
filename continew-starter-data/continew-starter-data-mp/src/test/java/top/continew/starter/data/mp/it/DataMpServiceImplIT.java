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

package top.continew.starter.data.mp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.mp.it.entity.ItUser;
import top.continew.starter.data.mp.it.entity.ItUserStatus;
import top.continew.starter.data.mp.it.service.ItUserService;

import java.util.Map;
import java.util.Optional;

/**
 * MyBatis Plus 通用业务实现集成测试：真实 MySQL 上的单条 CRUD、分页与逻辑删除。
 *
 * <p>断言直接核对真实表数据（列值、行数、逻辑删除标记位），不以实体回读自证。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMpServiceImplIT extends MySqlSupport {

    /**
     * 插入与按 ID 查询：插入真实落库、原始列值与实体回读一致；saveOrUpdate 对无主键记录走真实插入分支。
     */
    @Test
    void shouldInsertAndSelectById() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            ItUser user = user(1L, "junit_u1", "单元一号", 10L, ItUserStatus.ENABLED);
            assertThat(service.save(user)).isTrue();
            Map<String, Object> raw = rawRow(1L);
            assertThat(raw).containsEntry("username", "junit_u1");
            assertThat(((Number) raw.get("dept_id")).longValue()).isEqualTo(10L);
            assertThat(((Number) raw.get("status")).intValue()).isEqualTo(1);
            assertThat(((Number) raw.get("deleted")).intValue()).isZero();

            ItUser loaded = service.getById(1L);
            assertThat(loaded.getUsername()).isEqualTo("junit_u1");
            assertThat(loaded.getStatus()).isEqualTo(ItUserStatus.ENABLED);
            assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(BusinessException.class);

            ItUser fresh = user(2L, "junit_u2", "单元二号", 20L, ItUserStatus.DISABLED);
            assertThat(service.saveOrUpdate(fresh)).isTrue();
            assertThat(rawCount()).isEqualTo(2L);
        });
    }

    /**
     * 更新与条件查询：updateById 真实改列，saveOrUpdate 对已有主键走真实更新分支，
     * getOneOpt/getMap/getObj 返回数据库真实结果。
     */
    @Test
    void shouldUpdateAndQueryByWrapper() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            service.save(user(1L, "junit_u1", "单元一号", 10L, ItUserStatus.ENABLED));
            service.save(user(2L, "junit_u2", "单元二号", 10L, ItUserStatus.DISABLED));

            ItUser update = user(1L, "junit_u1", "单元一号改", 30L, ItUserStatus.LOCKED);
            assertThat(service.updateById(update)).isTrue();
            Map<String, Object> raw = rawRow(1L);
            assertThat(raw).containsEntry("nickname", "单元一号改");
            assertThat(((Number) raw.get("dept_id")).longValue()).isEqualTo(30L);
            assertThat(((Number) raw.get("status")).intValue()).isEqualTo(2);

            ItUser renamed = user(1L, "junit_u1", "单元一号再改", 30L, ItUserStatus.LOCKED);
            assertThat(service.saveOrUpdate(renamed)).isTrue();
            assertThat(rawRow(1L)).containsEntry("nickname", "单元一号再改");

            Optional<ItUser> one =
                service.getOneOpt(new QueryWrapper<ItUser>().eq("dept_id", 30L), false);
            assertThat(one).isPresent();
            assertThat(one.get().getId()).isEqualTo(1L);
            Map<String, Object> row = service.getMap(new QueryWrapper<ItUser>().eq("id", 2L));
            assertThat(row).containsEntry("username", "junit_u2");
            Long count =
                service.getObj(new QueryWrapper<ItUser>().select("count(*)"), obj -> ((Number) obj)
                    .longValue());
            assertThat(count).isEqualTo(2L);
        });
    }

    /**
     * 分页：分页拦截器生成真实 LIMIT，总数与各页记录按排序条件确定，越界页返回空记录。
     */
    @Test
    void shouldPageWithPaginationInterceptor() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            for (long i = 1; i <= 5; i++) {
                service.save(user(i, "junit_u" + i, "单元" + i + "号", 10L + i, ItUserStatus.ENABLED));
            }
            QueryWrapper<ItUser> wrapper = new QueryWrapper<ItUser>().orderByAsc("id");
            Page<ItUser> first = service.page(new Page<>(1, 2), wrapper);
            assertThat(first.getTotal()).isEqualTo(5L);
            assertThat(first.getPages()).isEqualTo(3L);
            assertThat(first.getRecords()).hasSize(2);
            assertThat(first.getRecords().get(0).getId()).isEqualTo(1L);
            assertThat(first.getRecords().get(1).getId()).isEqualTo(2L);

            Page<ItUser> last = service.page(new Page<>(3, 2), wrapper);
            assertThat(last.getRecords()).hasSize(1);
            assertThat(last.getRecords().get(0).getId()).isEqualTo(5L);

            Page<ItUser> overflow = service.page(new Page<>(9, 2), wrapper);
            assertThat(overflow.getRecords()).isEmpty();
            assertThat(overflow.getTotal()).isEqualTo(5L);
        });
    }

    /**
     * 逻辑删除：删除后查询侧不可见、数据库行仍在且标记位为 1；useFill 变体同样真实落标记。
     */
    @Test
    void shouldRemoveByIdWithLogicDelete() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            service.save(user(1L, "junit_u1", "单元一号", 10L, ItUserStatus.ENABLED));
            service.save(user(2L, "junit_u2", "单元二号", 20L, ItUserStatus.ENABLED));

            assertThat(service.removeById(1L)).isTrue();
            assertThat(service.getBaseMapper().selectById(1L)).isNull();
            assertThat(((Number) rawRow(1L).get("deleted")).intValue()).isEqualTo(1);
            assertThat(rawCount()).isEqualTo(2L);

            assertThat(service.removeById(2L, true)).isTrue();
            assertThat(service.getBaseMapper().selectById(2L)).isNull();
            assertThat(((Number) rawRow(2L).get("deleted")).intValue()).isEqualTo(1);
        });
    }

    private static ItUser user(Long id, String username, String nickname, Long deptId,
        ItUserStatus status) {
        ItUser user = new ItUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setDeptId(deptId);
        user.setStatus(status);
        user.setDeleted(0);
        return user;
    }
}
