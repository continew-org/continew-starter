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

package top.charles7c.continew.starter.core.util;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.ServletContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import top.charles7c.continew.starter.core.constant.StringConstants;

import java.util.Map;

/**
 * Spring 工具类
 *
 * @author Charles7c
 * @since 1.1.1
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpringUtils {

    /**
     * 取消注册静态资源映射
     *
     * @param handlerMap 静态资源映射
     */
    public static void deRegisterResourceHandler(Map<String, String> handlerMap) {
        ApplicationContext applicationContext = SpringUtil.getApplicationContext();
        // 获取已经注册的映射
        final HandlerMapping resourceHandlerMapping = applicationContext.getBean("resourceHandlerMapping", HandlerMapping.class);
        final Map<String, Object> oldHandlerMap = (Map<String, Object>) ReflectUtil.getFieldValue(resourceHandlerMapping, "handlerMap");
        // 移除之前注册的映射
        for (Map.Entry<String, String> entry : handlerMap.entrySet()) {
            String pathPattern = StrUtil.appendIfMissing(entry.getKey(), StringConstants.PATH_PATTERN);
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
        final HandlerMapping resourceHandlerMapping = applicationContext.getBean("resourceHandlerMapping", HandlerMapping.class);
        final Map<String, Object> oldHandlerMap = (Map<String, Object>) ReflectUtil.getFieldValue(resourceHandlerMapping, "handlerMap");
        // 重新注册映射
        final ServletContext servletContext = applicationContext.getBean(ServletContext.class);
        final ContentNegotiationManager contentNegotiationManager = applicationContext.getBean("mvcContentNegotiationManager", ContentNegotiationManager.class);
        final UrlPathHelper urlPathHelper = applicationContext.getBean("mvcUrlPathHelper", UrlPathHelper.class);
        final ResourceHandlerRegistry resourceHandlerRegistry = new ResourceHandlerRegistry(applicationContext, servletContext, contentNegotiationManager, urlPathHelper);
        for (Map.Entry<String, String> entry : handlerMap.entrySet()) {
            // 移除之前注册的映射
            String pathPattern = StrUtil.appendIfMissing(entry.getKey(), StringConstants.PATH_PATTERN);
            oldHandlerMap.remove(pathPattern);
            // 重新注册映射
            String resourceLocations = StrUtil.appendIfMissing(entry.getValue(), StringConstants.SLASH);
            resourceHandlerRegistry.addResourceHandler(pathPattern).addResourceLocations("file:" + resourceLocations);
        }
        final Map<String, ?> additionalUrlMap = ReflectUtil.<SimpleUrlHandlerMapping>invoke(resourceHandlerRegistry, "getHandlerMapping").getUrlMap();
        ReflectUtil.<Void>invoke(resourceHandlerMapping, "registerHandlers", additionalUrlMap);
    }
}
