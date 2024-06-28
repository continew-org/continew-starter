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

package top.continew.starter.security.crypto.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.security.crypto.core.MyBatisDecryptInterceptor;
import top.continew.starter.security.crypto.core.MyBatisEncryptInterceptor;

/**
 * 加/解密自动配置
 *
 * @author Charles7c
 * @since 1.4.0
 */
@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.SECURITY_CRYPTO, name = PropertiesConstants.ENABLED, matchIfMissing = true)
public class CryptoAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CryptoAutoConfiguration.class);
    private final CryptoProperties properties;

    public CryptoAutoConfiguration(CryptoProperties properties) {
        this.properties = properties;
    }

    /**
     * MyBatis 加密拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean(MyBatisEncryptInterceptor.class)
    public MyBatisEncryptInterceptor myBatisEncryptInterceptor() {
        return new MyBatisEncryptInterceptor(properties);
    }

    /**
     * MyBatis 解密拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean(MyBatisDecryptInterceptor.class)
    public MyBatisDecryptInterceptor myBatisDecryptInterceptor() {
        return new MyBatisDecryptInterceptor(properties);
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Security-Crypto' completed initialization.");
    }
}
