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

package top.continew.starter.web.autoconfigure.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

/**
 * 服务器扩展自动配置
 *
 * @author Jasmine
 * @author Charles7c
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ServerExtensionProperties.class)
@PropertySource(value = "classpath:default-server.yml",
    factory = GeneralPropertySourceFactory.class)
@ConditionalOnProperty(prefix = "server.extension", name = PropertiesConstants.ENABLED,
    havingValue = "true")
public class ServerExtensionAutoConfiguration {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ServerExtensionAutoConfiguration.class);

    /**
     * 禁止不安全的 HTTP 方法
     *
     * @param properties 服务器扩展配置
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<DisallowedMethodsFilter> disallowedMethodsFilter(
        ServerExtensionProperties properties) {
        FilterRegistrationBean<DisallowedMethodsFilter> registrationBean =
            new FilterRegistrationBean<>();
        registrationBean.setFilter(new DisallowedMethodsFilter(properties.getDisallowedMethods()));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("disallowedMethodsFilter");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        LOGGER.debug("[ContiNew Starter] - Disallowed HTTP methods on Server: {}.",
            properties.getDisallowedMethods());
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'Web-Server' completed initialization.");
        return registrationBean;
    }
}
