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

package top.continew.starter.extension.datapermission.autoconfigure;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
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
import top.continew.starter.extension.datapermission.filter.DataPermissionUserContextProvider;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;

/**
 * 数据权限自动配置
 *
 * @author Charles7c
 * @since 2.7.0
 */
@AutoConfiguration
@EnableConfigurationProperties(DataPermissionProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.DATA_PERMISSION, name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class DataPermissionAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataPermissionAutoConfiguration.class);

    private DataPermissionAutoConfiguration() {
    }

    /**
     * 数据权限拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public DataPermissionInterceptor dataPermissionInterceptor(DataPermissionHandler dataPermissionHandler) {
        return new DataPermissionInterceptor(dataPermissionHandler);
    }

    /**
     * 数据权限处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DataPermissionHandler dataPermissionHandler(DataPermissionUserContextProvider dataPermissionUserContextProvider) {
        return new DefaultDataPermissionHandler(dataPermissionUserContextProvider);
    }

    /**
     * 数据权限用户上下文提供者
     */
    @Bean
    @ConditionalOnMissingBean
    public DataPermissionUserContextProvider dataPermissionUserContextProvider() {
        if (log.isErrorEnabled()) {
            log.error("Consider defining a bean of type '{}' in your configuration.", ResolvableType
                .forClass(DataPermissionUserContextProvider.class));
        }
        throw new NoSuchBeanDefinitionException(DataPermissionUserContextProvider.class);
    }

    static {
        log.debug("[ContiNew Starter] - Auto Configuration 'DataPermission' completed initialization.");
    }
}
