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

package top.continew.starter.extension.crud.annotation;

import top.continew.starter.extension.crud.model.query.SortQuery;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字典结构映射
 *
 * <p>用于查询字典列表 API（下拉选项等场景）</p>
 *
 * @see top.continew.starter.extension.crud.controller.AbstractCrudController#dict(Object, SortQuery)
 *
 * @author Charles7c
 * @since 2.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DictModel {

    /**
     * 标签字段名
     *
     * @return 标签字段名
     */
    String labelKey() default "name";

    /**
     * 值字段名
     *
     * @return 值字段名
     */
    String valueKey() default "id";

    /**
     * 额外信息字段名
     *
     * @return 额外信息字段名
     */
    String[] extraKeys() default {};
}
