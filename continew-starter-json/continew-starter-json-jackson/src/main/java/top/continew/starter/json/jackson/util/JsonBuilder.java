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

package top.continew.starter.json.jackson.util;

import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON 构建工具
 *
 * @see ObjectMapper
 *
 * @author echo
 * @since 2.11.0
 */
public class JsonBuilder {

    private static final ObjectMapper OBJECT_MAPPER = SpringUtil.getBean(ObjectMapper.class);
    private final ObjectNode rootNode;

    private JsonBuilder() {
        this.rootNode = OBJECT_MAPPER.createObjectNode();
    }

    /**
     * 开始构建
     *
     * @return {@link JsonBuilder }
     */
    public static JsonBuilder builder() {
        return new JsonBuilder();
    }

    /**
     * 添加 字符串
     *
     * @param key   key 值
     * @param value 值
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, String value) {
        Objects.requireNonNull(key, "键不能为 null");
        if (value != null) {
            rootNode.put(key, value);
        }
        return this;
    }

    /**
     * 添加 int
     *
     * @param key   key 值
     * @param value 值
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, int value) {
        Objects.requireNonNull(key, "键不能为 null");
        rootNode.put(key, value);
        return this;
    }

    /**
     * 添加 long
     *
     * @param key   key 值
     * @param value 值
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, long value) {
        Objects.requireNonNull(key, "键不能为 null");
        rootNode.put(key, value);
        return this;
    }

    /**
     * 添加 布尔
     *
     * @param key   key 值
     * @param value 值
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, boolean value) {
        Objects.requireNonNull(key, "键不能为 null");
        rootNode.put(key, value);
        return this;
    }

    /**
     * 添加 浮点
     *
     * @param key   key 值
     * @param value 值
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, double value) {
        Objects.requireNonNull(key, "键不能为 null");
        rootNode.put(key, value);
        return this;
    }

    /**
     * 添加 json
     *
     * @param key   key 值
     * @param value 值
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, JsonNode value) {
        Objects.requireNonNull(key, "键不能为 null");
        if (value != null) {
            rootNode.set(key, value);
        }
        return this;
    }

    /**
     * 添加 Object
     *
     * @param key   key 值
     * @param value 值
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, Object value) {
        Objects.requireNonNull(key, "键不能为 null");
        if (value != null) {
            rootNode.set(key, OBJECT_MAPPER.valueToTree(value));
        }
        return this;
    }

    /**
     * 添加 List 到 JSON
     *
     * @param key  key 值
     * @param list list 参数
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, List<?> list) {
        Objects.requireNonNull(key, "键不能为 null");
        if (list != null) {
            ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
            for (Object item : list) {
                arrayNode.add(OBJECT_MAPPER.valueToTree(item));
            }
            rootNode.set(key, arrayNode);
        }
        return this;
    }

    /**
     * 添加 Map 到 JSON
     *
     * @param key key 值
     * @param map map 参数
     * @return {@link JsonBuilder }
     */
    public JsonBuilder add(String key, Map<?, ?> map) {
        Objects.requireNonNull(key, "键不能为 null");
        if (map != null) {
            ObjectNode objectNode = OBJECT_MAPPER.valueToTree(map);
            rootNode.set(key, objectNode);
        }
        return this;
    }

    /**
     * 构建
     *
     * @return {@link JsonNode }
     */
    public JsonNode build() {
        return rootNode;
    }

    /**
     * 构建 json 字符串
     *
     * @return {@link String }
     */
    public String buildString() {
        try {
            return rootNode.toString();
        } catch (Exception e) {
            throw new RuntimeException("构建 JSON 字符串失败", e);
        }
    }
}
