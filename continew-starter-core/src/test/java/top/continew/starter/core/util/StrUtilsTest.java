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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 字符串工具类测试
 *
 * <p>
 * 锁定 blankToDefault 对空白串走默认值、对非空白串走 mapper 的分支契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class StrUtilsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("null、空串与空白串返回默认值且不触发 mapper")
    void shouldReturnDefaultForBlankInput(String input) {
        String result = StrUtils.blankToDefault(input, "默认值", value -> {
            throw new IllegalStateException("mapper 不应被调用");
        });
        assertThat(result).isEqualTo("默认值");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", " a "})
    @DisplayName("非空白串经 mapper 转换后返回")
    void shouldApplyMapperForNonBlankInput(String input) {
        assertThat(StrUtils.blankToDefault(input, "默认值", String::trim)).isEqualTo("a");
    }

    @Test
    @DisplayName("blankToDefault 支持非字符串的返回类型")
    void shouldSupportNonStringReturnType() {
        Integer result = StrUtils.blankToDefault("42", 0, Integer::valueOf);
        assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("mapper 抛出异常时异常原样向外传播")
    void shouldPropagateMapperException() {
        assertThatThrownBy(() -> StrUtils.blankToDefault("x", "默认值", value -> {
            throw new IllegalArgumentException("转换失败");
        })).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("转换失败");
    }
}
