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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.log.model.LogProperties;

import jakarta.servlet.FilterChain;

/**
 * 日志过滤器测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class LogFilterTest {

    /**
     * 请求路径等于配置的错误路径时不过滤
     */
    @Test
    @DisplayName("请求路径等于配置的错误路径时不过滤")
    void shouldNotFilterConfiguredErrorPath() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.web.error.path", "/custom-error");
        LogFilter filter = new LogFilter(new LogProperties(), environment);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/custom-error");

        assertThat(filter.isNotFilter(request)).isTrue();
    }

    /**
     * 未配置错误路径时按默认值 /error 放行
     */
    @Test
    @DisplayName("未配置错误路径时默认放行 /error")
    void shouldNotFilterDefaultErrorPath() {
        LogFilter filter = new LogFilter(new LogProperties(), new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");

        assertThat(filter.isNotFilter(request)).isTrue();
    }

    /**
     * 普通请求仍然过滤
     */
    @Test
    @DisplayName("普通请求仍然过滤")
    void shouldFilterNormalRequest() {
        LogFilter filter = new LogFilter(new LogProperties(), new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");

        assertThat(filter.isNotFilter(request)).isFalse();
    }

    /**
     * 错误路径请求不包装请求与响应，直接放行
     */
    @Test
    @DisplayName("错误路径请求不包装请求与响应")
    void shouldPassThroughErrorPathWithoutWrapping() throws Exception {
        LogFilter filter = new LogFilter(new LogProperties(), new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
