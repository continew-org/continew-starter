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

package top.continew.starter.core.util.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.util.SpringContextTestHolder;

import jakarta.validation.Validator;
import java.util.function.BooleanSupplier;

/**
 * 基本参数校验工具类测试
 *
 * <p>
 * {@code Validator} 静态字段在类初始化时从容器取 JSR 303 校验器，此处通过
 * {@link SpringContextTestHolder} 注入 mock 上下文提供受控校验器实例。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ValidationUtilsTest {

    /**
     * 注入 mock 容器上下文，为 Validator 类的静态初始化提供 JSR 303 校验器
     */
    @BeforeAll
    static void initSpringContext() {
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBean(Validator.class)).thenReturn(mock(Validator.class));
        SpringContextTestHolder.inject(beanFactory);
    }

    @Test
    @DisplayName("throwIfNull 对 null 抛出 400 异常且模板参数被替换")
    void shouldThrowBadRequestForNull() {
        assertThatThrownBy(() -> ValidationUtils.throwIfNull(null, "参数 {} 不能为空", "name"))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("参数 name 不能为空");
    }

    @Test
    @DisplayName("throwIfNull 对非 null 不抛出")
    void shouldPassForNonNull() {
        assertThatCode(() -> ValidationUtils.throwIfNull("value", "参数 {} 不能为空", "name"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfNotNull 对非 null 抛出异常")
    void shouldThrowForNotNull() {
        assertThatThrownBy(() -> ValidationUtils.throwIfNotNull("value", "参数已存在"))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("参数已存在");
    }

    @Test
    @DisplayName("throwIfEmpty 对空对象抛出异常，非空对象放行")
    void shouldThrowForEmptyObject() {
        assertThatThrownBy(() -> ValidationUtils.throwIfEmpty("", "值不能为空"))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ValidationUtils.throwIfEmpty(null, "值不能为空"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfEmpty("value", "值不能为空"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfNotEmpty 对非空对象抛出异常，空对象放行")
    void shouldThrowForNotEmptyObject() {
        assertThatThrownBy(() -> ValidationUtils.throwIfNotEmpty("value", "值须为空"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfNotEmpty("", "值须为空"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfBlank 对 null、空串与纯空白抛出异常")
    void shouldThrowForBlankString() {
        assertThatThrownBy(() -> ValidationUtils.throwIfBlank(null, "文本不能为空"))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ValidationUtils.throwIfBlank("  ", "文本不能为空"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfBlank("value", "文本不能为空"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfNotBlank 对非空白文本抛出异常")
    void shouldThrowForNotBlankString() {
        assertThatThrownBy(() -> ValidationUtils.throwIfNotBlank("value", "文本须为空"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfNotBlank("  ", "文本须为空"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfEqual 对相等值抛出异常")
    void shouldThrowForEqualValues() {
        assertThatThrownBy(() -> ValidationUtils.throwIfEqual("a", "a", "两值不能相同"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfEqual("a", "b", "两值不能相同"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfNotEqual 对不相等值抛出异常")
    void shouldThrowForNotEqualValues() {
        assertThatThrownBy(() -> ValidationUtils.throwIfNotEqual("a", "b", "两值必须相同"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfNotEqual("a", "a", "两值必须相同"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfEqualIgnoreCase 忽略大小写判定相等")
    void shouldIgnoreCaseForEqualCheck() {
        assertThatThrownBy(() -> ValidationUtils.throwIfEqualIgnoreCase("ABC", "abc", "编码重复"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfEqualIgnoreCase("ABC", "abd", "编码重复"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfNotEqualIgnoreCase 忽略大小写判定不等")
    void shouldIgnoreCaseForNotEqualCheck() {
        assertThatThrownBy(() -> ValidationUtils.throwIfNotEqualIgnoreCase("ABC", "abd", "编码必须一致"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIfNotEqualIgnoreCase("ABC", "abc", "编码必须一致"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIf 布尔条件成立时抛出异常")
    void shouldThrowWhenConditionTrue() {
        assertThatThrownBy(() -> ValidationUtils.throwIf(true, "条件成立"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIf(false, "条件成立"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIf 条件供应器成立时抛出异常，null 供应器放行（锁定当前行为）")
    void shouldThrowWhenSupplierReturnsTrue() {
        assertThatThrownBy(() -> ValidationUtils.throwIf(() -> true, "条件成立"))
            .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> ValidationUtils.throwIf(() -> false, "条件成立"))
            .doesNotThrowAnyException();
        assertThatCode(() -> ValidationUtils.throwIf((BooleanSupplier) null, "条件成立"))
            .doesNotThrowAnyException();
    }
}
