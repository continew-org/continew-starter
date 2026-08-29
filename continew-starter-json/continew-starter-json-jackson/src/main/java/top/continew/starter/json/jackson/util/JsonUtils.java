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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.continew.starter.json.jackson.exception.JsonException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 工具类
 *
 * @see ObjectMapper
 * @see cn.hutool.json.JSONUtil
 *
 * @author echo
 * @author Charles7c
 * @since 2.11.0
 */
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = SpringUtil.getBean(ObjectMapper.class);

    private JsonUtils() {
    }

    /**
     * 获取 Jackson 对象映射器
     *
     * @return {@link ObjectMapper} Jackson 对象映射器
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 转换对象为JsonNode<br>
     *
     * @param obj 对象
     * @return JsonNode
     */
    public static JsonNode parseObj(Object obj) {
        if (obj == null) {
            return null;
        }
        return OBJECT_MAPPER.valueToTree(obj);
    }

    /**
     * 转换为JSON字符串
     *
     * @param obj 被转为JSON的对象
     * @return JSON字符串
     */
    public static String toJsonStr(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException(e);
        }
    }

    /**
     * JSON字符串转为实体类对象，转换异常将被抛出
     *
     * @param <T>        Bean类型
     * @param jsonString JSON字符串
     * @param beanClass  实体类对象
     * @return 实体类对象
     */
    public static <T> T toBean(String jsonString, Class<T> beanClass) {
        if (CharSequenceUtil.isBlank(jsonString)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonString, beanClass);
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }

    /**
     * 将JSON字符串转换为Bean的List，默认为ArrayList
     *
     * @param jsonStr     需要转换的JSON字符串
     * @param elementType 列表元素类型
     * @return 转换后的 List
     * @since 2.13.2
     */
    public static <T> List<T> toList(String jsonStr, Class<T> elementType) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new ArrayList<>(0);
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr, OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new JsonException("Failed to parse JSON string to list", e);
        }
    }

    /**
     * 将JSONArray转换为Bean的List，默认为ArrayList
     *
     * @param jsonNode    需要转换的 JsonNode
     * @param elementType 列表元素类型
     * @return 转换后的 List
     * @since 2.13.2
     */
    public static <T> List<T> toList(JsonNode jsonNode, Class<T> elementType) {
        if (jsonNode == null || jsonNode.isNull()) {
            return new ArrayList<>(0);
        }
        try {
            return OBJECT_MAPPER.convertValue(jsonNode, OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(List.class, elementType));
        } catch (IllegalArgumentException e) {
            throw new JsonException("Failed to convert JSON to list", e);
        }
    }

    /**
     * 是否为JSON类型字符串，首尾都为大括号或中括号判定为JSON字符串
     *
     * @param str 字符串
     * @return 是否为JSON类型字符串
     * @since 2.13.2
     * @see cn.hutool.json.JSONUtil#isTypeJson(String)
     * @author Looly（<a href="https://gitee.com/dromara/hutool">Hutool</a>）
     */
    public static boolean isTypeJson(String str) {
        return isTypeJsonObject(str) || isTypeJsonArray(str);
    }

    /**
     * 是否为JSONObject类型字符串，首尾都为大括号判定为JSONObject字符串
     *
     * @param str 字符串
     * @return 是否为JSON字符串
     * @since 2.13.2
     * @see cn.hutool.json.JSONUtil#isTypeJsonObject(String)
     * @author Looly（<a href="https://gitee.com/dromara/hutool">Hutool</a>）
     */
    public static boolean isTypeJsonObject(String str) {
        if (CharSequenceUtil.isBlank(str)) {
            return false;
        }
        return CharSequenceUtil.isWrap(CharSequenceUtil.trim(str), '{', '}');
    }

    /**
     * 是否为JSONArray类型的字符串，首尾都为中括号判定为JSONArray字符串
     *
     * @param str 字符串
     * @return 是否为JSONArray类型字符串
     * @since 2.13.2
     * @see cn.hutool.json.JSONUtil#isTypeJsonArray(String)
     * @author Looly（<a href="https://gitee.com/dromara/hutool">Hutool</a>）
     */
    public static boolean isTypeJsonArray(String str) {
        if (CharSequenceUtil.isBlank(str)) {
            return false;
        }
        return CharSequenceUtil.isWrap(CharSequenceUtil.trim(str), '[', ']');
    }
}
