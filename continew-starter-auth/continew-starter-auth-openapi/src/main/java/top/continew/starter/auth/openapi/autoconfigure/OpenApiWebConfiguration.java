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

package top.continew.starter.auth.openapi.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.continew.starter.auth.openapi.dao.OpenApiAppDao;
import top.continew.starter.auth.openapi.interceptor.OpenApiInterceptor;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.core.constant.StringConstants;

/**
 * 开放 API Web 层配置（拦截器等）
 *
 * @author Charles7c
 * @since 2.16.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OpenApiWebConfiguration implements WebMvcConfigurer {

    private final OpenApiInterceptor openApiInterceptor;

    protected OpenApiWebConfiguration(OpenApiInterceptor openApiInterceptor) {
        this.openApiInterceptor = openApiInterceptor;
    }

    /**
     * 开放 API 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenApiInterceptor openApiInterceptor(OpenApiProperties properties, OpenApiAppDao openApiAppDao) {
        return new OpenApiInterceptor(properties, openApiAppDao);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.openApiInterceptor)
            .addPathPatterns(StringConstants.PATH_PATTERN)
            .order(OrderedConstants.Interceptor.AUTH_INTERCEPTOR);
    }
}
