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

package top.continew.starter.encrypt.field.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.encrypt.field.interceptor.MyBatisDecryptInterceptor;
import top.continew.starter.encrypt.field.interceptor.MyBatisEncryptInterceptor;
import top.continew.starter.encrypt.field.util.EncryptHelper;

/**
 * 字段加密自动配置
 *
 * @author Charles7c
 * @author lishuyan
 * @since 1.4.0
 */
@AutoConfiguration
@EnableConfigurationProperties(FieldEncryptProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.ENCRYPT_FIELD,
    name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class FieldEncryptAutoConfiguration {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(FieldEncryptAutoConfiguration.class);
    private final FieldEncryptProperties properties;

    public FieldEncryptAutoConfiguration(FieldEncryptProperties properties) {
        this.properties = properties;
    }

    /**
     * MyBatis 加密拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MyBatisEncryptInterceptor mybatisEncryptInterceptor() {
        return new MyBatisEncryptInterceptor();
    }

    /**
     * MyBatis 解密拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean(MyBatisDecryptInterceptor.class)
    public MyBatisDecryptInterceptor mybatisDecryptInterceptor() {
        return new MyBatisDecryptInterceptor();
    }

    @PostConstruct
    public void postConstruct() {
        EncryptHelper.init(properties);
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'Encrypt-Field' completed initialization.");
    }
}
