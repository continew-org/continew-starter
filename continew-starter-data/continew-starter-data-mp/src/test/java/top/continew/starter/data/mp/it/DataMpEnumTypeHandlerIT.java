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

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.data.mp.it.entity.ItUser;
import top.continew.starter.data.mp.it.entity.ItUserStatus;
import top.continew.starter.data.mp.it.service.ItUserService;

import java.util.List;

/**
 * MyBatis Plus 枚举类型处理器集成测试：BaseEnum 枚举按 value 真实写入数据库并还原。
 *
 * <p>模块自动配置将默认枚举处理器设为 {@code CompositeBaseEnumTypeHandler}，对 BaseEnum
 * 委派 {@code MybatisBaseEnumTypeHandler}：写入侧取枚举 value，读取侧按列值还原枚举。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMpEnumTypeHandlerIT extends MySqlSupport {

    /**
     * 写入侧：实体枚举字段落库为枚举 value 数字而非枚举名。
     */
    @Test
    void shouldWriteEnumAsValue() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            ItUser user = user(1L, "单元一号", 10L, ItUserStatus.LOCKED);
            assertThat(service.save(user)).isTrue();
            assertThat(((Number) rawRow(1L).get("status")).intValue()).isEqualTo(2);
        });
    }

    /**
     * 读取侧：预先按 value 落库的行经默认枚举处理器还原为真实枚举常量。
     */
    @Test
    void shouldReadEnumFromColumnValue() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            jdbc()
                .update("INSERT INTO mp_it_user (id, username, nickname, dept_id, status, deleted) "
                    + "VALUES (1, 'junit_u1', '单元一号', 10, 1, 0), (2, 'junit_u2', '单元二号', 20, 2, 0)");
            ItUser enabled = service.getById(1L);
            assertThat(enabled.getStatus()).isEqualTo(ItUserStatus.ENABLED);
            ItUser locked = service.getById(2L);
            assertThat(locked.getStatus()).isEqualTo(ItUserStatus.LOCKED);
        });
    }

    /**
     * 条件侧：按枚举常量等值查询时绑定枚举 value，真实命中对应行。
     */
    @Test
    void shouldFilterByEnumValueInWhere() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            jdbc()
                .update("INSERT INTO mp_it_user (id, username, nickname, dept_id, status, deleted) "
                    + "VALUES (1, 'junit_u1', '单元一号', 10, 1, 0), (2, 'junit_u2', '单元二号', 20, 2, 0)");
            List<ItUser> matched =
                service.lambdaQuery().eq(ItUser::getStatus, ItUserStatus.LOCKED).list();
            assertThat(matched).hasSize(1);
            assertThat(matched.get(0).getId()).isEqualTo(2L);
        });
    }

    /**
     * 空值侧：null 枚举真实落库为 NULL 列，读取后保持 null 而非误还原。
     */
    @Test
    void shouldPreserveNullEnum() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            ItUser user = user(1L, "单元一号", 10L, null);
            assertThat(service.save(user)).isTrue();
            assertThat(rawRow(1L).get("status")).isNull();
            assertThat(service.getById(1L).getStatus()).isNull();
        });
    }

    private static ItUser user(Long id, String nickname, Long deptId, ItUserStatus status) {
        ItUser user = new ItUser();
        user.setId(id);
        user.setUsername("junit_u" + id);
        user.setNickname(nickname);
        user.setDeptId(deptId);
        user.setStatus(status);
        user.setDeleted(0);
        return user;
    }
}
