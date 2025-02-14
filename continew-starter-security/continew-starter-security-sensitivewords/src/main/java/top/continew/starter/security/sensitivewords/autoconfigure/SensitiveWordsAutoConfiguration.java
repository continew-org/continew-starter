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

package top.continew.starter.security.sensitivewords.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.continew.starter.security.sensitivewords.service.DefaultSensitiveWordsConfig;
import top.continew.starter.security.sensitivewords.service.DefaultSensitiveWordsService;
import top.continew.starter.security.sensitivewords.service.SensitiveWordsConfig;
import top.continew.starter.security.sensitivewords.service.SensitiveWordsService;

/**
 * 敏感词自动配置
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.9.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SensitiveWordsProperties.class)
public class SensitiveWordsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordsAutoConfiguration.class);

    /**
     * 默认敏感词配置
     */
    @Bean
    @ConditionalOnMissingBean
    public SensitiveWordsConfig sensitiveWordsConfig(SensitiveWordsProperties properties) {
        return new DefaultSensitiveWordsConfig(properties);
    }

    /**
     * 默认敏感词服务
     */
    @Bean
    @ConditionalOnMissingBean
    public SensitiveWordsService sensitiveWordsService(SensitiveWordsConfig sensitiveWordsConfig) {
        return new DefaultSensitiveWordsService(sensitiveWordsConfig);
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Security-Sensitive Words' completed initialization.");
    }
}
