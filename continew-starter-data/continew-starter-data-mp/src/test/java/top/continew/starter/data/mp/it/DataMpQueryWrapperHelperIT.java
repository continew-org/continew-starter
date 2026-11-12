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
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.LogicalRelation;
import top.continew.starter.data.enums.QueryType;
import top.continew.starter.data.mp.it.entity.ItUser;
import top.continew.starter.data.mp.it.service.ItUserService;
import top.continew.starter.data.util.QueryWrapperHelper;

import java.util.List;

/**
 * MyBatis Plus 查询条件助手集成测试：{@code QueryWrapperHelper} 构建的条件与排序在真实
 * MySQL 上生效。
 *
 * <p>种子数据中「一号」同时出现在第 1 行昵称与第 3 行用户名，多列 OR 与单列、
 * 多列 AND 的命中集合互不相同，可据此判定真实 SQL 逻辑关系。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMpQueryWrapperHelperIT extends MySqlSupport {

    /**
     * 单列条件：等值与模糊查询真实过滤数据行。
     */
    @Test
    void shouldBuildSingleColumnConditions() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            seedFourUsers();
            UserQuery deptQuery = new UserQuery();
            deptQuery.setDeptId(10L);
            List<ItUser> deptMatched = service.list(QueryWrapperHelper.build(deptQuery));
            assertThat(deptMatched).extracting(ItUser::getId).containsExactly(1L, 2L);

            UserQuery nicknameQuery = new UserQuery();
            nicknameQuery.setNickname("二号");
            List<ItUser> likeMatched = service.list(QueryWrapperHelper.build(nicknameQuery));
            assertThat(likeMatched).extracting(ItUser::getId).containsExactly(2L, 4L);
        });
    }

    /**
     * 多条件组合：IN 与 BETWEEN 条件以 AND 关系同时生效。
     */
    @Test
    void shouldBuildCombinedConditions() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            seedFourUsers();
            UserQuery query = new UserQuery();
            query.setIds(List.of(1L, 3L));
            query.setIdRange(List.of(2L, 4L));
            List<ItUser> matched = service.list(QueryWrapperHelper.build(query));
            assertThat(matched).extracting(ItUser::getId).containsExactly(3L);
        });
    }

    /**
     * 多列 OR：关键词命中任一列即返回，覆盖单列条件与 AND 关系无法命中的行。
     */
    @Test
    void shouldBuildMultiColumnOrLike() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            seedFourUsers();
            KeywordQuery query = new KeywordQuery();
            query.setKeyword("一号");
            List<ItUser> matched = service.list(QueryWrapperHelper.build(query));
            assertThat(matched).extracting(ItUser::getId).containsExactly(1L, 3L);
        });
    }

    /**
     * 排序：条件对象携带的排序真实生效，且注入式排序字段与非法标识符被拒绝。
     */
    @Test
    void shouldSortAndRejectIllegalSortProperty() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            seedFourUsers();
            UserQuery query = new UserQuery();
            List<ItUser> descById =
                service.list(QueryWrapperHelper.build(query, Sort.by(Sort.Order.desc("id"))));
            assertThat(descById).extracting(ItUser::getId).containsExactly(4L, 3L, 2L, 1L);

            QueryWrapper<ItUser> wrapper = new QueryWrapper<>();
            QueryWrapperHelper.sort(wrapper, Sort.by(Sort.Order.asc("deptId")));
            List<ItUser> ascByDept = service.list(wrapper);
            assertThat(ascByDept).extracting(ItUser::getId).containsExactly(1L, 2L, 3L, 4L);

            UserQuery injected = new UserQuery();
            assertThatThrownBy(
                () -> QueryWrapperHelper.build(injected, Sort.by("id; drop table mp_it_user")))
                .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> QueryWrapperHelper.sort(wrapper, Sort.by("bad prop")))
                .isInstanceOf(BadRequestException.class);
        });
    }

    /**
     * 预置四行种子数据：第 1、2 行属部门 10，第 3 行属部门 20，第 4 行属部门 30。
     */
    private static void seedFourUsers() {
        jdbc().update(
            "INSERT INTO mp_it_user (id, username, nickname, dept_id, status, deleted) VALUES "
                + "(1, 'junit_u1', '单元一号', 10, 1, 0), (2, 'junit_u2', '单元二号', 10, 2, 0), "
                + "(3, '一号用户', '管理一号', 20, 1, 0), (4, 'junit_u4', '管理二号', 30, 2, 0)");
    }

    /**
     * 单列查询条件对象
     */
    static class UserQuery {

        /**
         * 部门等值查询
         */
        @Query(type = QueryType.EQ)
        private Long deptId;
        /**
         * 昵称模糊查询
         */
        @Query(type = QueryType.LIKE)
        private String nickname;
        /**
         * 主键 IN 查询
         */
        @Query(type = QueryType.IN, columns = {"id"})
        private List<Long> ids;
        /**
         * 主键 BETWEEN 查询
         */
        @Query(type = QueryType.BETWEEN, columns = {"id"})
        private List<Long> idRange;

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }

        public List<Long> getIdRange() {
            return idRange;
        }

        public void setIdRange(List<Long> idRange) {
            this.idRange = idRange;
        }
    }

    /**
     * 多列 OR 模糊查询条件对象
     */
    static class KeywordQuery {

        /**
         * 关键词（用户名与昵称任一命中）
         */
        @Query(type = QueryType.LIKE, columns = {"username", "nickname"},
            logicalRelation = LogicalRelation.OR)
        private String keyword;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
    }
}
