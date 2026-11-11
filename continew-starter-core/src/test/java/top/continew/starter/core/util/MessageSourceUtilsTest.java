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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

/**
 * 国际化工具类测试
 *
 * <p>
 * {@code MESSAGE_SOURCE} 静态字段在类初始化时从容器获取，此处通过
 * {@link SpringContextTestHolder} 注入 mock 上下文提供受控 {@link MessageSource}。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MessageSourceUtilsTest {

    /**
     * 受控的 MessageSource，同时用于校验参数是否透传
     */
    private static final MessageSource MESSAGE_SOURCE = mock(MessageSource.class);

    /**
     * 注入 mock 容器上下文，为 MessageSourceUtils 类的静态初始化提供 MessageSource
     */
    @BeforeAll
    static void initSpringContext() {
        when(MESSAGE_SOURCE.getMessage(any(), any(Object[].class), any(Locale.class)))
            .thenAnswer(invocation -> "消息[" + invocation.getArgument(0) + "]");
        when(MESSAGE_SOURCE.getMessage(eq("missing.code"), any(Object[].class), any(Locale.class)))
            .thenThrow(new NoSuchMessageException("missing.code"));
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBean(MessageSource.class)).thenReturn(MESSAGE_SOURCE);
        SpringContextTestHolder.inject(beanFactory);
    }

    @Test
    @DisplayName("getMessage 按消息编码返回国际化消息")
    void shouldReturnMessageByCode() {
        assertThat(MessageSourceUtils.getMessage("user.not.exists"))
            .isEqualTo("消息[user.not.exists]");
    }

    @Test
    @DisplayName("getMessage 携带参数时参数透传给 MessageSource")
    void shouldPassArgsToMessageSource() {
        // 同类其他用例也会以相同编码调用，清空调用记录后才能断言单次透传
        clearInvocations(MESSAGE_SOURCE);
        String message = MessageSourceUtils.getMessage("user.not.exists", 1L);
        assertThat(message).isEqualTo("消息[user.not.exists]");
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(MESSAGE_SOURCE)
            .getMessage(eq("user.not.exists"), argsCaptor.capture(), any(Locale.class));
        assertThat(argsCaptor.getValue()).containsExactly(1L);
    }

    @Test
    @DisplayName("getMessage 未命中时回退到默认消息")
    void shouldFallbackToDefaultMessage() {
        assertThat(MessageSourceUtils.getMessage("missing.code", "默认消息")).isEqualTo("默认消息");
    }

    @Test
    @DisplayName("getMessage 未命中且带参数时同样回退默认消息")
    void shouldFallbackToDefaultMessageWithArgs() {
        assertThat(MessageSourceUtils.getMessage("missing.code", "默认消息", 1L)).isEqualTo("默认消息");
    }

    @Test
    @DisplayName("getMessage 双参重载未命中时以编码本身作为默认消息")
    void shouldFallbackToCodeWhenNoDefaultMessage() {
        assertThat(MessageSourceUtils.getMessage("missing.code")).isEqualTo("missing.code");
    }

    @Test
    @DisplayName("getMessage 单参重载与 varargs 重载结果一致")
    void shouldKeepConsistencyBetweenOverloads() {
        assertThat(MessageSourceUtils.getMessage("user.not.exists"))
            .isEqualTo(MessageSourceUtils.getMessage("user.not.exists", new Object[0]));
    }
}
