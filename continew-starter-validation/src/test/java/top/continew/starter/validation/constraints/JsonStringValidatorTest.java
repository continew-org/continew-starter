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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * {@link JsonStringValidator} JSON 字符串校验器测试。
 *
 * <p>空值按约定放行（由 @NotBlank 等注解负责非空），非空时仅接受合法 JSON。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class JsonStringValidatorTest {

    private static ValidatorFactory validatorFactory;
    private final JsonStringValidator validator = new JsonStringValidator();

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void nullValueShouldBeValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void emptyValueShouldBeValid() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    void blankValueShouldBeValid() {
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void jsonObjectShouldBeValid() {
        assertThat(validator.isValid("{\"name\":\"continew\"}", null)).isTrue();
    }

    @Test
    void jsonArrayShouldBeValid() {
        assertThat(validator.isValid("[1,2,3]", null)).isTrue();
    }

    @Test
    void plainTextShouldBeInvalid() {
        assertThat(validator.isValid("not-json", null)).isFalse();
    }

    @Test
    void bracketedButSyntacticallyMalformedShouldBeLockedValid() {
        // 锁定当前行为：hutool isTypeJSON 仅做首尾括号探测，不校验 JSON 语法合法性
        assertThat(validator.isValid("{\"name\":}", null)).isTrue();
    }

    @Test
    void annotationEndToEndShouldRejectNonJson() {
        Validator jakartaValidator = validatorFactory.getValidator();
        JsonPayload payload = new JsonPayload();
        payload.config = "not-json";
        assertThat(jakartaValidator.validate(payload))
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("配置格式错误");
        payload.config = "{\"ok\":true}";
        assertThat(jakartaValidator.validate(payload)).isEmpty();
    }

    /**
     * 端到端样例载体：config 字段要求为 JSON 字符串。
     */
    static class JsonPayload {

        @JsonString(message = "配置格式错误")
        private String config;
    }
}
