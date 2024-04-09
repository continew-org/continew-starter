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

package top.continew.starter.web.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.core.constant.StringConstants;

import java.util.*;

/**
 * Servlet 工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class ServletUtils {

    private ServletUtils() {
    }

    /**
     * 获取请求对象
     *
     * @return /
     */
    public static HttpServletRequest getRequest() {
        return getServletRequestAttributes().getRequest();
    }

    /**
     * 获取响应对象
     *
     * @return /
     */
    public static HttpServletResponse getResponse() {
        return getServletRequestAttributes().getResponse();
    }

    /**
     * 获取浏览器及其版本信息
     *
     * @param request 请求对象
     * @return 浏览器及其版本信息
     */
    public static String getBrowser(HttpServletRequest request) {
        if (null == request) {
            return null;
        }
        return getBrowser(request.getHeader("User-Agent"));
    }

    /**
     * 获取浏览器及其版本信息
     *
     * @param userAgentString User-Agent 字符串
     * @return 浏览器及其版本信息
     */
    public static String getBrowser(String userAgentString) {
        UserAgent userAgent = UserAgentUtil.parse(userAgentString);
        return userAgent.getBrowser().getName() + StringConstants.SPACE + userAgent.getVersion();
    }

    /**
     * 获取操作系统
     *
     * @param request 请求对象
     * @return 操作系统
     */
    public static String getOs(HttpServletRequest request) {
        if (null == request) {
            return null;
        }
        return getOs(request.getHeader("User-Agent"));
    }

    /**
     * 获取操作系统
     *
     * @param userAgentString User-Agent 字符串
     * @return 操作系统
     */
    public static String getOs(String userAgentString) {
        UserAgent userAgent = UserAgentUtil.parse(userAgentString);
        return userAgent.getOs().getName();
    }

    /**
     * 获取响应所有的头（header）信息
     *
     * @param response 响应对象{@link HttpServletResponse}
     * @return header值
     */
    public static Map<String, String> getHeaderMap(HttpServletResponse response) {
        final Collection<String> headerNames = response.getHeaderNames();
        final Map<String, String> headerMap = MapUtil.newHashMap(headerNames.size(), true);
        for (String name : headerNames) {
            headerMap.put(name, response.getHeader(name));
        }
        return headerMap;
    }

    private static ServletRequestAttributes getServletRequestAttributes() {
        return (ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
    }
}
