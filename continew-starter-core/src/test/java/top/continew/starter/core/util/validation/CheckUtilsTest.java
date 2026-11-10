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
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.SpringContextTestHolder;

import jakarta.validation.Validator;

/**
 * 业务参数校验工具类测试
 *
 * <p>
 * 异常类型统一为 500 {@link BusinessException}；存在性校验的消息模板由源码拼接，
 * 实体名的 DO 后缀剥离仅作用于 throwIfNotExists（锁定当前行为的不对称拼接）。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CheckUtilsTest {

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
    @DisplayName("throwIfNotExists 对 null 抛出 500 异常，实体名剥离 DO 后缀")
    void shouldThrowWhenRecordNotExists() {
        assertThatThrownBy(() -> CheckUtils.throwIfNotExists(null, "UserDO", "id", 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("id 为 [1] 的 User 记录已不存在");
    }

    @Test
    @DisplayName("throwIfNotExists 对非 null 放行")
    void shouldPassWhenRecordExists() {
        assertThatCode(() -> CheckUtils.throwIfNotExists(new Object(), "UserDO", "id", 1L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfExists 对非 null 抛出异常且保留实体名原文（锁定当前行为：不剥离 DO）")
    void shouldThrowWhenRecordExists() {
        assertThatThrownBy(() -> CheckUtils.throwIfExists(new Object(), "UserDO", "id", 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("id 为 [1] 的 UserDO 记录已存在");
    }

    @Test
    @DisplayName("throwIfExists 对 null 放行")
    void shouldPassWhenRecordNotExists() {
        assertThatCode(() -> CheckUtils.throwIfExists(null, "UserDO", "id", 1L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfNull 对 null 抛出异常且模板参数被替换")
    void shouldThrowForNull() {
        assertThatThrownBy(() -> CheckUtils.throwIfNull(null, "对象 {} 不允许为空", "config"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("对象 config 不允许为空");
    }

    @Test
    @DisplayName("throwIfNotNull 对非 null 抛出异常")
    void shouldThrowForNotNull() {
        assertThatThrownBy(() -> CheckUtils.throwIfNotNull("dup", "对象已存在"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("throwIfEmpty 与 throwIfNotEmpty 按空判断双向抛出")
    void shouldThrowBidirectionalForEmpty() {
        assertThatThrownBy(() -> CheckUtils.throwIfEmpty("", "内容为空"))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CheckUtils.throwIfNotEmpty("content", "内容须为空"))
            .isInstanceOf(BusinessException.class);
        assertThatCode(() -> CheckUtils.throwIfEmpty("content", "内容为空"))
            .doesNotThrowAnyException();
        assertThatCode(() -> CheckUtils.throwIfNotEmpty("", "内容须为空"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfBlank 与 throwIfNotBlank 按空白判断双向抛出，null 视为空白")
    void shouldThrowBidirectionalForBlank() {
        assertThatThrownBy(() -> CheckUtils.throwIfBlank(" ", "文本为空"))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CheckUtils.throwIfBlank(null, "文本为空"))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CheckUtils.throwIfNotBlank("text", "文本须为空"))
            .isInstanceOf(BusinessException.class);
        assertThatCode(() -> CheckUtils.throwIfBlank("content", "文本为空"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throwIfEqual 与 throwIfNotEqual 按相等判断双向抛出")
    void shouldThrowBidirectionalForEqual() {
        assertThatThrownBy(() -> CheckUtils.throwIfEqual(1, 1, "数值相同"))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CheckUtils.throwIfNotEqual(1, 2, "数值不同"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("throwIfEqualIgnoreCase 与 throwIfNotEqualIgnoreCase 忽略大小写双向抛出")
    void shouldThrowBidirectionalForEqualIgnoreCase() {
        assertThatThrownBy(() -> CheckUtils.throwIfEqualIgnoreCase("AbC", "abc", "编码相同"))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CheckUtils.throwIfNotEqualIgnoreCase("AbC", "abd", "编码不同"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("throwIf 布尔与供应器条件成立时抛出异常")
    void shouldThrowWhenConditionHolds() {
        assertThatThrownBy(() -> CheckUtils.throwIf(true, "条件成立"))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CheckUtils.throwIf(() -> true, "供应器条件成立"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("供应器条件成立");
    }

    @Test
    @DisplayName("不抛出场景统一放行")
    void shouldPassWhenConditionNotHolds() {
        assertThatCode(() -> CheckUtils.throwIfNull("v", "t"))
            .doesNotThrowAnyException();
        assertThatCode(() -> CheckUtils.throwIf(false, "条件成立"))
            .doesNotThrowAnyException();
        assertThatCode(() -> CheckUtils.throwIf(() -> false, "条件成立"))
            .doesNotThrowAnyException();
    }
}
