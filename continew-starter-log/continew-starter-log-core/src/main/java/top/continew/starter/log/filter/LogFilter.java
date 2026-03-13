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

import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import top.continew.starter.core.wrapper.RepeatReadRequestWrapper;
import top.continew.starter.core.wrapper.RepeatReadResponseWrapper;
import top.continew.starter.log.model.LogProperties;

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

    private final LogProperties logProperties;

    public LogFilter(LogProperties logProperties) {
        this.logProperties = logProperties;
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
        RepeatReadRequestWrapper wrappedRequest = request instanceof RepeatReadRequestWrapper wrapped
            ? wrapped
            : new RepeatReadRequestWrapper(request);
        RepeatReadResponseWrapper wrappedResponse = response instanceof RepeatReadResponseWrapper wrapped
            ? wrapped
            : new RepeatReadResponseWrapper(response);
        filterChain.doFilter(wrappedRequest, wrappedResponse);

        // 复制缓存数据到原始响应
        wrappedResponse.copyBodyToResponse();
    }

    /**
     * 是否不过滤
     *
     * @param request 请求对象
     * @return true: 不过滤; false: 过滤
     */
    private boolean isNotFilter(HttpServletRequest request) {
        if (!isRequestValid(request)) {
            return true;
        }
        // 不拦截 /error
        ServerProperties serverProperties = SpringUtil.getBean(ServerProperties.class);
        if (request.getRequestURI().equals(serverProperties.getError().getPath())) {
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
