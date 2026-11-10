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
import top.continew.starter.encrypt.context.CryptoContext;

/**
 * {@link AbstractSymmetricCryptoEncryptor} 测试：以 AES 实现为例验证对称加密器基类的空白透传、
 * 加密实例缓存与错误密钥异常路径。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AbstractSymmetricCryptoEncryptorTest {

    /**
     * 16 位 AES 合法密钥
     */
    private static final String PASSWORD = "1234567890123456";

    private AesEncryptor newEncryptor(String password) {
        CryptoContext context = new CryptoContext();
        context.setPassword(password);
        return new AesEncryptor(context);
    }

    @Test
    @DisplayName("空白输入不参与加密，原样返回")
    void shouldReturnBlankInputAsIs() {
        AesEncryptor encryptor = newEncryptor(PASSWORD);
        assertThat(encryptor.encrypt("")).isEmpty();
        assertThat(encryptor.encrypt("   ")).isEqualTo("   ");
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt("")).isEmpty();
        assertThat(encryptor.decrypt("   ")).isEqualTo("   ");
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("相同算法与密钥复用缓存的加密实例")
    void shouldReuseCachedCryptoForSameAlgorithmAndPassword() {
        AesEncryptor first = newEncryptor(PASSWORD);
        AesEncryptor second = newEncryptor(PASSWORD);
        assertThat(first.getCrypto(PASSWORD)).isSameAs(second.getCrypto(PASSWORD));
    }

    @Test
    @DisplayName("不同密钥使用不同的加密实例")
    void shouldCreateSeparateCryptoForDifferentPassword() {
        String otherPassword = "6543210987654321";
        AesEncryptor first = newEncryptor(PASSWORD);
        AesEncryptor second = newEncryptor(otherPassword);
        assertThat(first.getCrypto(PASSWORD)).isNotSameAs(second.getCrypto(otherPassword));
    }

    @Test
    @DisplayName("错误密钥解密密文抛出 CryptoException")
    void shouldThrowCryptoExceptionWhenDecryptWithWrongPassword() {
        AesEncryptor encryptor = newEncryptor(PASSWORD);
        String ciphertext = encryptor.encrypt("secret");
        AesEncryptor other = newEncryptor("6543210987654321");
        assertThatThrownBy(() -> other.decrypt(ciphertext)).isInstanceOf(CryptoException.class);
    }

    @Test
    @DisplayName("上下文未配置密码时加密抛出 NullPointerException")
    void shouldThrowNullPointerExceptionWhenPasswordMissing() {
        AesEncryptor encryptor = newEncryptor(null);
        assertThatThrownBy(() -> encryptor.encrypt("secret"))
            .isInstanceOf(NullPointerException.class);
    }
}
