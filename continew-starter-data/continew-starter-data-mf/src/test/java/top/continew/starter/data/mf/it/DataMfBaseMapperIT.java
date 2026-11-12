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

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.data.mf.it.entity.ItUser;
import top.continew.starter.data.mf.it.mapper.ItUserMapper;

import java.util.List;

/**
 * MyBatis Flex Mapper 扩展基类集成测试：批量更新、实体条件链式查询与实体类型解析。
 *
 * <p>批量更新默认方法经 Flex 的 {@code Db} 工具按全局配置解析真实会话工厂执行，
 * 断言直接核对真实表列值。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMfBaseMapperIT extends MySqlSupport {

    /**
     * Mapper 批量更新：默认方法经 Db 工具真实批量提交，各行列值全部生效。
     */
    @Test
    void shouldUpdateBatchByEntities() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            mapper.insert(user(1L, "junit_u1", "单元一号", 10L, 1L));
            mapper.insert(user(2L, "junit_u2", "单元二号", 20L, 2L));

            ItUser first = user(1L, "junit_u1", "单元一号改", 40L, 1L);
            ItUser second = user(2L, "junit_u2", "单元二号改", 50L, 2L);
            assertThat(mapper.updateBatchById(List.of(first, second))).isTrue();
            assertThat(((Number) rawUserRow(1L).get("dept_id")).longValue()).isEqualTo(40L);
            assertThat(rawUserRow(1L).get("nickname")).isEqualTo("单元一号改");
            assertThat(((Number) rawUserRow(2L).get("dept_id")).longValue()).isEqualTo(50L);
            assertThat(rawUserRow(2L).get("nickname")).isEqualTo("单元二号改");
        });
    }

    /**
     * 实体条件链式查询：非空属性生成等值条件，按真实数据过滤。
     */
    @Test
    void shouldQueryByEntityConditions() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            mapper.insert(user(1L, "junit_u1", "单元一号", 10L, 1L));
            mapper.insert(user(2L, "junit_u2", "单元二号", 10L, 2L));
            mapper.insert(user(3L, "junit_u3", "管理一号", 20L, 3L));

            ItUser condition = new ItUser();
            condition.setDeptId(10L);
            List<ItUser> matched = mapper.selectListByQuery(mapper.query(condition));
            assertThat(matched).extracting(ItUser::getId).containsExactly(1L, 2L);
        });
    }

    /**
     * 条件链式查询与实体类型解析：空条件构造器可继续拼接列条件，currentEntityClass
     * 经 Mapper 代理解析出真实实体类型。
     */
    @Test
    void shouldChainQueryAndResolveEntityClass() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            mapper.insert(user(1L, "junit_u1", "单元一号", 10L, 1L));
            mapper.insert(user(2L, "junit_u2", "单元二号", 20L, 2L));

            List<ItUser> matched = mapper.selectListByQuery(mapper.query().eq("dept_id", 20L));
            assertThat(matched).extracting(ItUser::getId).containsExactly(2L);
            assertThat(mapper.currentEntityClass()).isEqualTo(ItUser.class);
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
