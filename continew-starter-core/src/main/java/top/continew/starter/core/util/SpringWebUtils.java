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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.PathContainer;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import top.continew.starter.core.constant.StringConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Spring Web 工具类
 *
 * @author Charles7c
 * @since 1.1.1
 */
public class SpringWebUtils {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private SpringWebUtils() {
    }

    /**
     * 路径是否匹配
     *
     * @param path     路径
     * @param patterns 匹配模式列表
     * @return 是否匹配
     * @since 2.6.0
     */
    public static boolean isMatch(String path, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> isMatch(path, pattern));
    }

    /**
     * 路径是否匹配
     *
     * @param path     路径
     * @param patterns 匹配模式列表
     * @return 是否匹配
     * @since 2.6.0
     */
    public static boolean isMatch(String path, String... patterns) {
        return Arrays.stream(patterns).anyMatch(pattern -> isMatch(path, pattern));
    }

    /**
     * 路径是否匹配
     *
     * @param path    路径
     * @param pattern 匹配模式
     * @return 是否匹配
     * @since 2.4.0
     */
    public static boolean isMatch(String path, String pattern) {
        PathPattern pathPattern = PathPatternParser.defaultInstance.parse(pattern);
        PathContainer pathContainer = PathContainer.parsePath(path);
        return pathPattern.matches(pathContainer);
    }

    /**
     * 路径是否匹配 - Ant 风格
     *
     * @param path    路径
     * @param pattern 匹配模式
     * @return 是否匹配
     * @author echo
     * @since 2.15.0
     */
    public static boolean isMatchAnt(String path, String pattern) {
        return MATCHER.match(pattern, path);
    }

    /**
     * 路径是否匹配 - Ant 风格
     *
     * @param path     路径
     * @param patterns 匹配模式列表
     * @return 是否匹配
     * @author echo
     * @since 2.15.0
     */
    public static boolean isMatchAnt(String path, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> isMatchAnt(path, pattern));
    }

    /**
     * 取消注册静态资源映射
     *
     * @param handlerMap 静态资源映射
     */
    public static void deRegisterResourceHandler(Map<String, String> handlerMap) {
        ApplicationContext applicationContext = SpringUtil.getApplicationContext();
        // 获取已经注册的映射
        final HandlerMapping resourceHandlerMapping = applicationContext
            .getBean("resourceHandlerMapping", HandlerMapping.class);
        final Map<String, Object> oldHandlerMap = (Map<String, Object>)ReflectUtil
            .getFieldValue(resourceHandlerMapping, "handlerMap");
        // 移除之前注册的映射
        for (Map.Entry<String, String> entry : handlerMap.entrySet()) {
            String pathPattern = CharSequenceUtil.appendIfMissing(entry.getKey(), StringConstants.PATH_PATTERN);
            oldHandlerMap.remove(pathPattern);
        }
    }

    /**
     * 注册静态资源映射
     *
     * @param handlerMap 静态资源映射
     */
    public static void registerResourceHandler(Map<String, String> handlerMap) {
        ApplicationContext applicationContext = SpringUtil.getApplicationContext();
        // 获取已经注册的映射
        final HandlerMapping resourceHandlerMapping = applicationContext
            .getBean("resourceHandlerMapping", HandlerMapping.class);
        final Map<String, Object> oldHandlerMap = (Map<String, Object>)ReflectUtil
            .getFieldValue(resourceHandlerMapping, "handlerMap");
        // 重新注册映射
        final ServletContext servletContext = applicationContext.getBean(ServletContext.class);
        final ContentNegotiationManager contentNegotiationManager = applicationContext
            .getBean("mvcContentNegotiationManager", ContentNegotiationManager.class);
        final UrlPathHelper urlPathHelper = applicationContext.getBean("mvcUrlPathHelper", UrlPathHelper.class);
        final ResourceHandlerRegistry resourceHandlerRegistry = new ResourceHandlerRegistry(applicationContext, servletContext, contentNegotiationManager, urlPathHelper);
        for (Map.Entry<String, String> entry : handlerMap.entrySet()) {
            // 移除之前注册的映射
            String pathPattern = CharSequenceUtil.appendIfMissing(CharSequenceUtil.removeSuffix(entry
                .getKey(), StringConstants.SLASH), StringConstants.PATH_PATTERN);
            oldHandlerMap.remove(pathPattern);
            // 重新注册映射
            String resourceLocations = CharSequenceUtil.appendIfMissing(entry.getValue(), StringConstants.SLASH);
            resourceHandlerRegistry.addResourceHandler(pathPattern).addResourceLocations("file:" + resourceLocations);
        }
        final Map<String, ?> additionalUrlMap = ReflectUtil
            .<SimpleUrlHandlerMapping>invoke(resourceHandlerRegistry, "getHandlerMapping")
            .getUrlMap();
        ReflectUtil.<Void>invoke(resourceHandlerMapping, "registerHandlers", additionalUrlMap);
    }

    /**
     * 获取处理器方法
     *
     * @param request 请求
     * @return 处理器方法
     * @since 2.14.0
     */
    public static HandlerMethod getHandlerMethod(HttpServletRequest request) {
        try {
            RequestMappingHandlerMapping handlerMapping = SpringUtil
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
            HandlerExecutionChain handlerExecutionChain = handlerMapping.getHandler(request);
            // 检查是否存在处理链
            if (handlerExecutionChain == null) {
                return null;
            }
            // 获取处理器
            Object handler = handlerExecutionChain.getHandler();
            if (handler instanceof HandlerMethod handlerMethod) {
                return handlerMethod;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
