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

package top.continew.starter.auth.justauth.autoconfigure;

import jakarta.annotation.PostConstruct;
import me.zhyd.oauth.cache.AuthStateCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import top.continew.starter.auth.justauth.AuthRequestFactory;
import top.continew.starter.auth.justauth.autoconfigure.cache.JustAuthStateCacheConfiguration;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * JustAuth 自动配置
 *
 * @author Charles7c
 * @author <a href="https://gitee.com/justauth/justauth-spring-boot-starter">yangkai.shen</a>
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(JustAuthProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.AUTH_JUSTAUTH,
    name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
@Import({JustAuthStateCacheConfiguration.class})
public class JustAuthAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(JustAuthAutoConfiguration.class);

    /**
     * AuthRequest 工厂配置
     */
    @Bean
    public AuthRequestFactory authRequestFactory(JustAuthProperties properties,
        AuthStateCache stateCache) {
        return new AuthRequestFactory(properties, stateCache);
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER
            .debug("[ContiNew Starter] - Auto Configuration 'JustAuth' completed initialization.");
    }
}
