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

package top.continew.starter.extension.tenant.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import top.continew.starter.extension.tenant.annotation.TenantIgnore;
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContext;
import top.continew.starter.extension.tenant.context.TenantContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Stream;

/**
 * {@link TenantInterceptor} 租户拦截器测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantInterceptorTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("preHandle 从请求头读取租户 ID 并装配上下文，恒放行")
    void shouldBuildContextFromHeaderAndPassThrough() {
        TenantProvider provider = mock(TenantProvider.class);
        TenantContext context = new TenantContext();
        context.setTenantId(123L);
        when(provider.getByTenantId("123", true)).thenReturn(context);
        TenantInterceptor interceptor = new TenantInterceptor(new TenantProperties(), provider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "123");
        assertThat(interceptor
            .preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(provider).getByTenantId("123", true);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("请求头缺失时以 null 租户 ID 查询提供者（锁定当前行为）")
    void shouldQueryProviderWithNullTenantIdWhenHeaderMissing() {
        TenantProvider provider = mock(TenantProvider.class);
        when(provider.getByTenantId(null, true)).thenReturn(new TenantContext());
        TenantInterceptor interceptor = new TenantInterceptor(new TenantProperties(), provider);
        assertThat(interceptor
            .preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
            .isTrue();
        verify(provider).getByTenantId(null, true);
    }

    @Test
    @DisplayName("租户 ID 请求头键名取自配置属性")
    void shouldReadHeaderNameFromProperties() {
        TenantProperties properties = new TenantProperties();
        properties.setTenantIdHeader("X-Tid");
        TenantProvider provider = mock(TenantProvider.class);
        when(provider.getByTenantId("456", true)).thenReturn(new TenantContext());
        TenantInterceptor interceptor = new TenantInterceptor(properties, provider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tid", "456");
        assertThat(interceptor
            .preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(provider).getByTenantId("456", true);
    }

    @ParameterizedTest(name = "[{index}] {0} 时忽略标记为 {2}")
    @MethodSource("ignoreScenarios")
    void shouldMarkIgnoreByHandlerAnnotation(String scenario,
        Object handler,
        boolean expectedIgnore) {
        TenantProvider provider = mock(TenantProvider.class);
        when(provider.getByTenantId(any(), anyBoolean())).thenReturn(new TenantContext());
        TenantInterceptor interceptor = new TenantInterceptor(new TenantProperties(), provider);
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        assertThat(TenantContextHolder.isIgnore()).isEqualTo(expectedIgnore);
    }

    @Test
    @DisplayName("afterCompletion 清空上下文与忽略标记")
    void shouldClearContextAfterCompletion() {
        TenantContextHolder.setContext(new TenantContext());
        TenantContextHolder.setIgnore(true);
        TenantInterceptor interceptor = new TenantInterceptor(new TenantProperties(), mock(
            TenantProvider.class));
        interceptor.afterCompletion(new MockHttpServletRequest(), new MockHttpServletResponse(),
            new Object(), null);
        assertThat(TenantContextHolder.getContext()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    /**
     * 忽略标记判定场景：方法级注解、类级注解、无注解方法与非 HandlerMethod 处理器。
     *
     * @return 场景名、处理器实例、期望忽略标记
     */
    private static Stream<Arguments> ignoreScenarios() throws Exception {
        return Stream.of(arguments("方法级 @TenantIgnore", new HandlerMethod(new MethodIgnoreHandler(),
            MethodIgnoreHandler.class.getMethod("annotated")), true), arguments("类级 @TenantIgnore",
                new HandlerMethod(new TypeIgnoreHandler(), TypeIgnoreHandler.class
                    .getMethod("action")),
                true),
            arguments("无注解方法", new HandlerMethod(
                new MethodIgnoreHandler(), MethodIgnoreHandler.class
                    .getMethod("plain")),
                false),
            arguments("非 HandlerMethod", new Object(),
                false));
    }

    /**
     * 方法级忽略样例处理器
     */
    static class MethodIgnoreHandler {

        /**
         * 带忽略注解的方法
         */
        @TenantIgnore
        public void annotated() {
            // 方法体不会执行，仅作为方法级注解的反射取样锚点
        }

        /**
         * 无注解方法
         */
        public void plain() {
            // 方法体不会执行，作为无注解场景的对照取样
        }
    }

    /**
     * 类级忽略样例处理器
     */
    @TenantIgnore
    static class TypeIgnoreHandler {

        /**
         * 样例方法
         */
        public void action() {
            // 方法体不会执行，仅用于类级注解载体的反射取样
        }
    }
}
