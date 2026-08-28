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

package top.continew.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 导入注解
 *
 * @author jiang4yu
 * @since 2.13.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelImport {

    /**
     * 字段名称
     */
    String value();

    /**
     * 导出映射，格式如：0-未知;1-男;2-女
     */
    String kv() default "";

    /**
     * 是否为必填字段（默认为非必填）
     */
    boolean required() default false;

    /**
     * 最大长度（默认255）
     */
    int maxLength() default 255;

    /**
     * 导入唯一性验证（多个字段则取联合验证）
     */
    boolean unique() default false;

}
