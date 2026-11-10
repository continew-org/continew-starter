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

package top.continew.starter.encrypt.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import top.continew.starter.encrypt.encryptor.AesEncryptor;
import top.continew.starter.encrypt.encryptor.Base64Encryptor;
import top.continew.starter.encrypt.encryptor.DesEncryptor;
import top.continew.starter.encrypt.encryptor.IEncryptor;
import top.continew.starter.encrypt.encryptor.PbeWithMd5AndDesEncryptor;
import top.continew.starter.encrypt.encryptor.RsaEncryptor;

import java.util.stream.Stream;

/**
 * {@link Algorithm} 枚举全值遍历测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AlgorithmTest {

    @Test
    @DisplayName("枚举共 6 个值")
    void shouldContainExactlySixValues() {
        assertThat(Algorithm.values()).hasSize(6);
    }

    @ParameterizedTest(name = "[{index}] 枚举值 {0} 可按名称反查且名称非空")
    @EnumSource(Algorithm.class)
    void shouldSupportValueOfForEveryConstant(Algorithm algorithm) {
        assertThat(algorithm.name()).isNotBlank();
        assertThat(Algorithm.valueOf(algorithm.name())).isSameAs(algorithm);
    }

    @ParameterizedTest(name = "[{index}] {0} 对应的加密器类映射正确")
    @MethodSource("encryptorClassProvider")
    void shouldMapEachConstantToItsEncryptorClass(String name,
        Class<? extends IEncryptor> expected) {
        assertThat(Algorithm.valueOf(name).getEncryptor()).isSameAs(expected);
    }

    private static Stream<Arguments> encryptorClassProvider() {
        return Stream.of(arguments("DEFAULT", null), arguments("AES", AesEncryptor.class),
            arguments("DES",
                DesEncryptor.class),
            arguments("PBE_WITH_MD5_AND_DES", PbeWithMd5AndDesEncryptor.class), arguments("RSA",
                RsaEncryptor.class),
            arguments("BASE64", Base64Encryptor.class));
    }
}
