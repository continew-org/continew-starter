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

package top.charles7c.continew.starter.core.autoconfigure.password;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

/**
 * 密码编解码配置属性
 *
 * @author Jasmine
 * @since 1.3.0
 */
@ConfigurationProperties(PropertiesConstants.PASSWORD_ENCODER)
public class PasswordEncoderProperties {

    /**
     * 是否启用密码编解码配置
     */
    private boolean enabled = false;

    /**
     * 启用的算法 ID
     */
    private String encodingId;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEncodingId() {
        return encodingId;
    }

    public void setEncodingId(String encodingId) {
        this.encodingId = encodingId;
    }

    @Override
    public String toString() {
        return "PasswordEncoderProperties{" + "enabled=" + enabled + ", encodingId='" + encodingId + '\'' + '}';
    }
}