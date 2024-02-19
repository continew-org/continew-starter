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

package top.charles7c.continew.starter.log.httptracepro.handler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import top.charles7c.continew.starter.log.core.enums.Include;
import top.charles7c.continew.starter.log.httptracepro.autoconfigure.LogProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;

/**
 * 日志过滤器
 *
 * @author Dave Syer（Spring Boot Actuator）
 * @author Wallace Wadge（Spring Boot Actuator）
 * @author Andy Wilkinson（Spring Boot Actuator）
 * @author Venil Noronha（Spring Boot Actuator）
 * @author Madhura Bhave（Spring Boot Actuator）
 * @author Charles7c
 * @since 1.1.0
 */
public class LogFilter extends OncePerRequestFilter implements Ordered {

    private final LogProperties logProperties;

    public LogFilter(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!isRequestValid(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 包装输入流，可重复读取
        if (isRequestWrapper(request)) {
            request = new ContentCachingRequestWrapper(request);
        }
        // 包装输出流，可重复读取
        boolean isResponseWrapper = isResponseWrapper(response);
        if (isResponseWrapper) {
            response = new ContentCachingResponseWrapper(response);
        }
        filterChain.doFilter(request, response);
        // 更新响应（不操作这一步，会导致接口响应空白）
        if (isResponseWrapper) {
            updateResponse(response);
        }
    }

    /**
     * 请求是否有效
     *
     * @param request 请求对象
     * @return true：是；false：否
     */
    private boolean isRequestValid(HttpServletRequest request) {
        try {
            new URI(request.getRequestURL().toString());
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * 是否需要包装输入流
     *
     * @param request 请求对象
     * @return true：是；false：否
     */
    private boolean isRequestWrapper(HttpServletRequest request) {
        Set<Include> includeSet = logProperties.getIncludes();
        return !(request instanceof ContentCachingRequestWrapper) && (includeSet
            .contains(Include.REQUEST_BODY) || includeSet.contains(Include.REQUEST_PARAM));
    }

    /**
     * 是否需要包装输出流
     *
     * @param response 响应对象
     * @return true：是；false：否
     */
    private boolean isResponseWrapper(HttpServletResponse response) {
        Set<Include> includeSet = logProperties.getIncludes();
        return !(response instanceof ContentCachingResponseWrapper) && (includeSet
            .contains(Include.RESPONSE_BODY) || includeSet.contains(Include.RESPONSE_PARAM));
    }

    /**
     * 更新响应
     *
     * @param response 响应对象
     * @throws IOException /
     */
    private void updateResponse(HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper = WebUtils
            .getNativeResponse(response, ContentCachingResponseWrapper.class);
        Objects.requireNonNull(responseWrapper).copyBodyToResponse();
    }
}
