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

package top.continew.starter.extension.crud.autoconfigure;

import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * CRUD Request Mapping 自动配置
 * <p>
 * 通过 {@link WebMvcRegistrations} 替换默认的 RequestMappingHandlerMapping，而非继承 WebMvcConfigurationSupport：
 * 后者会让 Boot 的 WebMvcAutoConfiguration 因 @ConditionalOnMissingBean(WebMvcConfigurationSupport) 整体失效，
 * 导致 mvcApiVersionStrategy 等 MVC 基础设施缺失，消费方启动失败
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Configuration
public class CrudRequestMappingAutoConfiguration {

    /**
     * CRUD 请求映射器注册（覆盖 Boot 默认 RequestMappingHandlerMapping）
     * <p>
     * Boot 通过 ObjectProvider#getIfUnique 获取该类型，标注 @Primary 以保证同时存在自定义注册时仍取本实现
     */
    @Bean
    @Primary
    public WebMvcRegistrations crudWebMvcRegistrations() {
        return new CrudWebMvcRegistrations();
    }

    static class CrudWebMvcRegistrations implements WebMvcRegistrations {

        @Override
        public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
            return new CrudRequestMappingHandlerMapping();
        }
    }
}
