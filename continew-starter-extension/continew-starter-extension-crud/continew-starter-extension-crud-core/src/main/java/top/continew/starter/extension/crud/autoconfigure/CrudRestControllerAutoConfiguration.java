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

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.continew.starter.extension.crud.annotation.CrudApi;
import top.continew.starter.extension.crud.aop.CrudApiAnnotationAdvisor;
import top.continew.starter.extension.crud.aop.CrudApiAnnotationInterceptor;
import top.continew.starter.extension.crud.handler.CrudApiHandler;
import top.continew.starter.extension.crud.handler.DefaultCrudApiHandler;

/**
 * CRUD REST Controller 自动配置
 *
 * @author Charles7c
 * @since 2.7.5
 */
@AutoConfiguration
@EnableConfigurationProperties(CrudProperties.class)
public class CrudRestControllerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CrudRestControllerAutoConfiguration.class);

    /**
     * CRUD API 注解通知
     */
    @Bean
    @ConditionalOnMissingBean
    public CrudApiAnnotationAdvisor crudApiAnnotationAdvisor(CrudApiAnnotationInterceptor crudApiAnnotationInterceptor) {
        return new CrudApiAnnotationAdvisor(crudApiAnnotationInterceptor, CrudApi.class);
    }

    /**
     * CRUD API 注解拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public CrudApiAnnotationInterceptor crudApiAnnotationInterceptor(CrudApiHandler crudApiHandler) {
        return new CrudApiAnnotationInterceptor(crudApiHandler);
    }

    /**
     * CRUD API 处理器（默认）
     */
    @Bean
    @ConditionalOnMissingBean
    public CrudApiHandler crudApiHandler() {
        return new DefaultCrudApiHandler();
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Extension-CRUD REST Controller' completed initialization.");
    }
}
