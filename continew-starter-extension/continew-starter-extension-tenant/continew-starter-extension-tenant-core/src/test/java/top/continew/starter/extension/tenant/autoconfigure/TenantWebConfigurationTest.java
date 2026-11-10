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

package top.continew.starter.extension.tenant.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.extension.tenant.interceptor.TenantInterceptor;

/**
 * {@link TenantWebConfiguration} 拦截器装配测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantWebConfigurationTest {

    @Test
    @DisplayName("装配时注册租户拦截器并使用约定的租户拦截器顺序")
    void shouldRegisterTenantInterceptorWithConfiguredOrder() {
        TenantInterceptor interceptor = mock(TenantInterceptor.class);
        TenantWebConfiguration configuration = new TenantWebConfiguration(interceptor);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        when(registry.addInterceptor(interceptor)).thenReturn(registration);
        configuration.addInterceptors(registry);
        verify(registry).addInterceptor(interceptor);
        verify(registration).order(OrderedConstants.Interceptor.TENANT_INTERCEPTOR);
    }

    @Test
    @DisplayName("配置类实现 WebMvcConfigurer 且按 Servlet Web 条件装配")
    void shouldImplementWebMvcConfigurerUnderServletCondition() {
        assertThat(TenantWebConfiguration.class.getInterfaces()).contains(WebMvcConfigurer.class);
        assertThat(TenantWebConfiguration.class
            .getAnnotation(ConditionalOnWebApplication.class)).isNotNull();
    }
}
