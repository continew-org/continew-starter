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

import cn.hutool.crypto.CryptoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.continew.starter.encrypt.context.CryptoContext;

/**
 * {@link DesEncryptor} 加解密往返与边界测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DesEncryptorTest {

    /**
     * 8 位 DES 合法密钥
     */
    private static final String PASSWORD = "12345678";

    private DesEncryptor newEncryptor(String password) {
        CryptoContext context = new CryptoContext();
        context.setPassword(password);
        return new DesEncryptor(context);
    }

    @ParameterizedTest(name = "[{index}] 明文「{0}」加密后解密结果一致")
    @ValueSource(strings = {"hello, world", "你好，ContiNew Starter！『中文』emoji😀"})
    void shouldRoundTripWhenPlaintextValid(String plaintext) {
        DesEncryptor encryptor = newEncryptor(PASSWORD);
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @ParameterizedTest(name = "[{index}] 空白输入「{0}」原样返回")
    @ValueSource(strings = {"", " "})
    void shouldReturnBlankInputAsIs(String plaintext) {
        DesEncryptor encryptor = newEncryptor(PASSWORD);
        assertThat(encryptor.encrypt(plaintext)).isEqualTo(plaintext);
        assertThat(encryptor.decrypt(plaintext)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("超长明文（10KB 以上）加密后解密结果一致")
    void shouldRoundTripLongPlaintext() {
        DesEncryptor encryptor = newEncryptor(PASSWORD);
        String plaintext = " ContinuNew ".repeat(1000);
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("错误密钥解密密文抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenDecryptWithWrongKey() {
        DesEncryptor encryptor = newEncryptor(PASSWORD);
        String ciphertext = encryptor.encrypt("secret");
        DesEncryptor other = newEncryptor("87654321");
        assertThatThrownBy(() -> other.decrypt(ciphertext)).isInstanceOf(CryptoException.class);
    }
}
