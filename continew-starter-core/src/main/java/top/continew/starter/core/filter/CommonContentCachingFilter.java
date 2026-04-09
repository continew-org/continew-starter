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

package top.continew.starter.core.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import top.continew.starter.core.autoconfigure.contentcache.ContentCachingProperties;
import top.continew.starter.core.util.RepeatableContentCachingRequestWrapper;
import top.continew.starter.core.util.RepeatableContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 通用内容缓存过滤器
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class CommonContentCachingFilter extends OncePerRequestFilter {

    private final ContentCachingProperties properties;

    public CommonContentCachingFilter(ContentCachingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return properties.isMatchExcludeUri(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RepeatableContentCachingRequestWrapper requestWrapper = request instanceof RepeatableContentCachingRequestWrapper wrapped
            ? wrapped
            : new RepeatableContentCachingRequestWrapper(request, properties.getCacheLimit());
        RepeatableContentCachingResponseWrapper responseWrapper = response instanceof RepeatableContentCachingResponseWrapper wrapped
            ? wrapped
            : new RepeatableContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }
}
