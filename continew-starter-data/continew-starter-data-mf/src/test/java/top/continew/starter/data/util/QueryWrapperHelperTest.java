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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.util.ReflectUtils;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

/**
 * {@link QueryWrapperHelper} 分组选择行为测试。
 *
 * <p>纯单元测试，不依赖数据库：构造查询条件对象后调用 {@code build}，通过
 * {@code QueryWrapper} 的 SQL 文本断言分组选择与回退行为，语义与 MyBatis Plus
 * 实现保持一致。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class QueryWrapperHelperTest {

    /**
     * 分组选择测试用的查询条件载体。
     */
    static class QueryDto {

        /**
         * 既有用法：单条未分组声明。
         */
        @Query(type = QueryType.EQ)
        private String name;

        /**
         * 多组声明：列表等值、导出右模糊。
         */
        @Query(type = QueryType.EQ, groups = "list")
        @Query(type = QueryType.LIKE_RIGHT, groups = "export")
        private String keyword;

        /**
         * 混合声明：未分组等值兜底，导出场景覆盖为右模糊。
         */
        @Query(type = QueryType.EQ)
        @Query(type = QueryType.LIKE_RIGHT, groups = "export")
        private String mixed;

        /**
         * 未标注 {@code @Query} 的字段（由条件构建遍历按注解过滤，仅断言 SQL 时以列名
         * 字符串引用，Sonar 无法追踪该引用，故不使用 private 可见性）。
         */
        String noAnnotation;

        QueryDto(String name, String keyword, String mixed) {
            this.name = name;
            this.keyword = keyword;
            this.mixed = mixed;
        }
    }

    /**
     * 未指定分组时应与既有行为一致：仅未分组声明生效。
     */
    @Test
    void shouldFallbackToLegacyBehaviorWithoutGroup() {
        QueryDto query = new QueryDto("dev", "team", "ops");
        String sql = QueryWrapperHelper.build(query, ReflectUtils
            .getNonStaticFields(QueryDto.class), QueryWrapper.create(), null).toSQL();
        assertTrue(sql.contains("name ="), sql);
        assertTrue(sql.contains("mixed ="), sql);
        // 仅分组声明在未指定分组下无匹配，不参与条件构建
        assertFalse(sql.contains("keyword"), sql);
    }

    /**
     * 既有三参 build 与四参 build（分组为 null）输出应完全一致。
     */
    @Test
    void shouldKeepSameResultForLegacyOverload() {
        QueryDto query = new QueryDto("dev", "team", "ops");
        String legacySql = QueryWrapperHelper.build(query, ReflectUtils
            .getNonStaticFields(QueryDto.class), QueryWrapper.create()).toSQL();
        String groupedSql = QueryWrapperHelper.build(query, ReflectUtils
            .getNonStaticFields(QueryDto.class), QueryWrapper.create(), null).toSQL();
        assertEquals(legacySql, groupedSql);
    }

    /**
     * 列表分组应选中等值声明。
     */
    @Test
    void shouldSelectEqDeclarationForListGroup() {
        QueryDto query = new QueryDto("dev", "team", "ops");
        String sql = QueryWrapperHelper.build(query, ReflectUtils
            .getNonStaticFields(QueryDto.class), QueryWrapper.create(), "list").toSQL();
        assertTrue(sql.contains("name ="), sql);
        assertTrue(sql.contains("keyword ="), sql);
        assertTrue(sql.contains("mixed ="), sql);
    }

    /**
     * 导出分组应选中右模糊声明（keyword 与 mixed 均被分组声明覆盖）。
     */
    @Test
    void shouldSelectLikeRightDeclarationForExportGroup() {
        QueryDto query = new QueryDto("dev", "team", "ops");
        String sql = QueryWrapperHelper.build(query, ReflectUtils
            .getNonStaticFields(QueryDto.class), QueryWrapper.create(), "export").toSQL();
        assertTrue(sql.contains("name ="), sql);
        assertTrue(sql.contains("keyword LIKE"), sql);
        assertTrue(sql.contains("mixed LIKE"), sql);
        // MyBatis Flex 的 likeRight 生成左通配符（LIKE '%team'），与 MyBatis Plus 的右通配符语义不同，helper 均为透传
        assertTrue(sql.contains("'%team'"), sql);
    }

    /**
     * 分组不匹配且无未分组声明时，字段不应参与条件构建。
     */
    @Test
    void shouldSkipFieldWhenGroupNotMatched() {
        QueryDto query = new QueryDto(null, "team", "ops");
        String sql = QueryWrapperHelper.build(query, ReflectUtils
            .getNonStaticFields(QueryDto.class), QueryWrapper.create(), "page").toSQL();
        assertFalse(sql.contains("keyword"), sql);
        assertTrue(sql.contains("mixed ="), sql);
    }

    /**
     * 无 {@code @Query} 注解的字段不应参与条件构建。
     */
    @Test
    void shouldSkipFieldWithoutQueryAnnotation() {
        QueryDto query = new QueryDto("dev", null, null);
        String sql = QueryWrapperHelper.build(query, ReflectUtils
            .getNonStaticFields(QueryDto.class), QueryWrapper.create(), "list").toSQL();
        assertFalse(sql.contains("noAnnotation"), sql);
    }
}
