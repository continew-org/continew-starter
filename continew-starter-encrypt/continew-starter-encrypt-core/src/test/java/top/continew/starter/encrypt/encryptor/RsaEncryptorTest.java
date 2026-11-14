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

package top.continew.starter.encrypt.encryptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.SecureUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.continew.starter.encrypt.context.CryptoContext;

import java.security.KeyPair;

/**
 * {@link RsaEncryptor} 加解密往返与边界测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RsaEncryptorTest {

    /**
     * RSA 密钥位长：2048 位单块明文上限为 245 字节
     */
    private static final int KEY_SIZE = 2048;

    private static String publicKey;

    private static String privateKey;

    private static String otherPrivateKey;

    @BeforeAll
    static void generateKeyPairs() {
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", KEY_SIZE);
        publicKey = Base64.encode(keyPair.getPublic().getEncoded());
        privateKey = Base64.encode(keyPair.getPrivate().getEncoded());
        otherPrivateKey =
            Base64.encode(SecureUtil.generateKeyPair("RSA", KEY_SIZE).getPrivate().getEncoded());
    }

    private RsaEncryptor newEncryptor(String publicKeyValue, String privateKeyValue) {
        CryptoContext context = new CryptoContext();
        context.setPublicKey(publicKeyValue);
        context.setPrivateKey(privateKeyValue);
        return new RsaEncryptor(context);
    }

    @ParameterizedTest(name = "[{index}] 明文「{0}」加密后解密结果一致")
    @ValueSource(strings = {"hello, world", "你好，ContiNew Starter！『中文』emoji😀"})
    void shouldRoundTripWhenPlaintextValid(String plaintext) {
        RsaEncryptor encryptor = newEncryptor(publicKey, privateKey);
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("空串加密后得到非空密文且可解密回空串")
    void shouldRoundTripEmptyPlaintext() {
        RsaEncryptor encryptor = newEncryptor(publicKey, privateKey);
        String ciphertext = encryptor.encrypt("");
        assertThat(ciphertext).isNotEmpty();
        assertThat(encryptor.decrypt(ciphertext)).isEmpty();
    }

    @Test
    @DisplayName("200 字节长明文（未超单块上限）加密后解密结果一致")
    void shouldRoundTripLongPlaintextWithinBlockLimit() {
        RsaEncryptor encryptor = newEncryptor(publicKey, privateKey);
        String plaintext = "a".repeat(200);
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("超过单块上限（245 字节）的明文由 Hutool 自动分段加密，往返一致")
    void shouldRoundTripPlaintextExceedingSingleBlockLimit() {
        RsaEncryptor encryptor = newEncryptor(publicKey, privateKey);
        String plaintext = "a".repeat(500);
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("加密 null 明文抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenEncryptNull() {
        RsaEncryptor encryptor = newEncryptor(publicKey, privateKey);
        assertThatThrownBy(() -> encryptor.encrypt(null)).isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("非法公钥加密抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenPublicKeyIllegal() {
        RsaEncryptor encryptor = newEncryptor("not-a-valid-key", privateKey);
        assertThatThrownBy(() -> encryptor.encrypt("secret")).isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("错误私钥解密密文抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenDecryptWithWrongPrivateKey() {
        RsaEncryptor encryptor = newEncryptor(publicKey, privateKey);
        String ciphertext = encryptor.encrypt("secret");
        RsaEncryptor other = newEncryptor(publicKey, otherPrivateKey);
        assertThatThrownBy(() -> other.decrypt(ciphertext)).isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("公钥与私钥均未配置时加密快速失败并给出配置提示")
    void shouldFailFastWhenEncryptWithoutAnyKey() {
        RsaEncryptor encryptor = newEncryptor(null, null);
        assertThatThrownBy(() -> encryptor.encrypt("secret")).isInstanceOf(CryptoException.class)
            .hasMessageContaining("公钥");
    }

    @Test
    @DisplayName("公钥与私钥均未配置时解密快速失败并给出配置提示")
    void shouldFailFastWhenDecryptWithoutAnyKey() {
        RsaEncryptor encryptor = newEncryptor(null, null);
        assertThatThrownBy(() -> encryptor.decrypt("ciphertext"))
            .isInstanceOf(CryptoException.class)
            .hasMessageContaining("私钥");
    }

    @Test
    @DisplayName("仅配置公钥时加密正常，解密快速失败并提示私钥缺失")
    void shouldEncryptWhenOnlyPublicKeyConfigured() {
        RsaEncryptor encryptor = newEncryptor(publicKey, null);
        assertThat(encryptor.encrypt("secret")).isNotBlank();
        assertThatThrownBy(() -> encryptor.decrypt("ciphertext"))
            .isInstanceOf(CryptoException.class)
            .hasMessageContaining("私钥");
    }

    @Test
    @DisplayName("仅配置私钥时解密正常，加密快速失败并提示公钥缺失")
    void shouldDecryptWhenOnlyPrivateKeyConfigured() {
        RsaEncryptor encryptor = newEncryptor(null, privateKey);
        String ciphertext = newEncryptor(publicKey, privateKey).encrypt("secret");
        assertThat(encryptor.decrypt(ciphertext)).isEqualTo("secret");
        assertThatThrownBy(() -> encryptor.encrypt("secret")).isInstanceOf(CryptoException.class)
            .hasMessageContaining("公钥");
    }
}
