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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.LogicalRelation;
import top.continew.starter.data.enums.QueryType;
import top.continew.starter.data.mf.it.entity.ItUser;
import top.continew.starter.data.mf.it.mapper.ItUserMapper;
import top.continew.starter.data.util.QueryWrapperHelper;

import java.util.List;

/**
 * MyBatis Flex 查询条件助手集成测试：{@code QueryWrapperHelper} 构建的条件与排序在真实
 * MySQL 上生效。
 *
 * <p>种子数据中「一号」出现在第 1 行昵称与第 3 行用户名和昵称，据此可区分单列、
 * 多列 OR 与多列 AND 的真实命中集合。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMfQueryWrapperHelperIT extends MySqlSupport {

    /**
     * 单列条件：等值与模糊查询真实过滤数据行。
     */
    @Test
    void shouldBuildSingleColumnConditions() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            seedFourUsers();
            UserQuery deptQuery = new UserQuery();
            deptQuery.setDeptId(10L);
            List<ItUser> deptMatched =
                mapper.selectListByQuery(QueryWrapperHelper.build(deptQuery));
            assertThat(deptMatched).extracting(ItUser::getId).containsExactly(1L, 2L);

            UserQuery nicknameQuery = new UserQuery();
            nicknameQuery.setNickname("二号");
            List<ItUser> likeMatched =
                mapper.selectListByQuery(QueryWrapperHelper.build(nicknameQuery));
            assertThat(likeMatched).extracting(ItUser::getId).containsExactly(2L, 4L);
        });
    }

    /**
     * 多条件组合：IN 与 BETWEEN 条件以 AND 关系同时生效。
     */
    @Test
    void shouldBuildCombinedConditions() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            seedFourUsers();
            UserQuery query = new UserQuery();
            query.setIds(List.of(1L, 3L));
            query.setIdRange(List.of(2L, 4L));
            List<ItUser> matched = mapper.selectListByQuery(QueryWrapperHelper.build(query));
            assertThat(matched).extracting(ItUser::getId).containsExactly(3L);
        });
    }

    /**
     * 多列 OR：关键词命中用户名或昵称任一列即返回，覆盖仅单列无法命中的第 1 行。
     */
    @Test
    void shouldBuildMultiColumnConditions() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            seedFourUsers();
            KeywordQuery query = new KeywordQuery();
            query.setKeyword("一号");
            List<ItUser> matched = mapper.selectListByQuery(QueryWrapperHelper.build(query));
            assertThat(matched).extracting(ItUser::getId).containsExactly(1L, 3L);
        });
    }

    /**
     * 多列 AND：显式声明 AND 时同一查询值须同时命中用户名与昵称，仅第 3 行满足。
     */
    @Test
    void shouldBuildMultiColumnAndConditions() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            seedFourUsers();
            AndKeywordQuery query = new AndKeywordQuery();
            query.setKeyword("一号");
            List<ItUser> matched = mapper.selectListByQuery(QueryWrapperHelper.build(query));
            assertThat(matched).extracting(ItUser::getId).containsExactly(3L);
        });
    }

    /**
     * 排序：条件对象携带的排序真实生效，且注入式排序字段与非法标识符被拒绝。
     */
    @Test
    void shouldSortAndRejectIllegalSortProperty() {
        this.runner().run(context -> {
            useMysqlDialect();
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            seedFourUsers();
            UserQuery query = new UserQuery();
            List<ItUser> descById =
                mapper.selectListByQuery(QueryWrapperHelper.build(query, Sort.by(Sort.Order
                    .desc("id"))));
            assertThat(descById).extracting(ItUser::getId).containsExactly(4L, 3L, 2L, 1L);

            UserQuery injected = new UserQuery();
            Sort injectedSort = Sort.by("id; drop table mf_it_user");
            assertThatThrownBy(() -> QueryWrapperHelper.build(injected, injectedSort))
                .isInstanceOf(BadRequestException.class);
            Sort badSort = Sort.by("bad prop");
            assertThatThrownBy(() -> QueryWrapperHelper.build(injected, badSort))
                .isInstanceOf(BadRequestException.class);
        });
    }

    /**
     * 预置四行种子数据：第 1、2 行属部门 10，第 3 行属部门 20，第 4 行属部门 30。
     */
    private static void seedFourUsers() {
        jdbc().update(
            "INSERT INTO mf_it_user (id, username, nickname, dept_id, create_user, deleted) VALUES "
                + "(1, 'junit_u1', '单元一号', 10, 1, 0), (2, 'junit_u2', '单元二号', 10, 2, 0), "
                + "(3, '一号用户', '管理一号', 20, 3, 0), (4, 'junit_u4', '管理二号', 30, 4, 0)");
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
     * 多列 OR 模糊查询条件对象（显式标注默认值，锁定公开契约）
     */
    static class KeywordQuery {

        /**
         * 关键词（用户名或昵称任一命中）
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

    /**
     * 多列 AND 模糊查询条件对象
     */
    static class AndKeywordQuery {

        /**
         * 关键词（用户名与昵称须同时命中）
         */
        @Query(type = QueryType.LIKE, columns = {"username", "nickname"},
            logicalRelation = LogicalRelation.AND)
        private String keyword;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
    }
}
