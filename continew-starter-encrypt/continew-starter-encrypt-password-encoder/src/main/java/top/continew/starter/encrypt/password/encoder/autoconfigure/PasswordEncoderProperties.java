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

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.encrypt.password.encoder.enums.PasswordEncoderAlgorithm;

/**
 * 密码编码器配置属性
 *
 * @author Jasmine
 * @author Charles7c
 * @since 1.3.0
 */
@ConfigurationProperties(PropertiesConstants.ENCRYPT_PASSWORD_ENCODER)
public class PasswordEncoderProperties {

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 默认启用的编码器算法（默认：BCrypt 加密算法）
     */
    private PasswordEncoderAlgorithm algorithm = PasswordEncoderAlgorithm.BCRYPT;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public PasswordEncoderAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(PasswordEncoderAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}
