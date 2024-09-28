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

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.extension.tenant.config.TenantDataSourceProvider;
import top.continew.starter.extension.tenant.handler.*;

/**
 * 多租户自动配置
 *
 * @author Charles7c
 * @since 2.7.0
 */
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.TENANT, name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantAutoConfiguration.class);

    private TenantAutoConfiguration() {
    }

    /**
     * 租户隔离级别：行级
     */
    @AutoConfiguration
    @ConditionalOnProperty(name = PropertiesConstants.TENANT + ".isolation-level", havingValue = "line", matchIfMissing = true)
    public static class Line {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'Tenant-Line' completed initialization.");
        }

        /**
         * 租户行级隔离拦截器
         */
        @Bean
        @ConditionalOnMissingBean
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantLineHandler tenantLineHandler) {
            return new TenantLineInnerInterceptor(tenantLineHandler);
        }

        /**
         * 租户行级隔离处理器（默认）
         */
        @Bean
        @ConditionalOnMissingBean
        public TenantLineHandler tenantLineHandler(TenantProperties properties) {
            return new DefaultTenantLineHandler(properties);
        }
    }

    /**
     * 租户隔离级别：数据源级
     */
    @AutoConfiguration
    @ConditionalOnProperty(name = PropertiesConstants.TENANT + ".isolation-level", havingValue = "datasource")
    public static class DataSource {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'Tenant-DataSource' completed initialization.");
        }

        /**
         * 租户数据源级隔离通知
         */
        @Bean
        @ConditionalOnMissingBean
        public TenantDataSourceAdvisor tenantDataSourceAdvisor(TenantDataSourceInterceptor tenantDataSourceInterceptor) {
            return new TenantDataSourceAdvisor(tenantDataSourceInterceptor);
        }

        /**
         * 租户数据源级隔离拦截器
         */
        @Bean
        @ConditionalOnMissingBean
        public TenantDataSourceInterceptor tenantDataSourceInterceptor(TenantDataSourceHandler tenantDataSourceHandler) {
            return new TenantDataSourceInterceptor(tenantDataSourceHandler);
        }

        /**
         * 租户数据源级隔离处理器（默认）
         */
        @Bean
        @ConditionalOnMissingBean
        public TenantDataSourceHandler tenantDataSourceHandler(TenantDataSourceProvider tenantDataSourceProvider, DynamicRoutingDataSource dynamicRoutingDataSource, DefaultDataSourceCreator dataSourceCreator) {
            return new DefaultTenantDataSourceHandler(tenantDataSourceProvider, dynamicRoutingDataSource, dataSourceCreator);
        }

        /**
         * 多租户数据源提供者
         */
        @Bean
        @ConditionalOnMissingBean
        public TenantDataSourceProvider tenantDataSourceProvider() {
            if (log.isErrorEnabled()) {
                log.error("Consider defining a bean of type '{}' in your configuration.", ResolvableType
                    .forClass(TenantDataSourceProvider.class));
            }
            throw new NoSuchBeanDefinitionException(TenantDataSourceProvider.class);
        }
    }
}
