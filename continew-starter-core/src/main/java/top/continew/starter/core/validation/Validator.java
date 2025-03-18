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

package top.continew.starter.core.validation;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * 校验器
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class Validator {

    private static final jakarta.validation.Validator VALIDATOR = SpringUtil
        .getBean(jakarta.validation.Validator.class);

    protected Validator() {
    }

    /**
     * 如果为空，抛出异常
     *
     * @param obj           被检测的对象
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfNull(Object obj, String message, Class<? extends RuntimeException> exceptionType) {
        throwIf(null == obj, message, exceptionType);
    }

    /**
     * 如果不为空，抛出异常
     *
     * @param obj           被检测的对象
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfNotNull(Object obj, String message, Class<? extends RuntimeException> exceptionType) {
        throwIf(null != obj, message, exceptionType);
    }

    /**
     * 如果为空，抛出异常
     *
     * @param obj           被检测的对象
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfEmpty(Object obj, String message, Class<? extends RuntimeException> exceptionType) {
        throwIf(ObjectUtil.isEmpty(obj), message, exceptionType);
    }

    /**
     * 如果不为空，抛出异常
     *
     * @param obj           被检测的对象
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfNotEmpty(Object obj, String message, Class<? extends RuntimeException> exceptionType) {
        throwIf(ObjectUtil.isNotEmpty(obj), message, exceptionType);
    }

    /**
     * 如果为空，抛出异常
     *
     * @param str           被检测的字符串
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfBlank(CharSequence str,
                                       String message,
                                       Class<? extends RuntimeException> exceptionType) {
        throwIf(CharSequenceUtil.isBlank(str), message, exceptionType);
    }

    /**
     * 如果不为空，抛出异常
     *
     * @param str           被检测的字符串
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfNotBlank(CharSequence str,
                                          String message,
                                          Class<? extends RuntimeException> exceptionType) {
        throwIf(CharSequenceUtil.isNotBlank(str), message, exceptionType);
    }

    /**
     * 如果相同，抛出异常
     *
     * @param obj1          要比较的对象1
     * @param obj2          要比较的对象2
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfEqual(Object obj1,
                                       Object obj2,
                                       String message,
                                       Class<? extends RuntimeException> exceptionType) {
        throwIf(ObjectUtil.equal(obj1, obj2), message, exceptionType);
    }

    /**
     * 如果不相同，抛出异常
     *
     * @param obj1          要比较的对象1
     * @param obj2          要比较的对象2
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfNotEqual(Object obj1,
                                          Object obj2,
                                          String message,
                                          Class<? extends RuntimeException> exceptionType) {
        throwIf(ObjectUtil.notEqual(obj1, obj2), message, exceptionType);
    }

    /**
     * 如果相同，抛出异常（不区分大小写）
     *
     * @param str1          要比较的字符串1
     * @param str2          要比较的字符串2
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfEqualIgnoreCase(CharSequence str1,
                                                 CharSequence str2,
                                                 String message,
                                                 Class<? extends RuntimeException> exceptionType) {
        throwIf(CharSequenceUtil.equalsIgnoreCase(str1, str2), message, exceptionType);
    }

    /**
     * 如果不相同，抛出异常（不区分大小写）
     *
     * @param str1          要比较的字符串1
     * @param str2          要比较的字符串2
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIfNotEqualIgnoreCase(CharSequence str1,
                                                    CharSequence str2,
                                                    String message,
                                                    Class<? extends RuntimeException> exceptionType) {
        throwIf(!CharSequenceUtil.equalsIgnoreCase(str1, str2), message, exceptionType);
    }

    /**
     * 如果条件成立，抛出异常
     *
     * @param condition     条件
     * @param message       提示信息
     * @param exceptionType 异常类型
     */
    protected static void throwIf(boolean condition, String message, Class<? extends RuntimeException> exceptionType) {
        if (condition) {
            throw ReflectUtil.newInstance(exceptionType, message);
        }
    }

    /**
     * 如果条件成立，抛出异常
     *
     * @param conditionSupplier 条件
     * @param message           错误信息
     * @param exceptionType     异常类型
     */
    protected static void throwIf(BooleanSupplier conditionSupplier,
                                  String message,
                                  Class<? extends RuntimeException> exceptionType) {
        if (null != conditionSupplier && conditionSupplier.getAsBoolean()) {
            throw ReflectUtil.newInstance(exceptionType, message);
        }
    }

    /**
     * JSR 303 校验
     *
     * @param obj    被校验对象
     * @param groups 分组
     * @since 2.3.0
     */
    public static void validate(Object obj, Class<?>... groups) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(obj, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
