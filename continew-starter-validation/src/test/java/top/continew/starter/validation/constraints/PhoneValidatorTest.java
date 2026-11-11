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
 * {@link PhoneValidator} 电话校验器测试。
 *
 * <p>空值按约定放行，非空时接受座机与手机号。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class PhoneValidatorTest {

    private static ValidatorFactory validatorFactory;
    private final PhoneValidator validator = new PhoneValidator();

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
    void blankValueShouldBeValid() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    void mobileShouldBeValid() {
        assertThat(validator.isValid("13800138000", null)).isTrue();
    }

    @Test
    void landlineShouldBeValid() {
        assertThat(validator.isValid("0571-88888888", null)).isTrue();
    }

    @Test
    void plainTextShouldBeInvalid() {
        assertThat(validator.isValid("call-me", null)).isFalse();
    }

    @Test
    void annotationEndToEndShouldValidatePhone() {
        Validator jakartaValidator = validatorFactory.getValidator();
        PhonePayload payload = new PhonePayload();
        payload.phone = "010-88888888";
        assertThat(jakartaValidator.validate(payload)).isEmpty();
        payload.phone = "not-a-phone";
        assertThat(jakartaValidator.validate(payload))
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("电话格式错误");
    }

    /**
     * 端到端样例载体：phone 字段要求为合法座机或手机号。
     */
    static class PhonePayload {

        @Phone(message = "电话格式错误")
        private String phone;
    }
}
