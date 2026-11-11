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

package top.continew.starter.log.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.core.util.SpringUtils;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.model.AccessLogProperties;
import top.continew.starter.log.model.LogProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 访问日志工具类
 *
 * @author echo
 * @author Charles7c
 * @since 2.10.0
 */
public class AccessLogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogUtils.class);

    /**
     * 敏感值掩码
     */
    private static final String MASK = "***";

    /**
     * 参数处理的嵌套深度上限，超过后按整体掩码处理，避免客户端构造深层嵌套耗尽栈
     */
    private static final int MAX_PARAM_DEPTH = 10;

    /**
     * 解析前的嵌套层级预检上限：Hutool 的 JSON 解析本身是递归实现，过深会先耗尽栈
     */
    private static final int MAX_JSON_NESTING = 32;

    /**
     * 静态资源路径模式
     */
    private static final List<String> RESOURCE_PATH = List
        .of("/**/doc/**", "/**/doc.html", "/**/nextdoc/**", "/**/v*/api-docs/**", "/**/api-docs/**",
            "/**/swagger-ui/**", "/**/swagger-ui.html", "/**/swagger-resources/**",
            "/**/webjars/**", "/**/favicon.ico",
            "/**/static/**", "/**/assets/**", "/**/actuator/**", "/error", "/health");

    private AccessLogUtils() {
    }

    /**
     * 获取参数信息
     *
     * <p>
     * 参数不是可解析的 JSON 时无法按字段脱敏：未开启脱敏则按原始文本输出，开启脱敏则跳过参数记录。
     * </p>
     *
     * @param request    请求对象
     * @param properties 属性
     * @return {@link String }
     */
    public static String getParam(RecordableHttpRequest request, AccessLogProperties properties) {
        // 是否需要打印请求参数
        if (!properties.isPrintRequestParam()) {
            return null;
        }

        // 参数为空返回空
        String params = request.getParams();
        if (CharSequenceUtil.isBlank(params)) {
            return null;
        }

        Object paramObj = params;
        if (!isTooDeeplyNested(params)) {
            try {
                if (JSONUtil.isTypeJSONArray(params)) {
                    paramObj = JSONUtil.parseArray(params);
                } else if (JSONUtil.isTypeJSONObject(params)) {
                    paramObj = JSONUtil.parseObj(params);
                }
            } catch (JSONException e) {
                // 类型判断只看首尾字符，无法保证可解析；异常信息会带上原文，故不打印堆栈
                LOGGER.debug("请求参数不是合法 JSON，按原始文本处理");
            }
        }

        // 脱敏依赖 JSON 结构，非 JSON 文本无法按字段脱敏，配置了敏感字段时跳过参数记录
        if (properties.isParamSensitive() && CollectionUtil
            .isNotEmpty(properties.getSensitiveParams()) && !(paramObj instanceof Map)
            && !(paramObj instanceof List)) {
            LOGGER.debug("参数无法脱敏，已跳过参数记录");
            return null;
        }

        // 是否需要对特定入参脱敏
        if (properties.isParamSensitive()) {
            paramObj = processSensitiveParams(paramObj, properties.getSensitiveParams());
        }

        // 是否自动截断超长参数值
        if (properties.isLongParamTruncate()) {
            paramObj =
                processTruncateLongParams(paramObj, properties.getLongParamThreshold(), properties
                    .getLongParamMaxLength(), properties.getLongParamSuffix());
        }
        return JSONUtil.toJsonStr(paramObj);
    }

    /**
     * 排除路径
     *
     * @param properties 属性
     * @param path       路径
     * @return boolean
     */
    public static boolean exclusionPath(LogProperties properties, String path) {
        // 放行路由配置的排除检查
        return properties.isMatchExcludeUri(path) || RESOURCE_PATH.stream()
            .anyMatch(resourcePath -> SpringUtils.isMatchAnt(path, resourcePath));
    }

    /**
     * 预检参数嵌套层级，避免递归解析过深导致栈溢出
     *
     * <p>
     * 统计字符串字面量之外的括号层级，且深度不会降到 0 以下，防止请求体里的方括号
     * 抵消计数后绕过检查。
     * </p>
     *
     * @param params 原始参数
     * @return 是否超过解析上限
     */
    private static boolean isTooDeeplyNested(String params) {
        if (params == null) {
            return false;
        }
        int depth = 0;
        int index = 0;
        while (index < params.length()) {
            char ch = params.charAt(index);
            if (ch == '"') {
                index = skipString(params, index);
            } else if (ch == '[' || ch == '{') {
                if (++depth > MAX_JSON_NESTING) {
                    return true;
                }
            } else if ((ch == ']' || ch == '}') && depth > 0) {
                depth--;
            }
            index++;
        }
        return false;
    }

    /**
     * 跳过 JSON 字符串字面量
     *
     * @param params 原始参数
     * @param start  起始引号下标
     * @return 结束引号下标，未闭合时返回最后一个字符下标
     */
    private static int skipString(String params, int start) {
        int i = start + 1;
        while (i < params.length()) {
            char ch = params.charAt(i);
            if (ch == '\\') {
                i += 2;
                continue;
            }
            if (ch == '"') {
                return i;
            }
            i++;
        }
        return params.length() - 1;
    }

    /**
     * 处理敏感参数，支持 {@code Map}、{@code List} 及二者的嵌套组合
     *
     * @param params          参数
     * @param sensitiveParams 敏感参数列表
     * @return 处理后的参数
     */
    private static Object processSensitiveParams(Object params, List<String> sensitiveParams) {
        return processSensitiveParams(params, sensitiveParams, 0);
    }

    /**
     * 处理敏感参数，超出嵌套深度上限后整体掩码
     *
     * @param params          参数
     * @param sensitiveParams 敏感参数列表
     * @param depth           当前嵌套深度
     * @return 处理后的参数
     */
    private static Object processSensitiveParams(Object params, List<String> sensitiveParams,
        int depth) {
        if (depth > MAX_PARAM_DEPTH) {
            return MASK;
        }
        if (params instanceof Map) {
            return filterSensitiveParams((Map<String, Object>) params, sensitiveParams, depth);
        } else if (params instanceof List) {
            // 数组元素可能是嵌套数组，逐项递归才能覆盖内层对象；标量元素原样返回
            return ((List<?>) params).stream()
                .map(item -> processSensitiveParams(item, sensitiveParams, depth + 1))
                .toList();
        }
        return params;
    }

    /**
     * 过滤敏感参数
     *
     * @param params          参数 Map
     * @param sensitiveParams 敏感参数列表
     * @param depth           当前嵌套深度
     * @return 处理后的参数 Map
     */
    private static Map<String, Object> filterSensitiveParams(Map<String, Object> params,
        List<String> sensitiveParams,
        int depth) {
        if (params == null || params.isEmpty() || sensitiveParams == null
            || sensitiveParams.isEmpty()) {
            return params;
        }

        Map<String, Object> filteredParams = new LinkedHashMap<>(params);
        for (Map.Entry<String, Object> entry : filteredParams.entrySet()) {
            Object value = entry.getValue();
            if (sensitiveParams.contains(entry.getKey())) {
                entry.setValue(MASK);
            } else if (value instanceof Map || value instanceof List) {
                entry.setValue(processSensitiveParams(value, sensitiveParams, depth + 1));
            }
        }
        return filteredParams;
    }

    /**
     * 处理超长参数，支持 {@code Map}、{@code List}、字符串及三者的嵌套组合
     *
     * @param params    参数
     * @param threshold 截断阈值（值长度超过该值才截断）
     * @param maxLength 最大长度
     * @param suffix    后缀（如 "..."）
     * @return 处理后的参数
     */
    private static Object processTruncateLongParams(Object params, int threshold, int maxLength,
        String suffix) {
        return processTruncateLongParams(params, threshold, maxLength, suffix, 0);
    }

    /**
     * 处理超长参数，超出嵌套深度上限后整体掩码
     *
     * @param params    参数
     * @param threshold 截断阈值（值长度超过该值才截断）
     * @param maxLength 最大长度
     * @param suffix    后缀（如 "..."）
     * @param depth     当前嵌套深度
     * @return 处理后的参数
     */
    private static Object processTruncateLongParams(Object params, int threshold, int maxLength,
        String suffix, int depth) {
        if (depth > MAX_PARAM_DEPTH) {
            return MASK;
        }
        if (params instanceof Map) {
            return truncateLongParams((Map<String, Object>) params, threshold, maxLength, suffix,
                depth);
        } else if (params instanceof List) {
            return ((List<?>) params).stream()
                .map(item -> processTruncateLongParams(item, threshold, maxLength, suffix,
                    depth + 1))
                .toList();
        } else if (params instanceof String strValue) {
            return truncate(strValue, threshold, maxLength, suffix);
        }
        return params;
    }

    /**
     * 截断超长参数
     *
     * @param params    参数 Map
     * @param threshold 截断阈值（值长度超过该值才截断）
     * @param maxLength 最大长度
     * @param suffix    后缀（如 "..."）
     * @param depth     当前嵌套深度
     * @return 处理后的参数 Map
     */
    private static Map<String, Object> truncateLongParams(Map<String, Object> params,
        int threshold,
        int maxLength,
        String suffix,
        int depth) {
        if (params == null || params.isEmpty()) {
            return params;
        }

        Map<String, Object> truncatedParams = new LinkedHashMap<>(params);
        for (Map.Entry<String, Object> entry : truncatedParams.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strValue) {
                entry.setValue(truncate(strValue, threshold, maxLength, suffix));
            } else if (value instanceof Map || value instanceof List) {
                entry.setValue(processTruncateLongParams(value, threshold, maxLength, suffix,
                    depth + 1));
            }
        }
        return truncatedParams;
    }

    /**
     * 截断超长字符串
     *
     * @param value     原始值
     * @param threshold 截断阈值（长度超过该值才截断）
     * @param maxLength 最大长度
     * @param suffix    后缀（如 "..."）
     * @return 截断后的值
     */
    private static String truncate(String value, int threshold, int maxLength, String suffix) {
        if (threshold < 0 || value.length() <= threshold) {
            return value;
        }
        // 保留长度不超过阈值，否则截断后反而更长；负值按 0 处理，避免 substring 越界
        int limit = Math.clamp(maxLength, 0, threshold);
        // 避免从代理对中间截断，产生孤立代理字符
        if (limit > 0 && Character.isHighSurrogate(value.charAt(limit - 1))) {
            limit--;
        }
        String truncated = value.substring(0, limit) + (suffix == null ? "" : suffix);
        // 加上后缀后未变短时保留原文，截断不能把日志变长
        return truncated.length() >= value.length() ? value : truncated;
    }
}
