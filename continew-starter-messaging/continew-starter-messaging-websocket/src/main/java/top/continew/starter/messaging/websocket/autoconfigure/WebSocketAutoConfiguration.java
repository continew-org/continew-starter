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

package top.continew.starter.messaging.websocket.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;
import top.continew.starter.messaging.websocket.core.WebSocketInterceptor;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDaoDefaultImpl;

/**
 * WebSocket 自动配置
 *
 * @author WeiRan
 * @author Charles7c
 * @since 2.1.0
 */
@AutoConfiguration
@EnableWebSocket
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.MESSAGING_WEBSOCKET, name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class WebSocketAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAutoConfiguration.class);
    private final WebSocketProperties properties;

    public WebSocketAutoConfiguration(WebSocketProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WebSocketConfigurer webSocketConfigurer(WebSocketHandler handler, HandshakeInterceptor interceptor) {
        return registry -> registry.addHandler(handler, properties.getPath())
            .addInterceptors(interceptor)
            .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public WebSocketHandler webSocketHandler(WebSocketSessionDao webSocketSessionDao) {
        return new top.continew.starter.messaging.websocket.core.WebSocketHandler(properties, webSocketSessionDao);
    }

    @Bean
    @ConditionalOnMissingBean
    public HandshakeInterceptor handshakeInterceptor(WebSocketClientService webSocketClientService) {
        return new WebSocketInterceptor(properties, webSocketClientService);
    }

    /**
     * WebSocket 会话 DAO
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketSessionDao webSocketSessionDao() {
        return new WebSocketSessionDaoDefaultImpl();
    }

    /**
     * WebSocket 客户端服务（如不提供，则报错）
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketClientService webSocketClientService() {
        throw new NoSuchBeanDefinitionException(WebSocketClientService.class);
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Messaging-WebSocket' completed initialization.");
    }
}
