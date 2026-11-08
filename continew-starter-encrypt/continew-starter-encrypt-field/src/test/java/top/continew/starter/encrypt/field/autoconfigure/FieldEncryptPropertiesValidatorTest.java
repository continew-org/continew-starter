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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import top.continew.starter.encrypt.enums.Algorithm;
import top.continew.starter.encrypt.field.exception.FieldEncryptException;

/**
 * 字段加密配置校验器测试
 *
 * <p>对称算法必须有合法长度的全局密钥：AES 为 16/24/32 字节、DES 为 8 字节、PBE_WITH_MD5_AND_DES
 * 仅要求非空；非对称与编码算法不依赖对称密钥。异常信息须指向具体配置项。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class FieldEncryptPropertiesValidatorTest {

    /**
     * AES 未配置密钥时应失败并指向密钥配置项。
     */
    @Test
    void shouldFailWhenAesPasswordMissing() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        assertThatThrownBy(() -> FieldEncryptPropertiesValidator.validate(properties))
            .isInstanceOf(FieldEncryptException.class)
            .hasMessageContaining("continew-starter.encrypt.field.password")
            .hasMessageContaining("AES");
    }

    /**
     * AES 空白密钥等同于未配置。
     */
    @Test
    void shouldFailWhenAesPasswordBlank() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setPassword("   ");
        assertThatThrownBy(() -> FieldEncryptPropertiesValidator.validate(properties))
            .isInstanceOf(FieldEncryptException.class)
            .hasMessageContaining("continew-starter.encrypt.field.password");
    }

    /**
     * AES 密钥长度应为 16/24/32 字节，13 字节应失败并说明期望与实际长度。
     */
    @Test
    void shouldFailWhenAesPasswordLengthInvalid() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setPassword("1234567890abc");
        assertThatThrownBy(() -> FieldEncryptPropertiesValidator.validate(properties))
            .isInstanceOf(FieldEncryptException.class)
            .hasMessageContaining("16/24/32")
            .hasMessageContaining("13");
    }

    /**
     * AES 密钥为 16、24、32 字节时校验通过。
     */
    @Test
    void shouldPassWhenAesPasswordLengthValid() {
        for (String password : new String[] {"abcdefghijklmnop", "abcdefghijklmnopqrstuvwx",
            "abcdefghijklmnopqrstuvwxyz012345"}) {
            FieldEncryptProperties properties = new FieldEncryptProperties();
            properties.setPassword(password);
            assertThatCode(() -> FieldEncryptPropertiesValidator.validate(properties))
                .doesNotThrowAnyException();
        }
    }

    /**
     * DES 密钥应恰好 8 字节。
     */
    @Test
    void shouldCheckDesPasswordLength() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setAlgorithm(Algorithm.DES);
        properties.setPassword("12345678");
        assertThatCode(() -> FieldEncryptPropertiesValidator.validate(properties))
            .doesNotThrowAnyException();

        properties.setPassword("123456789");
        assertThatThrownBy(() -> FieldEncryptPropertiesValidator.validate(properties))
            .isInstanceOf(FieldEncryptException.class)
            .hasMessageContaining("continew-starter.encrypt.field.password")
            .hasMessageContaining("DES");
    }

    /**
     * PBE_WITH_MD5_AND_DES 由口令派生密钥，任意非空口令均通过。
     */
    @Test
    void shouldPassWhenPbePasswordAnyLength() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setAlgorithm(Algorithm.PBE_WITH_MD5_AND_DES);
        properties.setPassword("any-pwd");
        assertThatCode(() -> FieldEncryptPropertiesValidator.validate(properties))
            .doesNotThrowAnyException();
    }

    /**
     * 非对称与编码算法不依赖对称密钥，未配置 password 应通过。
     */
    @Test
    void shouldPassWhenKeyNotRequiredAlgorithm() {
        for (Algorithm algorithm : new Algorithm[] {Algorithm.RSA, Algorithm.BASE64}) {
            FieldEncryptProperties properties = new FieldEncryptProperties();
            properties.setAlgorithm(algorithm);
            assertThatCode(() -> FieldEncryptPropertiesValidator.validate(properties))
                .doesNotThrowAnyException();
        }
    }

    /**
     * 全局算法不能配置为 DEFAULT（仅用于注解占位）。
     */
    @Test
    void shouldFailWhenAlgorithmDefault() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setAlgorithm(Algorithm.DEFAULT);
        properties.setPassword("abcdefghijklmnop");
        assertThatThrownBy(() -> FieldEncryptPropertiesValidator.validate(properties))
            .isInstanceOf(FieldEncryptException.class)
            .hasMessageContaining("continew-starter.encrypt.field.algorithm");
    }
}
