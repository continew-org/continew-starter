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

package top.continew.starter.data.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.LogicalRelation;
import top.continew.starter.data.enums.QueryType;

/**
 * MyBatis Flex {@link QueryWrapperHelper} 多列逻辑关系离线单元测试。
 *
 * <p>直接对公开入口 {@code build} 产出的 QueryWrapper 渲染 SQL，断言多列条件的 OR/AND
 * 组合语义及其与其它字段条件的结合方式，无需数据库即可锁定 {@code @Query.logicalRelation}
 * 契约。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class QueryWrapperHelperTest {

    /**
     * 多列未显式声明逻辑关系时默认 OR：两列条件以 OR 连接，且不出现跨列 AND。
     */
    @Test
    void shouldCombineMultipleColumnsWithOrByDefault() {
        KeywordQuery query = new KeywordQuery("一号");
        String sql = QueryWrapperHelper.build(query).toSQL();
        assertThat(sql).contains("username LIKE '%一号%'").contains("nickname LIKE '%一号%'")
            .contains(" OR ");
        assertThat(sql.replace(" ", "")).doesNotContain(")AND(username");
    }

    /**
     * 多列显式声明 AND：两列条件各自以 AND 连接，不出现 OR。
     */
    @Test
    void shouldCombineMultipleColumnsWithAndWhenDeclared() {
        AndKeywordQuery query = new AndKeywordQuery("一号");
        String sql = QueryWrapperHelper.build(query).toSQL();
        assertThat(sql).contains("username LIKE '%一号%'").contains("nickname LIKE '%一号%'")
            .contains(" AND ").doesNotContain(" OR ");
    }

    /**
     * 多列 OR 分组与其它字段条件结合时整体保持 AND，OR 仅作用于分组内的列条件。
     */
    @Test
    void shouldKeepOrGroupAndedWithOtherFields() {
        MixedQuery query = new MixedQuery(10L, "一号");
        String sql = QueryWrapperHelper.build(query).toSQL();
        assertThat(sql).contains("dept_id = 10").contains("username LIKE '%一号%'")
            .contains("nickname LIKE '%一号%'").contains(" OR ");
        assertThat(sql.replace(" ", "")).contains("(dept_id=10)AND(");
    }

    /**
     * 单列条件仍按单列语义渲染，不受多列逻辑关系分支影响。
     */
    @Test
    void shouldKeepSingleColumnConditionUnchanged() {
        String sql = QueryWrapperHelper.build(new DeptQuery(10L)).toSQL();
        assertThat(sql).contains("dept_id = 10").doesNotContain(" OR ")
            .doesNotContain(" AND ");
    }

    /**
     * 多列 OR 模糊查询条件对象（默认逻辑关系）。
     */
    private static class KeywordQuery {

        /**
         * 关键词（用户名或昵称任一命中）
         */
        @Query(type = QueryType.LIKE, columns = {"username", "nickname"})
        private final String keyword;

        /**
         * 构造关键词条件
         *
         * @param keyword 关键词
         */
        KeywordQuery(String keyword) {
            this.keyword = keyword;
        }
    }

    /**
     * 多列 AND 模糊查询条件对象。
     */
    private static class AndKeywordQuery {

        /**
         * 关键词（用户名与昵称须同时命中）
         */
        @Query(type = QueryType.LIKE, columns = {"username", "nickname"},
            logicalRelation = LogicalRelation.AND)
        private final String keyword;

        /**
         * 构造关键词条件
         *
         * @param keyword 关键词
         */
        AndKeywordQuery(String keyword) {
            this.keyword = keyword;
        }
    }

    /**
     * 等值字段与多列 OR 字段混合的条件对象。
     */
    private static class MixedQuery {

        /**
         * 部门等值查询
         */
        @Query(type = QueryType.EQ)
        private final Long deptId;
        /**
         * 关键词（用户名或昵称任一命中）
         */
        @Query(type = QueryType.LIKE, columns = {"username", "nickname"})
        private final String keyword;

        /**
         * 构造混合条件
         *
         * @param deptId  部门 ID
         * @param keyword 关键词
         */
        MixedQuery(Long deptId, String keyword) {
            this.deptId = deptId;
            this.keyword = keyword;
        }
    }

    /**
     * 单列等值查询条件对象。
     */
    private static class DeptQuery {

        /**
         * 部门等值查询
         */
        @Query(type = QueryType.EQ)
        private final Long deptId;

        /**
         * 构造部门条件
         *
         * @param deptId 部门 ID
         */
        DeptQuery(Long deptId) {
            this.deptId = deptId;
        }
    }
}
