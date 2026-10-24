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

package top.continew.starter.trace.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * 链路追踪工具类测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class TraceUtilsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * 从 MDC 获取链路 ID
     */
    @Test
    @DisplayName("从 MDC 获取链路 ID 与 Span ID")
    void shouldGetTraceIdFromMdc() {
        MDC.put(TraceUtils.MDC_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put(TraceUtils.MDC_SPAN_ID, "00f067aa0ba902b7");

        assertThat(TraceUtils.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(TraceUtils.getSpanId()).isEqualTo("00f067aa0ba902b7");
    }

    /**
     * 从 Tracer 获取链路 ID
     */
    @Test
    @DisplayName("从 Tracer 获取链路 ID")
    void shouldGetTraceIdFromTracer() {
        TraceContext traceContext = mock(TraceContext.class);
        when(traceContext.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");
        Span span = mock(Span.class);
        when(span.context()).thenReturn(traceContext);
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);

        assertThat(TraceUtils.getTraceId(tracer)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    /**
     * 无 Span 时返回 null
     */
    @Test
    @DisplayName("无 Span 时返回 null")
    void shouldReturnNullWithoutSpan() {
        Tracer tracer = mock(Tracer.class);

        assertThat(TraceUtils.getTraceId(tracer)).isNull();
        assertThat(TraceUtils.getTraceId()).isNull();
    }
}
