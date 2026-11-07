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
import static org.junit.jupiter.api.Assertions.assertNull;

import cn.hutool.core.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.lang.reflect.Field;

/**
 * {@link QueryUtils} 分组选择测试。
 *
 * <p>覆盖分组声明的选择顺序（命中分组声明优先、未分组声明回退、无匹配返回 null）、
 * 未指定分组时与既有单声明行为的等价性，以及 hutool {@code AnnotationUtil} 与 JDK
 * {@code getAnnotationsByType} 在可重复注解下的解析语义。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class QueryUtilsTest {

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
         * 未标注 {@code @Query} 的字段（经 getDeclaredField 按名反射访问，Sonar 无法
         * 追踪该引用，故不使用 private 可见性）。
         */
        String plain;
    }

    /**
     * 字段未标注 {@code @Query} 时应返回 null。
     */
    @Test
    void shouldReturnNullWhenFieldHasNoQueryAnnotation() throws Exception {
        Field field = QueryDto.class.getDeclaredField("plain");
        assertNull(QueryUtils.selectQuery(field, null));
        assertNull(QueryUtils.selectQuery(field, "list"));
    }

    /**
     * 既有单声明用法在未指定分组下应原样选中，与既有行为等价。
     */
    @Test
    void shouldSelectSingleDeclarationWithoutGroup() throws Exception {
        Field field = QueryDto.class.getDeclaredField("name");
        Query selected = QueryUtils.selectQuery(field, null);
        assertEquals(QueryType.EQ, selected.type());
    }

    /**
     * 既有单声明用法在任意指定分组下均应回退生效（未分组声明全分组生效）。
     */
    @Test
    void shouldFallbackToUngroupedDeclarationForAnyGroup() throws Exception {
        Field field = QueryDto.class.getDeclaredField("name");
        assertEquals(QueryType.EQ, QueryUtils.selectQuery(field, "list").type());
        assertEquals(QueryType.EQ, QueryUtils.selectQuery(field, "export").type());
    }

    /**
     * 多组声明应按当前分组选中对应声明。
     */
    @Test
    void shouldSelectDeclarationByGroup() throws Exception {
        Field field = QueryDto.class.getDeclaredField("keyword");
        assertEquals(QueryType.EQ, QueryUtils.selectQuery(field, "list").type());
        assertEquals(QueryType.LIKE_RIGHT, QueryUtils.selectQuery(field, "export").type());
    }

    /**
     * 仅分组声明且分组不匹配时，字段不应参与查询条件构建。
     */
    @Test
    void shouldReturnNullWhenGroupNotMatchedAndNoUngroupedDeclaration() throws Exception {
        Field field = QueryDto.class.getDeclaredField("keyword");
        assertNull(QueryUtils.selectQuery(field, null));
        assertNull(QueryUtils.selectQuery(field, "page"));
    }

    /**
     * 分组声明应优先于未分组声明（导出场景覆盖默认等值）。
     */
    @Test
    void shouldPreferGroupedDeclarationOverUngroupedOne() throws Exception {
        Field field = QueryDto.class.getDeclaredField("mixed");
        assertEquals(QueryType.LIKE_RIGHT, QueryUtils.selectQuery(field, "export").type());
        assertEquals(QueryType.EQ, QueryUtils.selectQuery(field, "list").type());
        assertEquals(QueryType.EQ, QueryUtils.selectQuery(field, null).type());
    }

    /**
     * hutool {@code AnnotationUtil} 在可重复注解下遵循 JDK 容器合成语义：字段上存在
     * 多个 {@code @Query} 时按 {@code @Query} 类型直查返回 null。分组解析因此不能走
     * {@code AnnotationUtil.getAnnotation}，而应使用 {@code getAnnotationsByType} 展开。
     */
    @Test
    void shouldVerifyHutoolAnnotationSemanticsOnRepeatableQuery() throws Exception {
        Field singleField = QueryDto.class.getDeclaredField("name");
        Field multiField = QueryDto.class.getDeclaredField("keyword");
        // getAnnotationsByType 统一展开：单声明 1 条，多声明 2 条
        assertEquals(1, singleField.getAnnotationsByType(Query.class).length);
        assertEquals(2, multiField.getAnnotationsByType(Query.class).length);
        // 多声明下按 Query 类型直查返回 null（容器合成），不能作为分组解析入口
        assertNull(AnnotationUtil.getAnnotation(multiField, Query.class));
    }
}
