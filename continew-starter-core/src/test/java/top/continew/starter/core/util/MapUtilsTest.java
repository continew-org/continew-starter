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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Map 工具类测试
 *
 * <p>
 * 锁定 toProperties 的键值搬移与 mergeMap 的深度合并、覆盖优先级契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MapUtilsTest {

    @Test
    @DisplayName("toProperties 将 map 键值全量搬移")
    void shouldConvertMapToProperties() {
        Map<String, String> source = Map.of("continew.name", "starter", "continew.version", "3");
        Properties properties = MapUtils.toProperties(source);
        assertThat(properties)
            .containsEntry("continew.name", "starter")
            .containsEntry("continew.version", "3")
            .hasSize(2);
    }

    @Test
    @DisplayName("toProperties 对空 map 返回空 Properties")
    void shouldReturnEmptyPropertiesForEmptyMap() {
        assertThat(MapUtils.toProperties(Map.of())).isEmpty();
    }

    @Test
    @DisplayName("mergeMap 在 to 为空时直接返回 from")
    void shouldReturnFromWhenToEmpty() {
        Map<String, Object> from = Map.of("k", "v");
        assertThat(MapUtils.mergeMap(new HashMap<>(), from)).isSameAs(from);
    }

    @Test
    @DisplayName("mergeMap 在 from 为空时直接返回 to")
    void shouldReturnToWhenFromEmpty() {
        Map<String, Object> to = Map.of("k", "v");
        assertThat(MapUtils.mergeMap(to, new HashMap<>())).isSameAs(to);
    }

    @Test
    @DisplayName("mergeMap 在两侧均为 null 时返回 null（锁定当前行为：空侧短路返回原引用）")
    void shouldReturnNullWhenBothNull() {
        assertThat(MapUtils.mergeMap(null, null)).isNull();
    }

    @Test
    @DisplayName("mergeMap 以 from 的标量值覆盖 to 的同名键")
    void shouldOverrideScalarFromFrom() {
        Map<String, Object> to = new HashMap<>();
        to.put("name", "old");
        to.put("keep", "kept");
        Map<String, Object> from = new HashMap<>();
        from.put("name", "new");
        from.put("extra", "added");
        Map<String, Object> merged = MapUtils.mergeMap(to, from);
        assertThat(merged)
            .containsEntry("name", "new")
            .containsEntry("keep", "kept")
            .containsEntry("extra", "added");
    }

    @Test
    @DisplayName("mergeMap 对嵌套 map 递归合并，from 子键补充进 to 子 map")
    void shouldMergeNestedMapRecursively() {
        Map<String, Object> toChild = new HashMap<>();
        toChild.put("a", 1);
        Map<String, Object> to = new HashMap<>();
        to.put("child", toChild);
        Map<String, Object> fromChild = new HashMap<>();
        fromChild.put("b", 2);
        Map<String, Object> from = new HashMap<>();
        from.put("child", fromChild);
        Map<String, Object> merged = MapUtils.mergeMap(to, from);
        assertThat(merged).containsKey("child");
        Map<?, ?> mergedChild = (Map<?, ?>) merged.get("child");
        assertThat(mergedChild.get("a")).isEqualTo(1);
        assertThat(mergedChild.get("b")).isEqualTo(2);
    }

    @Test
    @DisplayName("mergeMap 中 from 值为 null 的键不覆盖 to 的既有值")
    void shouldKeepToValueWhenFromValueNull() {
        Map<String, Object> to = new HashMap<>();
        to.put("name", "kept");
        Map<String, Object> from = new HashMap<>();
        from.put("name", null);
        assertThat(MapUtils.mergeMap(to, from)).containsEntry("name", "kept");
    }
}
