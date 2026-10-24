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

package top.continew.starter.json.jackson.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.continew.starter.json.jackson.exception.JsonException;
import top.continew.starter.json.jackson.util.JsonBuilder;
import top.continew.starter.json.jackson.util.JsonUtils;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具类测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest
class JsonUtilsTest {

    @Test
    @DisplayName("toJsonStr 空值返回 null")
    void toJsonStrShouldReturnNullOnNull() {
        assertNull(JsonUtils.toJsonStr(null));
    }

    @Test
    @DisplayName("toJsonStr 序列化对象")
    void toJsonStrShouldSerializeObject() {
        TestModel model = new TestModel();
        model.setId(1L);
        model.setName("test");
        String json = JsonUtils.toJsonStr(model);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"test\""));
    }

    @Test
    @DisplayName("toBean 空字符串返回 null")
    void toBeanShouldReturnNullOnBlank() {
        assertNull(JsonUtils.toBean(" ", TestModel.class));
    }

    @Test
    @DisplayName("toBean 反序列化对象")
    void toBeanShouldDeserializeObject() {
        TestModel model = JsonUtils.toBean("{\"id\":1,\"name\":\"test\"}", TestModel.class);
        assertEquals(1L, model.getId());
        assertEquals("test", model.getName());
    }

    @Test
    @DisplayName("toBean 非法 JSON 抛出 JsonException")
    void toBeanShouldThrowJsonExceptionOnInvalidJson() {
        assertThrows(JsonException.class, () -> JsonUtils.toBean("{invalid", TestModel.class));
    }

    @Test
    @DisplayName("toList 反序列化集合")
    void toListShouldDeserializeList() {
        List<TestModel> list = JsonUtils
            .toList("[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]", TestModel.class);
        assertEquals(2, list.size());
        assertEquals("b", list.get(1)
            .getName());
    }

    @Test
    @DisplayName("toList 空 JSON 返回空集合")
    void toListShouldReturnEmptyListOnNull() {
        String json = null;
        assertTrue(JsonUtils.toList(json, TestModel.class)
            .isEmpty());
    }

    @Test
    @DisplayName("parseObj 转换为 JsonNode")
    void parseObjShouldConvertToJsonNode() {
        Map<String, Object> map = Map.of("key", "value");
        var node = JsonUtils.parseObj(map);
        assertEquals("value", node.get("key")
            .asString());
    }

    @Test
    @DisplayName("parseObj 空值返回 null")
    void parseObjShouldReturnNullOnNull() {
        assertNull(JsonUtils.parseObj(null));
    }

    @Test
    @DisplayName("isTypeJson 判定")
    void isTypeJsonShouldJudgeCorrectly() {
        assertTrue(JsonUtils.isTypeJson("{\"a\":1}"));
        assertTrue(JsonUtils.isTypeJson("[1,2]"));
        assertFalse(JsonUtils.isTypeJson("plain"));
        assertFalse(JsonUtils.isTypeJson(""));
    }

    @Test
    @DisplayName("JsonBuilder 链式构建 JSON")
    void jsonBuilderShouldBuildJson() {
        String json = JsonBuilder.builder()
            .add("name", "test")
            .add("count", 1)
            .add("flag", true)
            .add("list", List.of("a", "b"))
            .buildString();
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"count\":1"));
        assertTrue(json.contains("\"flag\":true"));
        assertTrue(json.contains("\"list\":[\"a\",\"b\"]"));
    }

    @Test
    @DisplayName("JsonBuilder 空键抛出 NullPointerException")
    void jsonBuilderShouldThrowOnNullKey() {
        JsonBuilder builder = JsonBuilder.builder();
        assertThrows(NullPointerException.class, () -> builder.add(null, "v"));
    }
}
