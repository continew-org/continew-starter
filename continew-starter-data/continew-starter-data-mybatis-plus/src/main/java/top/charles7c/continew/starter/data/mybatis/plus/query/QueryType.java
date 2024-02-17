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

package top.charles7c.continew.starter.data.mybatis.plus.query;

/**
 * 查询类型枚举
 *
 * @author Charles7c
 * @since 1.0.0
 */
public enum QueryType {

    /**
     * 等于 =，例如：WHERE age = 18
     */
    EQ,

    /**
     * 不等于 !=，例如：WHERE age != 18
     */
    NE,

    /**
     * 大于 >，例如：WHERE age > 18
     */
    GT,

    /**
     * 大于等于 >= ，例如：WHERE age >= 18
     */
    GE,

    /**
     * 小于 <，例如：WHERE age < 18
     */
    LT,

    /**
     * 小于等于 <=，例如：WHERE age <= 18
     */
    LE,

    /**
     * 范围查询，例如：WHERE age BETWEEN 10 AND 18
     */
    BETWEEN,

    /**
     * LIKE '%值%'，例如：WHERE nickname LIKE '%s%'
     */
    LIKE,

    /**
     * LIKE '%值'，例如：WHERE nickname LIKE '%s'
     */
    LIKE_LEFT,

    /**
     * LIKE '值%'，例如：WHERE nickname LIKE 's%'
     */
    LIKE_RIGHT,

    /**
     * 包含查询，例如：WHERE age IN (10, 20, 30)
     */
    IN,

    /**
     * 不包含查询，例如：WHERE age NOT IN (20, 30)
     */
    NOT_IN,

    /**
     * 空查询，例如：WHERE email IS NULL
     */
    IS_NULL,

    /**
     * 非空查询，例如：WHERE email IS NOT NULL
     */
    IS_NOT_NULL,;
}
