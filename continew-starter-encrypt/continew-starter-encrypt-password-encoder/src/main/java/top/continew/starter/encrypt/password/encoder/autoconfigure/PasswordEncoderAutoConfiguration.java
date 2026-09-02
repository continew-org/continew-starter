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

package top.continew.starter.encrypt.password.encoder.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.encrypt.password.encoder.enums.PasswordEncoderAlgorithm;
import top.continew.starter.encrypt.password.encoder.util.PasswordEncoderUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 密码编码器自动配置
 *
 * @author Charles7c
 * @since 2.14.0
 */
@AutoConfiguration
@EnableConfigurationProperties(PasswordEncoderProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.ENCRYPT_PASSWORD_ENCODER,
    name = PropertiesConstants.ENABLED, havingValue = "true")
public class PasswordEncoderAutoConfiguration {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(PasswordEncoderAutoConfiguration.class);

    /**
     * 密码编码器配置
     *
     * @see DelegatingPasswordEncoder
     * @see PasswordEncoderFactories
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder(PasswordEncoderProperties properties) {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(PasswordEncoderAlgorithm.BCRYPT.name().toLowerCase(), PasswordEncoderUtil
            .getEncoder(PasswordEncoderAlgorithm.BCRYPT));
        encoders.put(PasswordEncoderAlgorithm.SCRYPT.name().toLowerCase(), PasswordEncoderUtil
            .getEncoder(PasswordEncoderAlgorithm.SCRYPT));
        encoders.put(PasswordEncoderAlgorithm.PBKDF2.name().toLowerCase(), PasswordEncoderUtil
            .getEncoder(PasswordEncoderAlgorithm.PBKDF2));
        encoders.put(PasswordEncoderAlgorithm.ARGON2.name().toLowerCase(), PasswordEncoderUtil
            .getEncoder(PasswordEncoderAlgorithm.ARGON2));
        // 配置缺省时回退到默认算法（BCRYPT），避免 algorithm 为 null 导致 NPE
        PasswordEncoderAlgorithm algorithm = Optional.ofNullable(properties.getAlgorithm())
            .orElse(PasswordEncoderAlgorithm.BCRYPT);
        CheckUtils.throwIf(PasswordEncoderUtil.getEncoder(algorithm) == null, "不支持的加密算法: {}",
            algorithm);
        return new DelegatingPasswordEncoder(algorithm.name().toLowerCase(), encoders);
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'Encrypt-Password Encoder' completed initialization.");
    }
}
