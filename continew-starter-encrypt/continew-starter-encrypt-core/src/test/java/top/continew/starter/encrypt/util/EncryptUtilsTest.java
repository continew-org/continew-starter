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

package top.continew.starter.encrypt.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.SecureUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.KeyPair;

/**
 * {@link EncryptUtils} 工具方法直接断言测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class EncryptUtilsTest {

    /**
     * 16 位 AES 合法密钥
     */
    private static final String PASSWORD = "1234567890123456";

    private static String publicKey;

    private static String privateKey;

    @BeforeAll
    static void generateKeyPair() {
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 2048);
        publicKey = Base64.encode(keyPair.getPublic().getEncoded());
        privateKey = Base64.encode(keyPair.getPrivate().getEncoded());
    }

    @ParameterizedTest(name = "[{index}] 数据「{0}」Base64 编码后解码结果一致")
    @ValueSource(strings = {"hello, world", "你好，加密工具类！『中文』emoji😀", ""})
    void shouldRoundTripBase64(String data) {
        assertThat(EncryptUtils.decodeByBase64(EncryptUtils.encodeByBase64(data))).isEqualTo(data);
    }

    @Test
    @DisplayName("Base64 编码结果为标准编码")
    void shouldEncodeWithStandardBase64() {
        assertThat(EncryptUtils.encodeByBase64("hello")).isEqualTo("aGVsbG8=");
    }

    @ParameterizedTest(name = "[{index}] {0} 位密钥 AES 加解密往返一致")
    @ValueSource(strings = {"1234567890123456", "123456789012345678901234",
        "12345678901234567890123456789012"})
    void shouldRoundTripAesWithLegalKeyLength(String password) {
        String data = "你好，AES 工具方法 secret";
        String ciphertext = EncryptUtils.encryptByAes(data, password);
        assertThat(ciphertext).isNotBlank();
        assertThat(EncryptUtils.decryptByAes(ciphertext, password)).isEqualTo(data);
    }

    @Test
    @DisplayName("超长数据（10KB 以上）AES 加解密往返一致")
    void shouldRoundTripAesLongData() {
        String data = " ContinuNew ".repeat(1000);
        assertThat(EncryptUtils.decryptByAes(EncryptUtils.encryptByAes(data, PASSWORD), PASSWORD))
            .isEqualTo(data);
    }

    @ParameterizedTest(name = "[{index}] 密码「{0}」为空白时 AES 加解密均抛出 IllegalArgumentException")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowIllegalArgumentExceptionWhenAesPasswordBlank(String password) {
        assertThatThrownBy(() -> EncryptUtils.encryptByAes("data", password))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AES需要传入秘钥信息");
        assertThatThrownBy(() -> EncryptUtils.decryptByAes("data", password))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AES需要传入秘钥信息");
    }

    @ParameterizedTest(name = "[{index}] 长度 {0} 位的密码不符合 AES 密钥长度要求")
    @ValueSource(strings = {"123456789012345", "123456789012345678901234567890123"})
    void shouldThrowIllegalArgumentExceptionWhenAesKeyLengthIllegal(String password) {
        assertThatThrownBy(() -> EncryptUtils.encryptByAes("data", password))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AES秘钥长度要求为16位、24位、32位");
        assertThatThrownBy(() -> EncryptUtils.decryptByAes("data", password))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AES秘钥长度要求为16位、24位、32位");
    }

    @Test
    @DisplayName("错误密钥 AES 解密抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenAesDecryptWithWrongKey() {
        String ciphertext = EncryptUtils.encryptByAes("secret", PASSWORD);
        assertThatThrownBy(() -> EncryptUtils.decryptByAes(ciphertext, "6543210987654321"))
            .isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("非法密文 AES 解密抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenAesCiphertextIllegal() {
        assertThatThrownBy(() -> EncryptUtils.decryptByAes("!!!not-b64!!!", PASSWORD))
            .isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("非法密文 RSA 解密抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenRsaCiphertextIllegal() {
        assertThatThrownBy(() -> EncryptUtils.decryptByRsa("!!!not-b64!!!", privateKey))
            .isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("RSA 公钥加密私钥解密往返一致")
    void shouldRoundTripRsa() {
        String data = "你好，RSA 工具方法 secret";
        assertThat(
            EncryptUtils.decryptByRsa(EncryptUtils.encryptByRsa(data, publicKey), privateKey))
            .isEqualTo(data);
    }

    @Test
    @DisplayName("超长数据（500 字节，跨加密块）RSA 加解密往返一致")
    void shouldRoundTripRsaLongData() {
        String data = "a".repeat(500);
        assertThat(
            EncryptUtils.decryptByRsa(EncryptUtils.encryptByRsa(data, publicKey), privateKey))
            .isEqualTo(data);
    }

    @ParameterizedTest(name = "[{index}] 公钥「{0}」为空白时 RSA 加密抛出 IllegalArgumentException")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowIllegalArgumentExceptionWhenRsaPublicKeyBlank(String publicKeyValue) {
        assertThatThrownBy(() -> EncryptUtils.encryptByRsa("data", publicKeyValue))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("RSA需要传入公钥进行加密");
    }

    @ParameterizedTest(name = "[{index}] 私钥「{0}」为空白时 RSA 解密抛出 IllegalArgumentException")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowIllegalArgumentExceptionWhenRsaPrivateKeyBlank(String privateKeyValue) {
        assertThatThrownBy(() -> EncryptUtils.decryptByRsa("data", privateKeyValue))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("RSA需要传入私钥进行解密");
    }
}
