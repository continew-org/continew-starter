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

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.extension.tenant.handler.TenantLineHandlerImpl;

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
         * 租户行级隔离处理器
         */
        @Bean
        @ConditionalOnMissingBean
        public TenantLineHandler tenantLineHandler(TenantProperties properties) {
            return new TenantLineHandlerImpl(properties);
        }
    }
}
