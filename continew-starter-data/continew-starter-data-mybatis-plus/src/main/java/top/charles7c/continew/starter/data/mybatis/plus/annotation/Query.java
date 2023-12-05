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

package top.charles7c.continew.starter.data.mybatis.plus.annotation;

import top.charles7c.continew.starter.data.mybatis.plus.enums.QueryTypeEnum;

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
     * 属性名（默认和使用该注解的属性的名称一致）
     */
    String property() default "";

    /**
     * 查询类型（等值查询、模糊查询、范围查询等）
     */
    QueryTypeEnum type() default QueryTypeEnum.EQUAL;

    /**
     * 多属性模糊查询，仅支持 String 类型属性
     * <p>
     * 例如：@Query(blurry = {"username", "email"}) 表示根据用户名和邮箱模糊查询
     * </p>
     */
    String[] blurry() default {};
}
