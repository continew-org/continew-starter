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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import top.continew.starter.log.model.AccessLogProperties;
import top.continew.starter.log.model.LogProperties;
import top.continew.starter.core.util.ServletUtils;
import top.continew.starter.core.util.SpringWebUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 访问日志工具类
 *
 * @author echo
 * @author Charles7c
 * @since 2.10.0
 */
public class AccessLogUtils {

    /**
     * 静态资源路径模式
     */
    private static final List<String> RESOURCE_PATH = List
        .of("/**/doc/**", "/**/doc.html", "/**/nextdoc/**", "/**/v*/api-docs/**", "/**/api-docs/**", "/**/swagger-ui/**", "/**/swagger-ui.html", "/**/swagger-resources/**", "/**/webjars/**", "/**/favicon.ico", "/**/static/**", "/**/assets/**", "/**/actuator/**", "/error", "/health");

    private AccessLogUtils() {
    }

    /**
     * 获取参数信息
     *
     * @param properties 属性
     * @return {@link String }
     */
    public static String getParam(AccessLogProperties properties) {
        // 是否需要打印请求参数
        if (!properties.isPrintRequestParam()) {
            return null;
        }

        // 参数为空返回空
        Object params;
        try {
            params = getAccessLogReqParam();
        } catch (Exception e) {
            return null;
        }
        if (ObjectUtil.isEmpty(params)) {
            return null;
        }

        // 是否需要对特定入参脱敏
        if (properties.isParamSensitive()) {
            params = processSensitiveParams(params, properties.getSensitiveParams());
        }

        // 是否自动截断超长参数值
        if (properties.isLongParamTruncate()) {
            params = processTruncateLongParams(params, properties.getLongParamThreshold(), properties
                .getLongParamMaxLength(), properties.getLongParamSuffix());
        }
        return JSONUtil.toJsonStr(params);
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
        return properties.isMatch(path) || RESOURCE_PATH.stream()
            .anyMatch(resourcePath -> SpringWebUtils.isMatchAnt(path, resourcePath));
    }

    /**
     * 处理敏感参数，支持 Map 和 List<Map<String, Object>> 类型
     *
     * @param params          参数
     * @param sensitiveParams 敏感参数列表
     * @return 处理后的参数
     */
    private static Object processSensitiveParams(Object params, List<String> sensitiveParams) {
        if (params instanceof Map) {
            return filterSensitiveParams((Map<String, Object>)params, sensitiveParams);
        } else if (params instanceof List) {
            return ((List<?>)params).stream()
                .filter(item -> item instanceof Map)
                .map(item -> filterSensitiveParams((Map<String, Object>)item, sensitiveParams))
                .collect(Collectors.toList());
        }
        return params;
    }

    /**
     * 过滤敏感参数
     *
     * @param params          参数 Map
     * @param sensitiveParams 敏感参数列表
     * @return 处理后的参数 Map
     */
    private static Map<String, Object> filterSensitiveParams(Map<String, Object> params, List<String> sensitiveParams) {
        if (params == null || params.isEmpty() || sensitiveParams == null || sensitiveParams.isEmpty()) {
            return params;
        }

        Map<String, Object> filteredParams = new HashMap<>(params);
        for (String sensitiveKey : sensitiveParams) {
            if (filteredParams.containsKey(sensitiveKey)) {
                filteredParams.put(sensitiveKey, "***");
            }
        }
        return filteredParams;
    }

    /**
     * 处理超长参数，支持 Map 和 List<Map<String, Object>> 类型
     *
     * @param params    参数
     * @param threshold 截断阈值（值长度超过该值才截断）
     * @param maxLength 最大长度
     * @param suffix    后缀（如 "..."）
     * @return 处理后的参数
     */
    private static Object processTruncateLongParams(Object params, int threshold, int maxLength, String suffix) {
        if (params instanceof Map) {
            return truncateLongParams((Map<String, Object>)params, threshold, maxLength, suffix);
        } else if (params instanceof List) {
            return ((List<?>)params).stream()
                .filter(Map.class::isInstance)
                .map(item -> truncateLongParams((Map<String, Object>)item, threshold, maxLength, suffix))
                .collect(Collectors.toList());
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
     * @return 处理后的参数 Map
     */
    private static Map<String, Object> truncateLongParams(Map<String, Object> params,
                                                          int threshold,
                                                          int maxLength,
                                                          String suffix) {
        if (params == null || params.isEmpty()) {
            return params;
        }

        Map<String, Object> truncatedParams = new HashMap<>(params);
        for (Map.Entry<String, Object> entry : truncatedParams.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strValue) {
                if (strValue.length() > threshold) {
                    entry.setValue(strValue.substring(0, Math.min(strValue.length(), maxLength)) + suffix);
                }
            }
        }
        return truncatedParams;
    }

    /**
     * 获取访问日志请求参数
     *
     * @return {@link Object }
     */
    private static Object getAccessLogReqParam() {
        String body = ServletUtils.getRequestBody();
        if (CharSequenceUtil.isNotBlank(body) && JSONUtil.isTypeJSON(body)) {
            try {
                if (JSONUtil.isTypeJSONArray(body)) {
                    return JSONUtil.toBean(body, List.class);
                } else {
                    return JSONUtil.toBean(body, Map.class);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return Collections.unmodifiableMap(ServletUtils.getRequestParams());
    }
}
