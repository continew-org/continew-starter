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

package top.continew.starter.security.sensitivewords.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.continew.starter.security.sensitivewords.service.SensitiveWordsService;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * 敏感词校验器测试
 *
 * @author luoqiz
 * @since 3.0.0
 */
class SensitiveWordsValidatorTest {

    /**
     * 命中时校验失败并拼接命中词
     */
    @Test
    @DisplayName("命中敏感词时校验失败")
    void invalidWhenHit() throws NoSuchFieldException {
        SensitiveWordsService service = mock(SensitiveWordsService.class);
        when(service.findAll("含敏感内容")).thenReturn(List.of("敏感"));

        SensitiveWordsValidator validator = newValidator(service, "content");
        ConstraintValidatorContext context = mockContext("内容包含敏感词汇");
        boolean valid = validator.isValid("含敏感内容", context);

        assertThat(valid).isFalse();
        verify(context).buildConstraintViolationWithTemplate("内容包含敏感词汇: 敏感");
    }

    /**
     * 自定义消息生效
     */
    @Test
    @DisplayName("自定义 message 生效")
    void customMessage() throws NoSuchFieldException {
        SensitiveWordsService service = mock(SensitiveWordsService.class);
        when(service.findAll("含敏感内容")).thenReturn(List.of("敏感"));

        SensitiveWordsValidator validator = newValidator(service, "customMessageContent");
        ConstraintValidatorContext context = mockContext("昵称不合规");
        validator.isValid("含敏感内容", context);

        verify(context).buildConstraintViolationWithTemplate("昵称不合规: 敏感");
    }

    /**
     * 未命中时校验通过
     */
    @Test
    @DisplayName("未命中时校验通过")
    void validWhenNoHit() throws NoSuchFieldException {
        SensitiveWordsService service = mock(SensitiveWordsService.class);
        when(service.findAll("正常内容")).thenReturn(List.of());

        SensitiveWordsValidator validator = newValidator(service, "content");
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        assertThat(validator.isValid("正常内容", context)).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    /**
     * 空值直接通过，交给其他约束处理
     */
    @Test
    @DisplayName("空值直接通过")
    void blankValue() throws NoSuchFieldException {
        SensitiveWordsService service = mock(SensitiveWordsService.class);
        SensitiveWordsValidator validator = newValidator(service, "content");
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        assertThat(validator.isValid(null, context)).isTrue();
        assertThat(validator.isValid("", context)).isTrue();
        assertThat(validator.isValid("   ", context)).isTrue();
    }

    /**
     * 快速失败只取第一个词
     */
    @Test
    @DisplayName("failFast 只使用第一个命中词")
    void failFast() throws NoSuchFieldException {
        SensitiveWordsService service = mock(SensitiveWordsService.class);
        when(service.findFirst("含敏感内容")).thenReturn("敏感");

        SensitiveWordsValidator validator = newValidator(service, "fastContent");
        ConstraintValidatorContext context = mockContext("内容包含敏感词汇");

        assertThat(validator.isValid("含敏感内容", context)).isFalse();
        verify(context).buildConstraintViolationWithTemplate("内容包含敏感词汇: 敏感");
    }

    private static SensitiveWordsValidator newValidator(SensitiveWordsService service,
        String fieldName) throws NoSuchFieldException {
        SensitiveWordsValidator validator = new SensitiveWordsValidator();
        ReflectionTestUtils.setField(validator, "sensitiveWordsService", service);
        SensitiveWords annotation =
            DemoRequest.class.getDeclaredField(fieldName).getAnnotation(SensitiveWords.class);
        validator.initialize(annotation);
        return validator;
    }

    private static ConstraintValidatorContext mockContext(String messageTemplate) {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(
            ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.getDefaultConstraintMessageTemplate()).thenReturn(messageTemplate);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        return context;
    }

    /**
     * 测试用请求对象
     */
    static class DemoRequest {

        @SensitiveWords
        private String content;

        @SensitiveWords(message = "昵称不合规")
        private String customMessageContent;

        @SensitiveWords(failFast = true)
        private String fastContent;
    }
}
