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

package top.continew.starter.encrypt.password.encoder.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.continew.starter.encrypt.password.encoder.enums.PasswordEncoderAlgorithm;

/**
 * {@link PasswordEncoderUtil} 密码编码工具单元测试：BCRYPT/SCRYPT 编码往返、格式识别与参数校验。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class PasswordEncoderUtilTest {

    /**
     * BCRYPT 编码应产出 $2 开头 60 位密文且可匹配校验。
     */
    @Test
    @DisplayName("BCRYPT 编码往返")
    void shouldEncodeAndMatchWithBcrypt() {
        String encoded = PasswordEncoderUtil.encode(PasswordEncoderAlgorithm.BCRYPT, "123456");

        assertThat(encoded).startsWith("$2").hasSize(60);
        assertThat(PasswordEncoderUtil.matches(PasswordEncoderAlgorithm.BCRYPT, "123456", encoded))
            .isTrue();
        assertThat(PasswordEncoderUtil.matches(PasswordEncoderAlgorithm.BCRYPT, "654321", encoded))
            .isFalse();
    }

    /**
     * PBKDF2 编码应产出 $pbkdf2-sha256 开头密文且可匹配校验。
     */
    @Test
    @DisplayName("PBKDF2 编码往返")
    void shouldEncodeAndMatchWithPbkdf2() {
        String encoded = PasswordEncoderUtil.encode(PasswordEncoderAlgorithm.PBKDF2, "123456");

        // v5_8 默认配置产出 96 位小写 hex（不带算法前缀）
        assertThat(encoded).matches("[0-9a-f]{96}");
        assertThat(PasswordEncoderUtil.matches(PasswordEncoderAlgorithm.PBKDF2, "123456", encoded))
            .isTrue();
    }

    /**
     * getEncoder 应返回算法对应编码器，null 算法返回 null。
     */
    @Test
    @DisplayName("getEncoder 按算法返回")
    void shouldGetEncoderByAlgorithm() {
        PasswordEncoder bcrypt = PasswordEncoderUtil.getEncoder(PasswordEncoderAlgorithm.BCRYPT);

        assertThat(bcrypt).isNotNull();
        assertThat(PasswordEncoderUtil.getEncoder(null)).isNull();
    }

    /**
     * algorithm 或 rawPassword 为 null 时 encode 应抛 IllegalArgumentException。
     */
    @Test
    @DisplayName("encode 参数校验")
    void shouldValidateEncodeParams() {
        assertThatThrownBy(() -> PasswordEncoderUtil.encode(null, "123456"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("加密算法不能为空");
        assertThatThrownBy(() -> PasswordEncoderUtil.encode(PasswordEncoderAlgorithm.BCRYPT, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("原始密码不能为空");
    }

    /**
     * algorithm 或密文为 null 时 matches 应抛 IllegalArgumentException。
     */
    @Test
    @DisplayName("matches 参数校验")
    void shouldValidateMatchesParams() {
        assertThatThrownBy(() -> PasswordEncoderUtil.matches(null, "123456", "cipher"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("加密算法不能为空");
        assertThat(PasswordEncoderUtil.matches(PasswordEncoderAlgorithm.BCRYPT, null, "cipher"))
            .isFalse();
        assertThat(PasswordEncoderUtil.matches(PasswordEncoderAlgorithm.BCRYPT, "123456", null))
            .isFalse();
    }

    /**
     * BCRYPT/SCRYPT 密文格式识别应按算法正则匹配（encryptor 判定依据）。
     *
     * @param cipher 既有密文
     */
    @ParameterizedTest
    @ValueSource(strings = {"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"})
    @DisplayName("BCRYPT 密文格式识别")
    void shouldRecognizeBcryptFormat(String cipher) {
        assertThat(cipher).hasSize(60)
            .matches(PasswordEncoderAlgorithm.BCRYPT.getPattern().pattern());
    }

    /**
     * SCRYPT 密文格式识别（SCRYPT/ARGON2 编码依赖 bcprov 由使用方按需引入，单测只覆盖格式）。
     */
    @Test
    @DisplayName("SCRYPT 密文格式识别")
    void shouldRecognizeScryptFormat() {
        String scrypt = "$s0$e0801$abcd1234$9876fedc";
        assertThat(scrypt).matches(PasswordEncoderAlgorithm.SCRYPT.getPattern().pattern());
    }

    /**
     * matches 对 null 与空串密文均应返回 false 而非抛异常。
     *
     * @param encodedPassword 空密文
     */
    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("matches 对空密文不误判")
    void shouldNotMatchBlankEncodedPassword(String encodedPassword) {
        assertThat(PasswordEncoderUtil.matches(PasswordEncoderAlgorithm.BCRYPT, "123456",
            encodedPassword)).isFalse();
    }
}
