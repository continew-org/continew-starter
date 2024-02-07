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

package top.charles7c.continew.starter.security.mask.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import top.charles7c.continew.starter.core.constant.StringConstants;
import top.charles7c.continew.starter.security.mask.core.JsonMaskSerializer;
import top.charles7c.continew.starter.security.mask.enums.MaskType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON 脱敏注解
 *
 * @author Charles7c
 * @since 1.4.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = JsonMaskSerializer.class)
public @interface JsonMask {

    /**
     * 脱敏类型
     */
    MaskType value() default MaskType.CUSTOM;

    /**
     * 左侧保留位数
     * <p>
     * 仅在脱敏类型为 {@code DesensitizedType.CUSTOM } 时使用
     * </p>
     */
    int left() default 0;

    /**
     * 右侧保留位数
     * <p>
     * 仅在脱敏类型为 {@code DesensitizedType.CUSTOM } 时使用
     * </p>
     */
    int right() default 0;

    /**
     * 脱敏符号（默认：*）
     */
    char character() default StringConstants.C_ASTERISK;
}