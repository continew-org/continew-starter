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

package top.continew.starter.encrypt.api.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link ApiEncryptProperties} API 加密配置属性测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ApiEncryptPropertiesTest {

    @Test
    void secretKeyHeaderShouldDefaultToXApiEncrypt() {
        assertThat(new ApiEncryptProperties().getSecretKeyHeader()).isEqualTo("X-Api-Encrypt");
    }

    @Test
    void enabledAndKeysShouldBeNullByDefault() {
        ApiEncryptProperties properties = new ApiEncryptProperties();
        assertThat(properties.getEnabled()).isNull();
        assertThat(properties.getPublicKey()).isNull();
        assertThat(properties.getPrivateKey()).isNull();
    }

    @Test
    void settersShouldOverrideDefaults() {
        ApiEncryptProperties properties = new ApiEncryptProperties();
        properties.setEnabled(Boolean.TRUE);
        properties.setSecretKeyHeader("X-Enc-Key");
        properties.setPublicKey("public");
        properties.setPrivateKey("private");
        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getSecretKeyHeader()).isEqualTo("X-Enc-Key");
        assertThat(properties.getPublicKey()).isEqualTo("public");
        assertThat(properties.getPrivateKey()).isEqualTo("private");
    }
}
