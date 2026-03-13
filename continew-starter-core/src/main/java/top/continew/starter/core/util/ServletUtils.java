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

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.core.constant.StringConstants;

import java.util.Collection;
import java.util.Map;

/**
 * Servlet 工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class ServletUtils extends JakartaServletUtil {

    private ServletUtils() {
    }

    /**
     * 获取浏览器及其版本信息
     *
     * @param request 请求对象
     * @return 浏览器及其版本信息
     */
    public static String getBrowser(HttpServletRequest request) {
        if (request == null) {
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
        try {
            UserAgent userAgent = UserAgentUtil.parse(userAgentString);
            if (userAgent == null || userAgent.getBrowser() == null) {
                return null;
            }
            String browserName = userAgent.getBrowser().getName();
            String version = userAgent.getVersion();
            return CharSequenceUtil.isBlank(version) ? browserName : browserName + StringConstants.SPACE + version;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取操作系统
     *
     * @param request 请求对象
     * @return 操作系统
     */
    public static String getOs(HttpServletRequest request) {
        if (request == null) {
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
        try {
            UserAgent userAgent = UserAgentUtil.parse(userAgentString);
            if (userAgent == null || userAgent.getOs() == null) {
                return null;
            }
            return userAgent.getOs().getName();
        } catch (Exception e) {
            return null;
        }
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

    /**
     * 获取 HTTP Session
     *
     * @return HttpSession
     * @since 2.11.0
     */
    public static HttpSession getSession() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getSession() : null;
    }

    /**
     * 获取 HTTP Request
     *
     * @return HttpServletRequest
     * @since 2.11.0
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    /**
     * 获取 HTTP Response
     *
     * @return HttpServletResponse
     * @since 2.11.0
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getResponse();
    }

    /**
     * 获取请求属性
     *
     * @return {@link ServletRequestAttributes }
     * @since 2.11.0
     */
    public static ServletRequestAttributes getRequestAttributes() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            return (ServletRequestAttributes)attributes;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 响应 JSON 数据给客户端
     *
     * @param response 响应对象
     * @param data     响应数据
     * @since 2.13.1
     * @see #write(HttpServletResponse, String, String)
     */
    public static void writeJSON(HttpServletResponse response, String data) {
        write(response, data, MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 检查请求是否为 {@code multipart/form-data} 格式（常用于文件上传）
     *
     * @param request 请求对象
     * @return true: 是; false: 否
     * @since 2.15.1
     */
    public static boolean isMultipart(HttpServletRequest request) {
        return StrUtil.startWithIgnoreCase(request.getContentType(), "multipart/");
    }

    /**
     * 检查 HTTP 请求是否为 {@code application/x-www-form-urlencoded} 格式（标准表单提交）
     *
     * @param request 请求对象
     * @return true: 是; false: 否
     * @see MediaType#APPLICATION_FORM_URLENCODED_VALUE
     * @since 2.15.1
     */
    public static boolean isForm(HttpServletRequest request) {
        return StrUtil.contains(request.getContentType(), MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    /**
     * 检查 HTTP 响应是否为 {@code Server-Sent Events (SSE)} 流格式
     *
     * @param response 响应对象
     * @return true: 是; false: 否
     * @see MediaType#TEXT_EVENT_STREAM_VALUE
     * @since 2.15.1
     */
    public static boolean isStream(HttpServletResponse response) {
        return StrUtil.contains(response.getContentType(), MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
