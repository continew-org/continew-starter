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

package top.continew.starter.trace.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.trace.autoconfigure.TraceProperties;

import jakarta.servlet.FilterChain;

/**
 * 链路追踪响应过滤器测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class TraceResponseFilterTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final FilterChain chain = (servletRequest, servletResponse) -> {
    };

    /**
     * 存在当前 Span 时写入响应头
     */
    @Test
    @DisplayName("存在当前 Span 时写入链路 ID 响应头")
    void shouldWriteTraceIdHeader() throws Exception {
        Tracer tracer = mockTracer("4bf92f3577b34da6a3ce929d0e0e4736");
        TraceProperties traceProperties = new TraceProperties();
        traceProperties.setTraceIdName("X-Trace-Id");

        new TraceResponseFilter(tracer, traceProperties).doFilter(request, response, chain);

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    /**
     * 默认响应头名称为 traceId
     */
    @Test
    @DisplayName("默认响应头名称为 traceId")
    void shouldWriteDefaultHeaderName() throws Exception {
        Tracer tracer = mockTracer("4bf92f3577b34da6a3ce929d0e0e4736");
        TraceProperties traceProperties = new TraceProperties();

        new TraceResponseFilter(tracer, traceProperties).doFilter(request, response, chain);

        assertThat(response.getHeader("traceId")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    /**
     * 无当前 Span 时不写入响应头
     */
    @Test
    @DisplayName("无当前 Span 时不写入响应头")
    void shouldNotWriteHeaderWithoutSpan() throws Exception {
        Tracer tracer = mock(Tracer.class);
        TraceProperties traceProperties = new TraceProperties();
        traceProperties.setTraceIdName("X-Trace-Id");

        new TraceResponseFilter(tracer, traceProperties).doFilter(request, response, chain);

        assertThat(response.getHeader("X-Trace-Id")).isNull();
    }

    /**
     * 链路 ID 名称留空时不写入响应头
     */
    @Test
    @DisplayName("链路 ID 名称留空时不写入响应头")
    void shouldNotWriteHeaderWhenNameBlank() throws Exception {
        Tracer tracer = mockTracer("4bf92f3577b34da6a3ce929d0e0e4736");
        TraceProperties traceProperties = new TraceProperties();
        traceProperties.setTraceIdName(null);

        new TraceResponseFilter(tracer, traceProperties).doFilter(request, response, chain);

        assertThat(response.getHeaderNames()).isEmpty();
    }

    private static Tracer mockTracer(String traceId) {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(traceContext.traceId()).thenReturn(traceId);
        when(span.context()).thenReturn(traceContext);
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);
        return tracer;
    }
}
