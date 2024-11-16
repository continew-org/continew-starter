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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

/**
 * 枚举校验注解校验器
 *
 * @author Charles7c
 * @author Jasmine
 * @since 2.7.3
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private static final Logger log = LoggerFactory.getLogger(EnumValueValidator.class);
    private Class<? extends Enum> enumClass;
    private String[] enumValues;
    private String enumMethod;

    @Override
    public void initialize(EnumValue enumValue) {
        this.enumClass = enumValue.value();
        this.enumValues = enumValue.enumValues();
        this.enumMethod = enumValue.method();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        // 优先校验 enumValues
        if (enumValues.length > 0) {
            return Arrays.asList(enumValues).contains(Convert.toStr(value));
        }
        Enum[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants.length == 0) {
            return false;
        }
        if (CharSequenceUtil.isBlank(enumMethod)) {
            return findEnumValue(enumConstants, Enum::toString, Convert.toStr(value));
        }
        try {
            // 枚举类指定了方法名，则调用指定方法获取枚举值
            Method method = enumClass.getMethod(enumMethod);
            for (Enum enumConstant : enumConstants) {
                if (Convert.toStr(method.invoke(enumConstant)).equals(Convert.toStr(value))) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("An error occurred while validating the enum value, please check the @EnumValue parameter configuration.", e);
        }
        return false;
    }

    /**
     * 遍历枚举类，判断是否包含指定值
     *
     * @param enumConstants 枚举类数组
     * @param function      获取枚举值的函数
     * @param value         待校验的值
     * @return 是否包含指定值
     */
    private boolean findEnumValue(Enum[] enumConstants, Function<Enum, Object> function, Object value) {
        for (Enum enumConstant : enumConstants) {
            if (function.apply(enumConstant).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
