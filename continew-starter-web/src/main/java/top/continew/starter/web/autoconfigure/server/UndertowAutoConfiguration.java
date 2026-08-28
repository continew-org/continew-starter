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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

import io.undertow.Undertow;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.util.HttpString;
import org.springframework.context.annotation.PropertySource;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.CollUtils;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

/**
 * Undertow 自动配置
 *
 * @author Jasmine
 * @author Charles7c
 * @since 2.11.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(Undertow.class)
@EnableConfigurationProperties(ServerExtensionProperties.class)
@PropertySource(value = "classpath:default-server.yml",
    factory = GeneralPropertySourceFactory.class)
@ConditionalOnProperty(prefix = "server.extension", name = PropertiesConstants.ENABLED,
    havingValue = "true")
public class UndertowAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UndertowAutoConfiguration.class);

    /**
     * Undertow 自定义配置
     */
    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> customize(
        ServerExtensionProperties properties) {
        return factory -> {
            factory.addDeploymentInfoCustomizers(deploymentInfo -> deploymentInfo
                .addInitialHandlerChainWrapper(
                    handler -> new DisallowedMethodsHandler(handler, CollUtils
                        .mapToSet(properties.getDisallowedMethods(), HttpString::tryFromString))));
            log.debug("[ContiNew Starter] - Disallowed HTTP methods on Server Undertow: {}.",
                properties
                    .getDisallowedMethods());
            log.debug(
                "[ContiNew Starter] - Auto Configuration 'Web-Server Undertow' completed initialization.");
        };
    }
}
