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
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link Base64Encryptor} 加解密往返与边界测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class Base64EncryptorTest {

    @ParameterizedTest(name = "[{index}] 明文「{0}」编码后解码结果一致")
    @ValueSource(strings = {"hello, world", "你好，ContiNew Starter！『中文』emoji😀", ""})
    void shouldRoundTripWhenPlaintextValid(String plaintext) {
        Base64Encryptor encryptor = new Base64Encryptor();
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("编码结果为标准 Base64 编码")
    void shouldEncodeWithStandardBase64() {
        assertThat(new Base64Encryptor().encrypt("hello")).isEqualTo("aGVsbG8=");
    }

    @Test
    @DisplayName("超长明文（100KB）编码后解码结果一致")
    void shouldRoundTripLongPlaintext() {
        Base64Encryptor encryptor = new Base64Encryptor();
        String plaintext = " ContinuNew ".repeat(10000);
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("非法 Base64 密文解码不抛出异常（Hutool 宽容处理）")
    void shouldNotThrowWhenDecryptIllegalBase64() {
        assertThatCode(() -> new Base64Encryptor().decrypt("!!!not-b64!!!"))
            .doesNotThrowAnyException();
    }
}
