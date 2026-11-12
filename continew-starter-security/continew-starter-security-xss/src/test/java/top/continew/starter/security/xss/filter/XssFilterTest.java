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

package top.continew.starter.security.xss.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import top.continew.starter.security.xss.autoconfigure.XssProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * XSS 过滤器路径匹配分支测试
 *
 * <p>逐分支覆盖 {@link XssFilter#doFilter} 的四层条件：非 HTTP 请求与开关短路、放行路由
 * 优先于拦截路由、拦截路由命中与否、以及两者均未配置时的默认过滤。通配符用例验证
 * {@code SpringUtils.isMatch} 的 PathPattern 语义（{@code *} 单段、{@code **} 多段、
 * 精确路径整串比对）。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class XssFilterTest {

    private FilterChain filterChain;
    private ServletResponse response;

    @BeforeEach
    void setUp() {
        this.filterChain = mock(FilterChain.class);
        this.response = mock(ServletResponse.class);
    }

    /**
     * 非 HTTP 请求即使开关开启也应原样放行，不做任何包装。
     */
    @Test
    void shouldPassThroughWhenRequestIsNotHttp() throws Exception {
        ServletRequest nonHttpRequest = mock(ServletRequest.class);
        new XssFilter(new XssProperties()).doFilter(nonHttpRequest, this.response,
            this.filterChain);
        assertThat(this.capturedRequest()).isSameAs(nonHttpRequest);
    }

    /**
     * 开关关闭时即使配置了路由规则也应原样放行，规则不参与判断。
     */
    @Test
    void shouldPassThroughWhenDisabled() throws Exception {
        XssProperties properties = new XssProperties();
        properties.setEnabled(false);
        properties.setExcludePatterns(List.of("/api/**"));
        properties.setIncludePatterns(List.of("/api/**"));
        MockHttpServletRequest request = this.requestOf("/api/user");
        new XssFilter(properties).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isSameAs(request);
    }

    /**
     * 放行路由命中时应原样放行，且优先于拦截路由判断。
     */
    @Test
    void shouldExcludeMatchingPathEvenIfIncludeAlsoMatches() throws Exception {
        XssProperties properties = this.propertiesOf(List.of("/api/public/**"), List.of("/api/**"));
        MockHttpServletRequest request = this.requestOf("/api/public/info");
        new XssFilter(properties).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isSameAs(request);
    }

    /**
     * 放行路由未命中而拦截路由命中时应进入过滤包装。
     */
    @Test
    void shouldWrapWhenExcludeNotMatchedAndIncludeMatched() throws Exception {
        XssProperties properties = this.propertiesOf(List.of("/api/public/**"), List.of("/api/**"));
        MockHttpServletRequest request = this.requestOf("/api/private/info");
        new XssFilter(properties).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isInstanceOf(XssServletRequestWrapper.class);
    }

    /**
     * 拦截路由未命中时应原样放行，不做默认过滤。
     */
    @Test
    void shouldPassThroughWhenIncludeNotMatched() throws Exception {
        XssProperties properties = this.propertiesOf(List.of(), List.of("/api/**"));
        MockHttpServletRequest request = this.requestOf("/web/index");
        new XssFilter(properties).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isSameAs(request);
    }

    /**
     * 仅放行路由未命中且拦截路由未配置时应执行默认过滤。
     */
    @Test
    void shouldWrapByDefaultWhenIncludeNotConfigured() throws Exception {
        XssProperties properties = this.propertiesOf(List.of("/other/**"), List.of());
        MockHttpServletRequest request = this.requestOf("/web/index");
        new XssFilter(properties).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isInstanceOf(XssServletRequestWrapper.class);
    }

    /**
     * 放行与拦截路由均未配置时应执行默认过滤。
     */
    @Test
    void shouldWrapByDefaultWhenNoPatternsConfigured() throws Exception {
        MockHttpServletRequest request = this.requestOf("/web/index");
        new XssFilter(new XssProperties()).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isInstanceOf(XssServletRequestWrapper.class);
    }

    /**
     * 精确路径整串相等时命中放行。
     */
    @Test
    void shouldExcludeExactMatchedPath() throws Exception {
        XssProperties properties = this.propertiesOf(List.of("/api/health"), List.of());
        MockHttpServletRequest request = this.requestOf("/api/health");
        new XssFilter(properties).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isSameAs(request);
    }

    /**
     * 精确路径比对是整串语义，前缀相似但更长的路径不命中。
     */
    @Test
    void shouldNotExcludeSimilarButLongerPath() throws Exception {
        XssProperties properties = this.propertiesOf(List.of("/api/health"), List.of());
        MockHttpServletRequest request = this.requestOf("/api/healthX");
        new XssFilter(properties).doFilter(request, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isInstanceOf(XssServletRequestWrapper.class);
    }

    /**
     * 单段通配符 {@code *} 只匹配一层路径段。
     */
    @Test
    void shouldMatchSingleSegmentWildcard() throws Exception {
        XssProperties properties = this.propertiesOf(List.of(), List.of("/api/*"));
        MockHttpServletRequest matched = this.requestOf("/api/user");
        new XssFilter(properties).doFilter(matched, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isInstanceOf(XssServletRequestWrapper.class);
    }

    /**
     * 单段通配符不匹配更深的多段路径。
     */
    @Test
    void shouldNotMatchSingleSegmentWildcardForDeepPath() throws Exception {
        XssProperties properties = this.propertiesOf(List.of(), List.of("/api/*"));
        MockHttpServletRequest deep = this.requestOf("/api/user/detail");
        new XssFilter(properties).doFilter(deep, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isSameAs(deep);
    }

    /**
     * 多段通配符 {@code **} 匹配任意层级后缀路径。
     */
    @Test
    void shouldMatchMultiSegmentWildcard() throws Exception {
        XssProperties properties = this.propertiesOf(List.of(), List.of("/api/**"));
        MockHttpServletRequest matched = this.requestOf("/api/user/detail/list");
        new XssFilter(properties).doFilter(matched, this.response, this.filterChain);
        assertThat(this.capturedRequest()).isInstanceOf(XssServletRequestWrapper.class);
    }

    /**
     * 捕获过滤器放行给下游链路的请求对象。
     *
     * @return 下游链路收到的请求
     */
    private ServletRequest capturedRequest() throws IOException, ServletException {
        ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
        verify(this.filterChain).doFilter(captor.capture(), any(ServletResponse.class));
        return captor.getValue();
    }

    /**
     * 构造带路由规则的配置属性。
     *
     * @param excludePatterns 放行路由
     * @param includePatterns 拦截路由
     * @return 配置属性
     */
    private XssProperties propertiesOf(List<String> excludePatterns, List<String> includePatterns) {
        XssProperties properties = new XssProperties();
        properties.setExcludePatterns(excludePatterns);
        properties.setIncludePatterns(includePatterns);
        return properties;
    }

    /**
     * 构造指定访问路径的模拟请求。
     *
     * @param servletPath 访问路径
     * @return 模拟请求
     */
    private MockHttpServletRequest requestOf(String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(servletPath);
        return request;
    }
}
