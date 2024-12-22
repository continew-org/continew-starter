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

package top.continew.starter.log.enums;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 日志包含信息
 *
 * @author Wallace Wadge（Spring Boot Actuator）
 * @author Emily Tsanova（Spring Boot Actuator）
 * @author Joseph Beeton（Spring Boot Actuator）
 * @author Charles7c
 * @since 1.1.0
 */
public enum Include {

    /**
     * 描述
     */
    DESCRIPTION,

    /**
     * 模块
     */
    MODULE,

    /**
     * 请求头（默认）
     */
    REQUEST_HEADERS,

    /**
     * 请求体（如包含请求体，则请求参数无效）
     */
    REQUEST_BODY,

    /**
     * 请求参数（默认）
     */
    REQUEST_PARAM,

    /**
     * IP 归属地
     */
    IP_ADDRESS,

    /**
     * 浏览器
     */
    BROWSER,

    /**
     * 操作系统
     */
    OS,

    /**
     * 响应头（默认）
     */
    RESPONSE_HEADERS,

    /**
     * 响应体（如包含响应体，则响应参数无效）
     */
    RESPONSE_BODY,

    /**
     * 响应参数（默认）
     */
    RESPONSE_PARAM,;

    private static final Set<Include> DEFAULT_INCLUDES;

    static {
        Set<Include> defaultIncludes = new LinkedHashSet<>();
        defaultIncludes.add(Include.REQUEST_HEADERS);
        defaultIncludes.add(Include.RESPONSE_HEADERS);
        defaultIncludes.add(Include.REQUEST_PARAM);
        defaultIncludes.add(Include.RESPONSE_PARAM);
        DEFAULT_INCLUDES = Collections.unmodifiableSet(defaultIncludes);
    }

    /**
     * 获取默认包含信息
     *
     * @return 默认包含信息
     */
    public static Set<Include> defaultIncludes() {
        return DEFAULT_INCLUDES;
    }
}