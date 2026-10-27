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

package top.continew.starter.log.filter;

import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import top.continew.starter.core.wrapper.RepeatReadRequestWrapper;
import top.continew.starter.core.wrapper.RepeatReadResponseWrapper;
import top.continew.starter.log.model.LogProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 日志过滤器
 *
 * @author Charles7c
 * @author echo
 * @since 1.1.0
 */
public class LogFilter extends OncePerRequestFilter {

    private static final String ERROR_PATH_PROPERTY = "spring.web.error.path";

    private static final String DEFAULT_ERROR_PATH = "/error";

    private final LogProperties logProperties;

    private final String errorPath;

    public LogFilter(LogProperties logProperties, Environment environment) {
        this.logProperties = logProperties;
        // Spring Boot 4 不再将 ErrorProperties 注册为 bean（已改为 WebProperties 的嵌套属性），
        // 只能在启动时按配置项解析错误路径，避免请求期因缺少 bean 抛 NoSuchBeanDefinitionException
        this.errorPath = environment.getProperty(ERROR_PATH_PROPERTY, DEFAULT_ERROR_PATH);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (this.isNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 包装可重复读取请求及响应
        RepeatReadRequestWrapper wrappedRequest =
            request instanceof RepeatReadRequestWrapper wrapped
                ? wrapped
                : new RepeatReadRequestWrapper(request);
        RepeatReadResponseWrapper wrappedResponse =
            response instanceof RepeatReadResponseWrapper wrapped
                ? wrapped
                : new RepeatReadResponseWrapper(response);
        filterChain.doFilter(wrappedRequest, wrappedResponse);

        // 响应内容已在写入时同步推送到原始响应流，这里仅刷新缓冲确保数据（尤其 PrintWriter 缓冲）全部写出
        wrappedResponse.flushBuffer();
    }

    /**
     * 是否不过滤
     *
     * @param request 请求对象
     * @return true: 不过滤; false: 过滤
     */
    boolean isNotFilter(HttpServletRequest request) {
        if (!isRequestValid(request)) {
            return true;
        }
        // 不拦截错误页
        if (request.getRequestURI().equals(errorPath)) {
            return true;
        }
        // 放行路径
        return logProperties.isMatchExcludeUri(request.getRequestURI());
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
}
