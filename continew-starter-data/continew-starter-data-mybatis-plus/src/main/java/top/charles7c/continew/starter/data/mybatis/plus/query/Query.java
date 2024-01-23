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

import java.lang.annotation.*;

/**
 * 查询注解
 *
 * @author Zheng Jie（<a href="https://gitee.com/elunez/eladmin">ELADMIN</a>）
 * @author Charles7c
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {

    /**
     * 属性名数组
     * columns为空 走实体类的字段，并且根据queryType来查询;
     * columns不为空且columns长度为1，走columns[0]的字段，并且根据queryType来查询；
     * columns不为空且columns长度大于1，走columns的所有字段， 并且根据queryType来查询； columns之间的处理是OR操作。
     */
    String[] columns() default {};

    /**
     * 查询类型（等值查询、模糊查询、范围查询等）
     */
    QueryType type() default QueryType.EQUAL;
}
