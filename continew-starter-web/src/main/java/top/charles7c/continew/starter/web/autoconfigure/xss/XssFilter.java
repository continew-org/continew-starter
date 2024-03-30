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

package top.charles7c.continew.starter.web.autoconfigure.xss;

import cn.hutool.core.collection.CollectionUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.List;

/**
 * xss过滤器
 *
 * @author whhya
 * @since 1.0.0
 */
public class XssFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(XssFilter.class);

    private final XssProperties xssProperties;

    public XssFilter(XssProperties xssProperties) {
        this.xssProperties = xssProperties;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.debug("[ContiNew Starter] - Auto Configuration 'Web-XssFilter' completed initialization.");
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        //未开启xss过滤，则直接跳过
        if (!xssProperties.isEnabled()) {
            filterChain.doFilter(req, servletResponse);
            return;
        }

        //限定url地址
        List<String> pathPatterns = xssProperties.getPathPatterns();

        //判断是否匹配需要忽略地址
        List<String> pathExcludePatterns = xssProperties.getPathExcludePatterns();
        if (CollectionUtil.isNotEmpty(pathPatterns)) {
            if (isMatchPath(req.getServletPath(), pathExcludePatterns)) {
                filterChain.doFilter(req, servletResponse);
                return;
            }
        }

        //如果存在则限定path拦截
        if (CollectionUtil.isNotEmpty(pathPatterns)) {
            //未匹配上限定地址，则直接不过滤
            if (isMatchPath(req.getServletPath(), pathPatterns)) {
                filterChain.doFilter(new XssServletRequestWrapper(req), servletResponse);
                return;
            } else {
                filterChain.doFilter(req, servletResponse);
                return;
            }
        }

        //默认拦截
        filterChain.doFilter(new XssServletRequestWrapper((HttpServletRequest) servletRequest), servletResponse);
    }

    /**
     * 判断数组中是否存在匹配的路径
     *
     * @param requestURL   请求地址
     * @param pathPatterns 指定匹配路径
     * @return true 匹配 false 不匹配
     */
    private static boolean isMatchPath(String requestURL, List<String> pathPatterns) {
        for (String pattern : pathPatterns) {
            PathPattern pathPattern = PathPatternParser.defaultInstance.parse(pattern);
            PathContainer pathContainer = PathContainer.parsePath(requestURL);
            boolean matches = pathPattern.matches(pathContainer);
            if (matches) {
                return true;
            }
        }
        return false;
    }

}
