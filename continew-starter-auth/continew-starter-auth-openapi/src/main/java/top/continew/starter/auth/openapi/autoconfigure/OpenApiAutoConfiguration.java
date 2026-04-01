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

package top.continew.starter.auth.openapi.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import top.continew.starter.auth.openapi.autoconfigure.dao.OpenApiAppDaoConfiguration;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

/**
 * 开放 API 自动配置
 *
 * @author Charles7c
 * @since 2.16.0
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenApiProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.AUTH_OPENAPI, name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
@PropertySource(value = "classpath:default-auth-openapi.yml", factory = GeneralPropertySourceFactory.class)
@Import({OpenApiWebConfiguration.class, OpenApiAppDaoConfiguration.class})
public class OpenApiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OpenApiAutoConfiguration.class);

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'OpenApi' completed initialization.");
    }
}
