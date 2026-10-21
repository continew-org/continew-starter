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

package top.continew.starter.web.autoconfigure.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 禁止不安全的 HTTP 方法过滤器
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class DisallowedMethodsFilter extends OncePerRequestFilter {

    private final Set<String> disallowedMethods;

    /**
     * 构造方法
     *
     * @param disallowedMethods 不允许的请求方式
     */
    public DisallowedMethodsFilter(List<String> disallowedMethods) {
        this.disallowedMethods = disallowedMethods.stream()
            .map(method -> method.toUpperCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        if (disallowedMethods.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
