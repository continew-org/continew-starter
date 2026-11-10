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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;
import top.continew.starter.core.util.validation.Validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/**
 * JSR 303 校验器测试
 *
 * <p>
 * {@code Validator} 静态字段在类初始化时从容器取校验器，此处通过
 * {@link SpringContextTestHolder} 注入 mock 上下文提供受控校验器实例；
 * 测试类置于 util 包以避免与 validation 包内同名类型相互遮蔽。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ValidatorTest {

    private static jakarta.validation.Validator jakartaValidator;

    /**
     * 注入 mock 容器上下文，为 Validator 类的静态初始化提供 JSR 303 校验器；
     * 校验器实例随类初始化固化，两个用例共用并按入参区分返回结果
     */
    @BeforeAll
    static void initSpringContext() {
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBean(jakarta.validation.Validator.class))
            .thenReturn(mock(jakarta.validation.Validator.class));
        SpringContextTestHolder.inject(beanFactory);
        // 静态字段随首次类初始化固化，须对固化实例打桩以保证与同 JVM 内初始化顺序无关
        jakartaValidator = (jakarta.validation.Validator) ReflectionTestUtils
            .getField(Validator.class, "VALIDATOR_INSTANCE");
    }

    @Test
    @DisplayName("validate 无违规时放行")
    void shouldPassWhenNoViolation() {
        assertThatCode(() -> Validator.validate(new Object())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate 存在违规时抛出 ConstraintViolationException")
    void shouldThrowWhenViolationExists() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Object target = new Object();
        when(jakartaValidator.validate(eq(target))).thenReturn(Set.of(violation));
        assertThatThrownBy(() -> Validator.validate(target))
            .isInstanceOf(ConstraintViolationException.class);
    }
}
