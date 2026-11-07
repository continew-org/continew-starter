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

package top.continew.starter.data.annotation;

import top.continew.starter.data.enums.LogicalRelation;
import top.continew.starter.data.enums.QueryType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询注解
 *
 * <p>
 * 同一字段可通过重复声明本注解表达多组查询条件，每组可通过 {@link #groups()} 绑定生效
 * 分组（如列表、导出），由查询构建方按当前分组选择声明；未声明分组的声明在所有分组下
 * 生效。
 * </p>
 *
 * @author Charles7c
 * @author Jasmine
 * @since 1.0.0
 */
@Repeatable(Queries.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {

    /**
     * 列名（注意：列名是数据库字段名，而不是实体类字段名。如果命名是数据库关键字的，请使用转义符包裹）
     *
     * <p>
     * columns 为空时，默认取值字段名（自动转换为下划线命名）；<br>
     * columns 不为空且 columns 长度大于 1，多个列查询条件之间为或关系（OR）。
     * </p>
     *
     * @return 列名
     */
    String[] columns() default {};

    /**
     * 查询类型（等值查询、模糊查询、范围查询等）
     *
     * @return 查询类型
     */
    QueryType type() default QueryType.EQ;

    /**
     * 多列查询时的逻辑关系（仅当 columns 长度大于 1 时生效）
     *
     * @return 多列查询时的逻辑关系
     */
    LogicalRelation logicalRelation() default LogicalRelation.OR;

    /**
     * 生效分组
     *
     * <p>
     * 为空时该声明在所有分组下生效；不为空时仅在命中的分组下生效，且优先级高于未声明
     * 分组的声明。分组标识由查询构建方传入，CRUD 场景内置列表（list）与导出（export）
     * 两个分组，见 {@code CrudQueryGroups}。
     * </p>
     *
     * @return 生效分组
     * @since 3.1.0
     */
    String[] groups() default {};
}
