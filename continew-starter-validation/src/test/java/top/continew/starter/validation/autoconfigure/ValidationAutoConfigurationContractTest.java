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

package top.continew.starter.validation.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import java.util.Set;

/**
 * 校验模块自动配置契约测试
 *
 * <p>装配前提为容器中存在 MessageSource（validator Bean 的硬性依赖），
 * 各用例以 StaticMessageSource 供给。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ValidationAutoConfigurationContractTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class));

    /**
     * ValidationAutoConfiguration 必须显式登记进 imports 资源。
     *
     * <p>该入口只标注 {@code @AutoConfigureBefore}，不在候选扫描结果中。</p>
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .isRegistered(ValidationAutoConfiguration.class));
    }

    /**
     * 无装配开关，默认应注册启用快速失败的 Validator。
     */
    @Test
    void validatorShouldRegisterByDefault() {
        this.runner.withBean(MessageSource.class, StaticMessageSource::new)
            .run(context -> assertThat(context).hasSingleBean(Validator.class));
    }

    /**
     * 校验器应启用快速失败：多字段同时违规时仅返回首个违规（默认行为是收集全部违规）。
     */
    @Test
    void validatorShouldFailFast() {
        this.runner.withBean(MessageSource.class, StaticMessageSource::new).run(context -> {
            Validator validator = context.getBean(Validator.class);
            Set<ConstraintViolation<UserRegistration>> violations =
                validator.validate(new UserRegistration());
            assertEquals(1, violations.size());
        });
    }

    /**
     * 校验消息应通过容器 MessageSource 做国际化插值，而非 Hibernate Validator 内置资源束。
     */
    @Test
    void validatorShouldInterpolateMessageFromMessageSource() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("contract.not-blank", Locale.getDefault(), "参数不能为空");
        messageSource.addMessage("contract.not-blank", Locale.ROOT, "参数不能为空");
        this.runner.withBean(MessageSource.class, () -> messageSource).run(context -> {
            Validator validator = context.getBean(Validator.class);
            Set<ConstraintViolation<ValidatedPayload>> violations =
                validator.validate(new ValidatedPayload());
            violations.forEach(violation -> assertEquals("参数不能为空", violation.getMessage()));
        });
    }

    /**
     * 双字段违规样例，用于验证快速失败语义。
     */
    static class UserRegistration {

        @NotBlank
        private String username;

        @NotBlank
        private String mobile;
    }

    /**
     * 单字段违规样例，消息占位符指向容器 MessageSource。
     */
    static class ValidatedPayload {

        @NotBlank(message = "{contract.not-blank}")
        private String username;
    }
}
