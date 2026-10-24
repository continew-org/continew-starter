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

package top.continew.starter.trace.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.trace.filter.TraceResponseFilter;

/**
 * 链路追踪自动配置测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class TraceAutoConfigurationTest {

    private final TraceAutoConfiguration autoConfiguration =
        new TraceAutoConfiguration(new TraceProperties());

    /**
     * Tracer 为 NOOP 时启动即失败
     */
    @Test
    @DisplayName("Tracer 为 NOOP 时启动即失败")
    void shouldFailFastWhenTracerIsNoop() {
        ObjectProvider<Tracer> tracerProvider = mockProvider(Tracer.NOOP);

        assertThatThrownBy(() -> autoConfiguration.traceResponseFilter(tracerProvider))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("micrometer-tracing-bridge-brave");
    }

    /**
     * Tracer 不可用时启动即失败
     */
    @Test
    @DisplayName("Tracer 不可用时启动即失败")
    void shouldFailFastWhenTracerMissing() {
        ObjectProvider<Tracer> tracerProvider = mockProvider(null);

        assertThatThrownBy(() -> autoConfiguration.traceResponseFilter(tracerProvider))
            .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Tracer 可用时注册过滤器
     */
    @Test
    @DisplayName("Tracer 可用时注册链路追踪过滤器")
    void shouldRegisterFilter() {
        FilterRegistrationBean<TraceResponseFilter> registrationBean = autoConfiguration
            .traceResponseFilter(mockProvider(mock(Tracer.class)));

        assertThat(registrationBean.getFilter()).isInstanceOf(TraceResponseFilter.class);
        assertThat(registrationBean.getOrder()).isEqualTo(OrderedConstants.Filter.TRACE_FILTER);
        assertThat(registrationBean.getUrlPatterns()).containsExactly("/*");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<Tracer> mockProvider(Tracer tracer) {
        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        return tracerProvider;
    }
}
