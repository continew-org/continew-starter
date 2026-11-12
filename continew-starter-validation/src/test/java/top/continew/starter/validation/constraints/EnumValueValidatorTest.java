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

package top.continew.starter.validation.constraints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.enums.BaseEnum;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link EnumValueValidator} 枚举校验器分支测试。
 *
 * <p>通过反射持有各注解配置实例，直接驱动 isValid 覆盖数组、集合、单值与异常分支；
 * 同时用 Jakarta Validator 做一次注解端到端校验。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class EnumValueValidatorTest {

    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    /**
     * 实现 BaseEnum 的样例枚举，枚举值为整型。
     */
    enum Gender implements BaseEnum<Integer> {

        MALE(1), FEMALE(2);

        private final Integer value;

        Gender(Integer value) {
            this.value = value;
        }

        @Override
        public Integer getValue() {
            return value;
        }

        @Override
        public String getDescription() {
            return name();
        }
    }

    /**
     * 未实现 BaseEnum 的普通枚举，校验时退化为 toString 忽略大小写比对。
     */
    enum Color {
        RED, GREEN
    }

    /**
     * 空枚举，用于锁定无枚举常量时校验失败的分支。
     */
    enum EmptyEnum {
    }

    /**
     * 带取值方法的样例枚举，method 配置指向 getCode。
     */
    enum GenderCode {

        MALE("M"), FEMALE("F");

        private final String code;

        GenderCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * 各注解配置的字段载体，仅用于反射取注解实例。
     */
    static class AnnotatedHolder {

        @EnumValue(value = Gender.class)
        private Gender gender;

        @EnumValue(enumValues = {"F", "M"})
        private String plain;

        @EnumValue(value = Color.class)
        private Color color;

        @EnumValue(value = EmptyEnum.class)
        private EmptyEnum empty;

        @EnumValue(value = GenderCode.class, method = "getCode")
        private GenderCode genderCode;

        @EnumValue(value = GenderCode.class, method = "notExists")
        private Object badMethod;
    }

    private static EnumValue anno(String fieldName) {
        try {
            Field field = AnnotatedHolder.class.getDeclaredField(fieldName);
            return field.getAnnotation(EnumValue.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static EnumValueValidator validator(String fieldName) {
        EnumValueValidator validator = new EnumValueValidator();
        validator.initialize(anno(fieldName));
        return validator;
    }

    @Test
    void nullValueShouldBeValid() {
        assertThat(validator("gender").isValid(null, null)).isTrue();
    }

    @Test
    void emptyArrayShouldBeValid() {
        assertThat(validator("gender").isValid(new Object[0], null)).isTrue();
    }

    @Test
    void arrayAllMatchedByEnumValuesShouldBeValid() {
        assertThat(validator("plain").isValid(new Object[] {"F", "M"}, null)).isTrue();
    }

    @Test
    void arrayWithInvalidElementShouldBeInvalid() {
        assertThat(validator("plain").isValid(new Object[] {"F", "X"}, null)).isFalse();
    }

    @Test
    void emptyCollectionShouldBeValid() {
        assertThat(validator("gender").isValid(Collections.emptyList(), null)).isTrue();
    }

    @Test
    void collectionAllMatchedByValueStringShouldBeValid() {
        List<Object> values = Arrays.asList("1", "2");
        assertThat(validator("gender").isValid(values, null)).isTrue();
    }

    @Test
    void collectionWithInvalidElementShouldBeInvalid() {
        List<Object> values = Arrays.asList(Gender.MALE, "X");
        assertThat(validator("gender").isValid(values, null)).isFalse();
    }

    @Test
    void singleValueMatchedByEnumValuesShouldBeValid() {
        assertThat(validator("plain").isValid("F", null)).isTrue();
    }

    @Test
    void singleValueNotMatchedByEnumValuesShouldBeInvalid() {
        assertThat(validator("plain").isValid("X", null)).isFalse();
    }

    @Test
    void enumInstanceItselfShouldNotBeMatchedByBaseEnumValue() {
        // 锁定当前行为：BaseEnum 分支只比对 getValue() 的字符串形式，枚举实例本身不参与比对
        assertThat(validator("gender").isValid(Gender.MALE, null)).isFalse();
    }

    @Test
    void baseEnumValueAsRawStringShouldBeMatched() {
        assertThat(validator("gender").isValid("1", null)).isTrue();
    }

    @Test
    void baseEnumValueNotMatchedShouldBeInvalid() {
        assertThat(validator("gender").isValid(3, null)).isFalse();
    }

    @Test
    void plainEnumNameIgnoreCaseShouldBeMatched() {
        assertThat(validator("color").isValid("red", null)).isTrue();
    }

    @Test
    void plainEnumNameNotMatchedShouldBeInvalid() {
        assertThat(validator("color").isValid("BLUE", null)).isFalse();
    }

    @Test
    void emptyEnumShouldBeInvalid() {
        assertThat(validator("empty").isValid("MALE", null)).isFalse();
    }

    @Test
    void configuredMethodValueShouldBeMatched() {
        assertThat(validator("genderCode").isValid("M", null)).isTrue();
    }

    @Test
    void configuredMethodValueNotMatchedShouldBeInvalid() {
        assertThat(validator("genderCode").isValid("X", null)).isFalse();
    }

    @Test
    void configuredMethodNotExistsShouldBeInvalid() {
        assertThat(validator("badMethod").isValid("M", null)).isFalse();
    }

    @Test
    void annotationEndToEndShouldValidateByJakartaValidator() {
        Validator validator = validatorFactory.getValidator();
        EnumPayload payload = new EnumPayload();
        payload.gender = Gender.FEMALE;
        assertThat(validator.validate(payload)).isEmpty();
        payload.gender = null;
        // 校验器约定：null 交给 @NotNull 等注解处理，本注解不拦截
        assertThat(validator.validate(payload)).isEmpty();
    }

    @Test
    void annotationEndToEndShouldReportDefaultMessage() {
        Validator validator = validatorFactory.getValidator();
        EnumPayload payload = new EnumPayload();
        payload.gender = Gender.MALE;
        assertThat(validator.validate(payload))
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("参数值无效");
    }

    /**
     * 端到端样例载体：gender 字段只允许 FEMALE（enumValues 与枚举实例比对的是枚举名）。
     */
    static class EnumPayload {

        @EnumValue(value = Gender.class, enumValues = {"FEMALE"})
        private Gender gender;
    }
}
