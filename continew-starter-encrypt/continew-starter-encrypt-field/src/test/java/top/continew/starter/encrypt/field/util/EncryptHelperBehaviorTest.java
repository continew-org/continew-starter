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

package top.continew.starter.encrypt.field.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.hutool.core.util.ReflectUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;
import top.continew.starter.encrypt.field.autoconfigure.FieldEncryptProperties;
import top.continew.starter.encrypt.field.exception.FieldEncryptException;

/**
 * 加密助手行为测试
 *
 * <p>fail-fast（默认）模式下加解密失败抛 {@link FieldEncryptException}，异常信息包含字段名与算法；
 * fail-fast=false 时降级为返回原值。正常路径的加密解密往返不应受影响。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class EncryptHelperBehaviorTest {

    private static final String VALID_PASSWORD = "abcdefghijklmnop";

    private static final String PHONE = "13800138000";

    /**
     * 携带非法注解级密钥（AES 密钥 5 字节）的样例实体
     */
    static class InvalidKeyEntity {

        @FieldEncrypt(password = "12345")
        private String phone;
    }

    /**
     * 使用全局默认配置的样例实体
     */
    static class DefaultEntity {

        @FieldEncrypt
        private String phone;
    }

    @AfterEach
    void resetDefaultProperties() {
        EncryptHelper.init(validProperties());
    }

    /**
     * 正常路径：加密后密文不同于明文，且可解密还原。
     */
    @Test
    void shouldEncryptAndDecryptRoundtrip() {
        EncryptHelper.init(validProperties());
        FieldEncrypt fieldEncrypt = fieldEncrypt(DefaultEntity.class, "phone");
        String ciphertext = EncryptHelper.encrypt(PHONE, fieldEncrypt);
        assertThat(ciphertext).isNotEqualTo(PHONE);
        assertThat(EncryptHelper.decrypt(ciphertext, fieldEncrypt)).isEqualTo(PHONE);
    }

    /**
     * fail-fast（默认）模式下，加密失败应抛异常且信息包含字段名与算法。
     */
    @Test
    void shouldThrowWhenEncryptFailedAndFailFast() {
        EncryptHelper.init(validProperties());
        FieldEncrypt fieldEncrypt = fieldEncrypt(InvalidKeyEntity.class, "phone");
        assertThatThrownBy(() -> EncryptHelper.encrypt(PHONE, fieldEncrypt, "phone"))
            .isInstanceOf(FieldEncryptException.class)
            .hasMessageContaining("加密")
            .hasMessageContaining("phone")
            .hasMessageContaining("AES");
    }

    /**
     * fail-fast（默认）模式下，解密失败应抛异常且信息包含字段名与算法。
     */
    @Test
    void shouldThrowWhenDecryptFailedAndFailFast() {
        EncryptHelper.init(validProperties());
        FieldEncrypt fieldEncrypt = fieldEncrypt(InvalidKeyEntity.class, "phone");
        assertThatThrownBy(() -> EncryptHelper.decrypt("ciphertext", fieldEncrypt, "phone"))
            .isInstanceOf(FieldEncryptException.class)
            .hasMessageContaining("解密")
            .hasMessageContaining("phone")
            .hasMessageContaining("AES");
    }

    /**
     * 全局密钥缺失（null）触发加密器内部异常时同样应抛出。
     */
    @Test
    void shouldThrowWhenGlobalPasswordMissingAndFailFast() {
        FieldEncryptProperties properties = validProperties();
        properties.setPassword(null);
        EncryptHelper.init(properties);
        FieldEncrypt fieldEncrypt = fieldEncrypt(DefaultEntity.class, "phone");
        assertThatThrownBy(() -> EncryptHelper.encrypt(PHONE, fieldEncrypt, "phone"))
            .isInstanceOf(FieldEncryptException.class);
    }

    /**
     * fail-fast=false 时，加密失败应降级返回原值且不抛异常。
     */
    @Test
    void shouldReturnOriginalValueWhenEncryptFailedAndFailFastDisabled() {
        FieldEncryptProperties properties = validProperties();
        properties.setFailFast(Boolean.FALSE);
        EncryptHelper.init(properties);
        FieldEncrypt fieldEncrypt = fieldEncrypt(InvalidKeyEntity.class, "phone");
        assertThat(EncryptHelper.encrypt(PHONE, fieldEncrypt, "phone")).isEqualTo(PHONE);
    }

    /**
     * fail-fast=false 时，解密失败应降级返回密文且不抛异常。
     */
    @Test
    void shouldReturnOriginalValueWhenDecryptFailedAndFailFastDisabled() {
        FieldEncryptProperties properties = validProperties();
        properties.setFailFast(Boolean.FALSE);
        EncryptHelper.init(properties);
        FieldEncrypt fieldEncrypt = fieldEncrypt(InvalidKeyEntity.class, "phone");
        String ciphertext = "ciphertext";
        assertThat(EncryptHelper.decrypt(ciphertext, fieldEncrypt, "phone")).isEqualTo(ciphertext);
    }

    /**
     * 编程式加解密入口（无注解参数）在 fail-fast 模式下失败同样应抛异常。
     */
    @Test
    void shouldThrowWhenGlobalEncryptFailedAndFailFast() {
        FieldEncryptProperties properties = validProperties();
        properties.setPassword(null);
        EncryptHelper.init(properties);
        assertThatThrownBy(() -> EncryptHelper.encrypt(PHONE))
            .isInstanceOf(FieldEncryptException.class);
    }

    private FieldEncryptProperties validProperties() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setPassword(VALID_PASSWORD);
        return properties;
    }

    private FieldEncrypt fieldEncrypt(Class<?> entityClass, String fieldName) {
        return ReflectUtil.getField(entityClass, fieldName).getAnnotation(FieldEncrypt.class);
    }
}
