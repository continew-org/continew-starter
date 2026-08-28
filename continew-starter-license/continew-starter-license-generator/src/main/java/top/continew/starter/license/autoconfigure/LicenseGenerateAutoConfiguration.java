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

package top.continew.starter.license.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import top.continew.starter.license.service.LicenseCreateService;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * license 生成模块 自动配置
 *
 * @author loach
 * @since 2.12.0
 */
@AutoConfiguration
@EnableConfigurationProperties(LicenseGenerateProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.LICENSE_GENERATOR,
    name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class LicenseGenerateAutoConfiguration {

    private static final Logger log =
        LoggerFactory.getLogger(LicenseGenerateAutoConfiguration.class);

    /**
     * license 生成服务接口
     */
    @Bean
    @ConditionalOnMissingBean
    public LicenseCreateService licenseCreateService() {
        return LicenseCreateService.getInstance();
    }

    @PostConstruct
    public void postConstruct() {
        log.debug(
            "[ContiNew Starter] - Auto Configuration 'License-Generator' completed initialization.");
    }
}
