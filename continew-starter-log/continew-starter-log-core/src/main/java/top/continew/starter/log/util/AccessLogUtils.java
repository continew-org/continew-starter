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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.model.AccessLogProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author echo
 * @since 2025/03/25 19:16
 **/
public class AccessLogUtils {

    public AccessLogUtils() {
    }

    /**
     * 获取参数信息
     *
     * @param request    请求
     * @param properties 属性
     * @return {@link String }
     */
    public static String getParam(RecordableHttpRequest request, AccessLogProperties properties) {
        // 是否需要输出参数
        if (!properties.isReqParams()) {
            return null;
        }

        // 参数为空返回空
        Map<String, Object> params = request.getParam();
        if (ObjectUtil.isEmpty(params) || params.isEmpty()) {
            return null;
        }

        // 是否需要对特定入参脱敏
        if (properties.isSensitiveParams()) {
            params = filterSensitiveParams(params, properties.getSensitiveParamList());
        }

        // 是否自动截断超长参数值
        if (properties.isTruncateLongParams()) {
            params = truncateLongParams(params, properties.getUltraLongParamThreshold(), properties
                .getUltraLongParamMaxLength(), properties.getTruncateSuffix());
        }
        return "param:" + JSONUtil.toJsonStr(params);
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
}
