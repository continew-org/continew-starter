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

package top.continew.starter.data.core.annotation;

import top.continew.starter.data.core.enums.QueryType;

import java.lang.annotation.*;

/**
 * 查询注解
 *
 * @author Charles7c
 * @author Jasmine
 * @since 1.0.0
 */
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
     */
    String[] columns() default {};

    /**
     * 查询类型（等值查询、模糊查询、范围查询等）
     */
    QueryType type() default QueryType.EQ;
}
