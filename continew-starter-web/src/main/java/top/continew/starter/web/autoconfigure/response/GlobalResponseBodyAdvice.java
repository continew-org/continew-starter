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

package top.continew.starter.web.autoconfigure.response;

import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import top.continew.starter.web.annotation.ExcludeFromGlobalResponse;
import top.continew.starter.web.model.R;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * 全局响应体处理
 * <p>
 * 对 JSON 响应统一包装为 {@link R}，并支持国际化、排除注解、例外包路径、例外 URL 与例外返回类型。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
@ControllerAdvice
@Order(1000)
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GlobalResponseProperties properties;

    private final GlobalResponseFactory responseFactory;

    private final MessageSource messageSource;

    /**
     * 构造方法
     *
     * @param properties      全局响应配置属性
     * @param responseFactory 全局响应工厂
     * @param messageSource   国际化消息源
     */
    public GlobalResponseBodyAdvice(GlobalResponseProperties properties,
        GlobalResponseFactory responseFactory,
        @Nullable MessageSource messageSource) {
        this.properties = properties;
        this.responseFactory = responseFactory;
        this.messageSource = messageSource;
    }

    @Override
    public boolean supports(MethodParameter returnType,
        Class<? extends HttpMessageConverter<?>> converterType) {
        Method method = returnType.getMethod();
        if (method == null || !JsonHttpMessageConverterDetector
            .isJsonConverter(converterType, properties.getJsonHttpMessageConverter())) {
            return false;
        }
        // 国际化场景需要接管控制器自行返回的 R，因此不做包装条件判断
        return properties.isI18n() || shouldWrap(method);
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response) {
        if (responseFactory.isResponse(body)) {
            renderMessage(body);
            return body;
        }
        Method method = returnType.getMethod();
        if (method == null || !shouldWrap(method)) {
            return body;
        }
        Object result = responseFactory.newSuccess(body);
        renderMessage(result);
        return result;
    }

    /**
     * 是否需要包装响应
     *
     * @param method 控制器方法
     * @return 是否需要包装响应
     */
    private boolean shouldWrap(Method method) {
        if (isExcluded(method)) {
            return false;
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            return true;
        }
        if (R.class.isAssignableFrom(returnType) || HttpEntity.class.isAssignableFrom(returnType)) {
            return false;
        }
        return !isExcludedReturnType(returnType) && !isExcludedPackage(method) && !isExcludedUrl();
    }

    private boolean isExcluded(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, ExcludeFromGlobalResponse.class)
            || AnnotatedElementUtils.hasAnnotation(method
                .getDeclaringClass(), ExcludeFromGlobalResponse.class)
            || AnnotatedElementUtils.hasAnnotation(method
                .getReturnType(), ExcludeFromGlobalResponse.class);
    }

    private boolean isExcludedReturnType(Class<?> returnType) {
        Set<Class<?>> excludeReturnTypes = properties.getExcludeReturnTypes();
        return !CollectionUtils.isEmpty(excludeReturnTypes) && excludeReturnTypes
            .contains(returnType);
    }

    private boolean isExcludedPackage(Method method) {
        List<String> excludePackages = properties.getExcludePackages();
        if (CollectionUtils.isEmpty(excludePackages)) {
            return false;
        }
        String packageName = method.getDeclaringClass().getPackageName();
        return excludePackages.stream().anyMatch(item -> PATH_MATCHER.match(item, packageName));
    }

    private boolean isExcludedUrl() {
        List<String> excludeUrls = properties.getExcludeUrls();
        if (CollectionUtils.isEmpty(excludeUrls)) {
            return false;
        }
        String requestUri = currentRequestUri();
        return requestUri != null && excludeUrls.stream()
            .anyMatch(item -> PATH_MATCHER.match(item, requestUri));
    }

    @Nullable
    private String currentRequestUri() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest().getRequestURI();
        }
        return null;
    }

    /**
     * 按响应码渲染国际化信息
     *
     * @param body 响应对象
     */
    private void renderMessage(Object body) {
        if (!properties.isI18n() || messageSource == null) {
            return;
        }
        String code = responseFactory.getCode(body);
        if (!StringUtils.hasText(code)) {
            return;
        }
        String message = messageSource.getMessage(code, null, null, LocaleContextHolder
            .getLocale());
        if (StringUtils.hasText(message)) {
            responseFactory.setMessage(body, message);
        }
    }
}
