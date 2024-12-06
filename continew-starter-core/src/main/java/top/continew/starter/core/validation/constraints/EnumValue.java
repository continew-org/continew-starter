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

package top.continew.starter.core.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 枚举校验注解
 *
 * <p>
 * {@code @EnumValue(value = XxxEnum.class, message = "参数值非法")} <br />
 * {@code @EnumValue(enumValues = {"F", "M"} ,message = "性别只允许为F或M")}
 * </p>
 *
 * @author Jasmine
 * @author Charles7c
 * @since 2.7.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {

    /**
     * 枚举类
     *
     * @return 枚举类
     */
    Class<? extends Enum> value() default Enum.class;

    /**
     * 枚举值
     *
     * @return 枚举值
     */
    String[] enumValues() default {};

    /**
     * 获取枚举值的方法名
     *
     * @return 获取枚举值的方法名
     */
    String method() default "";

    /**
     * 提示消息
     *
     * @return 提示消息
     */
    String message() default "参数值非法";

    /**
     * 分组
     *
     * @return 分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     *
     * @return 负载
     */
    Class<? extends Payload>[] payload() default {};
}
