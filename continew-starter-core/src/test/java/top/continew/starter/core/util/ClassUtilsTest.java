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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 类工具类测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ClassUtilsTest {

    /**
     * 已确定泛型参数的列表实现
     */
    private static class StringList extends ArrayList<String> {
    }

    /**
     * 已确定双泛型参数的映射实现
     */
    private static class StringIntegerMap extends HashMap<String, Integer> {
    }

    @Test
    @DisplayName("已确定单泛型参数的类返回参数类型数组")
    void shouldExtractSingleTypeArgument() {
        Class<?>[] arguments = ClassUtils.getTypeArguments(StringList.class);
        assertThat(arguments).containsExactly(String.class);
    }

    @Test
    @DisplayName("已确定双泛型参数的类按声明顺序返回参数类型数组")
    void shouldExtractDoubleTypeArguments() {
        Class<?>[] arguments = ClassUtils.getTypeArguments(StringIntegerMap.class);
        assertThat(arguments).containsExactly(String.class, Integer.class);
    }

    @Test
    @DisplayName("未确定泛型参数的类返回空数组")
    void shouldReturnEmptyArrayForNonGenericType() {
        assertThat(ClassUtils.getTypeArguments(Object.class)).isEmpty();
    }

    @Test
    @DisplayName("泛型参数未绑定的原始使用回退为 Object（锁定当前行为）")
    void shouldFallbackToObjectForRawGenericType() {
        // AbstractList 自身未绑定实参，Hutool TypeUtil 将未确定实参解析为 Object
        assertThat(ClassUtils.getTypeArguments(AbstractList.class)).containsExactly(Object.class);
    }
}
