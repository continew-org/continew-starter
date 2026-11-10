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

package top.continew.starter.encrypt.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.encrypt.encryptor.AesEncryptor;
import top.continew.starter.encrypt.encryptor.Base64Encryptor;
import top.continew.starter.encrypt.enums.Algorithm;

/**
 * {@link CryptoContext} 属性读写与相等性测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CryptoContextTest {

    private CryptoContext newFullContext() {
        CryptoContext context = new CryptoContext();
        context.setAlgorithm(Algorithm.AES);
        context.setEncryptor(Base64Encryptor.class);
        context.setPassword("1234567890123456");
        context.setPublicKey("public-key");
        context.setPrivateKey("private-key");
        return context;
    }

    @Test
    @DisplayName("getter 返回 setter 写入的值")
    void shouldReturnValuesWrittenBySetters() {
        CryptoContext context = newFullContext();
        assertThat(context.getAlgorithm()).isEqualTo(Algorithm.AES);
        assertThat(context.getEncryptor()).isEqualTo(Base64Encryptor.class);
        assertThat(context.getPassword()).isEqualTo("1234567890123456");
        assertThat(context.getPublicKey()).isEqualTo("public-key");
        assertThat(context.getPrivateKey()).isEqualTo("private-key");
    }

    @Test
    @DisplayName("全字段一致的上下文相等且哈希一致")
    void shouldBeEqualAndSameHashCodeWhenAllFieldsSame() {
        CryptoContext first = newFullContext();
        CryptoContext second = newFullContext();
        assertThat(first).isEqualTo(first).isEqualTo(second);
        assertThat(second).isEqualTo(first);
        assertThat(first.hashCode()).hasSameHashCodeAs(second.hashCode());
    }

    @Test
    @DisplayName("全字段为空的上下文相等且哈希一致")
    void shouldBeEqualWhenAllFieldsNull() {
        CryptoContext first = new CryptoContext();
        CryptoContext second = new CryptoContext();
        assertThat(first).isEqualTo(second).isNotEqualTo(newFullContext());
        assertThat(first.hashCode()).hasSameHashCodeAs(second.hashCode());
    }

    @Test
    @DisplayName("任一字段不同则上下文不相等")
    void shouldNotBeEqualWhenAnyFieldDiffers() {
        CryptoContext base = newFullContext();

        CryptoContext differentAlgorithm = newFullContext();
        differentAlgorithm.setAlgorithm(Algorithm.DES);
        assertThat(differentAlgorithm).isNotEqualTo(base);

        CryptoContext differentEncryptor = newFullContext();
        differentEncryptor.setEncryptor(AesEncryptor.class);
        assertThat(differentEncryptor).isNotEqualTo(base);

        CryptoContext differentPassword = newFullContext();
        differentPassword.setPassword("6543210987654321");
        assertThat(differentPassword).isNotEqualTo(base);

        CryptoContext differentPublicKey = newFullContext();
        differentPublicKey.setPublicKey("other-public-key");
        assertThat(differentPublicKey).isNotEqualTo(base);

        CryptoContext differentPrivateKey = newFullContext();
        differentPrivateKey.setPrivateKey("other-private-key");
        assertThat(differentPrivateKey).isNotEqualTo(base);
    }

    @Test
    @DisplayName("与 null 及其他类型比较不相等")
    void shouldNotBeEqualToNullOrOtherType() {
        CryptoContext context = newFullContext();
        assertThat(context).isNotEqualTo(null).isNotEqualTo("not-a-context");
    }
}
