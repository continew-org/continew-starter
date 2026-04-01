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

package top.continew.starter.auth.openapi.autoconfigure.dao;

import jakarta.annotation.PostConstruct;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import top.continew.starter.auth.openapi.autoconfigure.OpenApiProperties;
import top.continew.starter.auth.openapi.dao.DefaultOpenApiAppDao;
import top.continew.starter.auth.openapi.dao.OpenApiAppDao;
import top.continew.starter.auth.openapi.dao.RedisOpenApiAppDao;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * 开放 API 应用 DAO 配置
 *
 * @author Charles7c
 * @since 2.16.0
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiAppDaoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OpenApiAppDaoConfiguration.class);

    /**
     * 使用内存
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(OpenApiAppDao.class)
    @ConditionalOnProperty(prefix = PropertiesConstants.AUTH_OPENAPI, name = "app-dao.type", havingValue = "default", matchIfMissing = true)
    static class Default {

        @Bean
        public OpenApiAppDao openApiAppDao() {
            log.debug("[ContiNew Starter] - Auto Configuration 'OpenApi-AppDao-Default' completed initialization.");
            return new DefaultOpenApiAppDao();
        }
    }

    /**
     * 使用 Redis
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(OpenApiAppDao.class)
    @ConditionalOnProperty(prefix = PropertiesConstants.AUTH_OPENAPI, name = "app-dao.type", havingValue = "redis")
    static class Redis {

        @Bean
        public OpenApiAppDao openApiAppDao(OpenApiProperties properties) {
            log.debug("[ContiNew Starter] - Auto Configuration 'OpenApi-AppDao-Redis' completed initialization.");
            return new RedisOpenApiAppDao(properties.getAppDao());
        }
    }

    /**
     * 自定义
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = PropertiesConstants.AUTH_OPENAPI, name = "app-dao.type", havingValue = "custom")
    static class Custom {

        @Bean
        @ConditionalOnMissingBean
        public OpenApiAppDao openApiAppDao() {
            if (log.isErrorEnabled()) {
                log.error("[ContiNew Starter] - When 'continew-starter.openapi.app-dao.type' is 'custom', you must provide a bean of type '{}' in your configuration.", ResolvableType
                    .forClass(OpenApiAppDao.class));
            }
            throw new NoSuchBeanDefinitionException(OpenApiAppDao.class);
        }

        @PostConstruct
        public void postConstruct() {
            log.debug("[ContiNew Starter] - Auto Configuration 'OpenApi-AppDao-Custom' completed initialization.");
        }
    }
}
