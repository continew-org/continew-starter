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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 集合工具类测试
 *
 * <p>
 * 锁定 mapToList / mapToSet 对空集合、null 元素与 null 映射结果的过滤契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CollUtilsTest {

    /**
     * 构造包含 null 元素的目标集合（Arrays.asList 允许 null 成员）
     *
     * @param values 集合元素
     * @return 含 null 成员的集合
     */
    private static Collection<String> buildNullableCollection(String... values) {
        return Arrays.asList(values);
    }

    /**
     * 断言函数：在映射前返回入参，null 入参返回 null，用于同时覆盖处理前与处理后的 null
     *
     * @return 恒等映射函数
     */
    private static Function<String, String> identityMapper() {
        return value -> value;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 与空集合的 mapToList 返回空列表")
    void shouldReturnEmptyListForNullAndEmptyCollection(Collection<String> collection) {
        assertThat(CollUtils.mapToList(collection, String::length)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 与空集合的 mapToSet 返回空集合")
    void shouldReturnEmptySetForNullAndEmptyCollection(Collection<String> collection) {
        assertThat(CollUtils.mapToSet(collection, String::length)).isEmpty();
    }

    @Test
    @DisplayName("mapToList 默认过滤 null 元素与 null 映射结果")
    void shouldFilterNullWhenMapToListByDefault() {
        Collection<String> collection = buildNullableCollection("abc", null, "de", "f");
        List<Integer> result = CollUtils.mapToList(collection, String::length);
        assertThat(result).containsExactly(3, 2, 1);
    }

    @Test
    @DisplayName("mapToList ignoreNull 为 false 时保留 null 元素与 null 映射结果")
    void shouldKeepNullWhenMapToListWithoutIgnore() {
        Collection<String> collection = buildNullableCollection("abc", null);
        List<String> result = CollUtils.mapToList(collection, identityMapper(), false);
        assertThat(result).containsExactly("abc", null);
    }

    @Test
    @DisplayName("mapToList 保留重复映射结果")
    void shouldKeepDuplicatesWhenMapToList() {
        List<Integer> result = CollUtils.mapToList(Arrays.asList("aa", "bb"), value -> 2);
        assertThat(result).containsExactly(2, 2);
    }

    @Test
    @DisplayName("mapToSet 默认过滤 null 且按集合语义去重")
    void shouldDeduplicateWhenMapToSetByDefault() {
        Collection<String> collection = buildNullableCollection("abc", "de", "f", null, "x");
        Set<Integer> result = CollUtils.mapToSet(collection, String::length);
        assertThat(result).containsExactlyInAnyOrder(3, 2, 1);
    }

    @Test
    @DisplayName("mapToSet ignoreNull 为 false 时保留 null 映射结果")
    void shouldKeepNullWhenMapToSetWithoutIgnore() {
        Collection<String> collection = buildNullableCollection("abc", null);
        Set<String> result = CollUtils.mapToSet(collection, identityMapper(), false);
        assertThat(result).containsExactlyInAnyOrder("abc", null);
    }

    @Test
    @DisplayName("恒等映射下 mapToList 输出顺序与原集合一致")
    void shouldKeepOrderWhenMapToList() {
        Collection<String> collection = buildNullableCollection("a", "bb", "ccc");
        assertThat(CollUtils.mapToList(collection, identityMapper())).containsExactly("a", "bb",
            "ccc");
    }
}
