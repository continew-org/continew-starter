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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.encrypt.context.CryptoContext;
import top.continew.starter.encrypt.encryptor.IEncryptor;
import top.continew.starter.encrypt.enums.Algorithm;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;
import top.continew.starter.encrypt.field.autoconfigure.FieldEncryptProperties;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密助手
 *
 * @author lishuyan
 * @since 2.13.2
 */
public class EncryptHelper {

    private static final Logger log = LoggerFactory.getLogger(EncryptHelper.class);

    /**
     * 默认加密配置
     */
    private static FieldEncryptProperties defaultProperties;

    /**
     * 加密器缓存
     */
    private static final Map<Integer, IEncryptor> ENCRYPTOR_CACHE = new ConcurrentHashMap<>();

    private EncryptHelper() {
    }

    /**
     * 初始化默认配置
     *
     * @param properties 加密配置
     */
    public static void init(FieldEncryptProperties properties) {
        defaultProperties = properties;
    }

    /**
     * 注册加密执行者到缓存
     * <p>
     * 计算 CryptoContext 对象的hashCode作为缓存中的key，通过hashCode查询缓存中存在则直接返回，不存在则创建并缓存
     * </p>
     *
     * @param cryptoContext 加密执行者需要的相关配置参数
     * @return 加密执行者
     */
    public static IEncryptor registerAndGetEncryptor(CryptoContext cryptoContext) {
        int key = cryptoContext.hashCode();
        return ENCRYPTOR_CACHE.computeIfAbsent(key,
            k -> cryptoContext.getEncryptor().equals(IEncryptor.class)
                ? ReflectUtil.newInstance(cryptoContext.getAlgorithm().getEncryptor(),
                    cryptoContext)
                : ReflectUtil.newInstance(cryptoContext.getEncryptor(), cryptoContext));
    }

    /**
     * 获取字段上的 @FieldEncrypt 注解
     *
     * @param obj       对象
     * @param fieldName 字段名称
     * @return 字段上的 @FieldEncrypt 注解
     * @throws NoSuchFieldException /
     */
    public static FieldEncrypt getFieldEncrypt(Object obj, String fieldName)
        throws NoSuchFieldException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        return field.getAnnotation(FieldEncrypt.class);
    }

    /**
     * 加密方法
     *
     * @param value        待加密字符串
     * @param fieldEncrypt 待加密字段注解
     * @return 加密后的字符串
     */
    public static String encrypt(String value, FieldEncrypt fieldEncrypt) {
        if (CharSequenceUtil.isBlank(value) || fieldEncrypt == null) {
            return value;
        }
        String ciphertext = value;
        try {
            CryptoContext cryptoContext = buildCryptoContext(fieldEncrypt);
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            ciphertext = encryptor.encrypt(ciphertext);
        } catch (Exception e) {
            log.warn("加密失败，请检查加密配置，处理加密字段异常：{}", e.getMessage(), e);
        }
        return ciphertext;
    }

    /**
     * 加密方法
     *
     * @param value 待加密字符串
     * @return 加密后的字符串
     */
    public static String encrypt(String value) {
        if (CharSequenceUtil.isBlank(value)) {
            return value;
        }
        String ciphertext = value;
        try {
            CryptoContext cryptoContext = buildCryptoContext();
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            ciphertext = encryptor.encrypt(ciphertext);
        } catch (Exception e) {
            log.warn("加密失败，请检查加密配置，处理加密字段异常：{}", e.getMessage(), e);
        }
        return ciphertext;
    }

    /**
     * 解密方法
     *
     * @param value        待解密字符串
     * @param fieldEncrypt 待解密字段注解
     * @return 解密后的字符串
     */
    public static String decrypt(String value, FieldEncrypt fieldEncrypt) {
        if (CharSequenceUtil.isBlank(value) || fieldEncrypt == null) {
            return value;
        }
        String plaintext = value;
        try {
            CryptoContext cryptoContext = buildCryptoContext(fieldEncrypt);
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            plaintext = encryptor.decrypt(plaintext);
        } catch (Exception e) {
            log.warn("解密失败，请检查加密配置，处理解密字段异常：{}", e.getMessage(), e);
        }
        return plaintext;
    }

    /**
     * 解密方法
     *
     * @param value 待解密字符串
     * @return 解密后的字符串
     */
    public static String decrypt(String value) {
        if (CharSequenceUtil.isBlank(value)) {
            return value;
        }
        String plaintext = value;
        try {
            CryptoContext cryptoContext = buildCryptoContext();
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            plaintext = encryptor.decrypt(plaintext);
        } catch (Exception e) {
            log.warn("解密失败，请检查加密配置，处理解密字段异常：{}", e.getMessage(), e);
        }
        return plaintext;
    }

    /**
     * 构建加密上下文
     *
     * @param fieldEncrypt 字段加密注解
     * @return 加密上下文
     */
    private static CryptoContext buildCryptoContext(FieldEncrypt fieldEncrypt) {
        CryptoContext cryptoContext = new CryptoContext();
        cryptoContext.setAlgorithm(fieldEncrypt.value() == Algorithm.DEFAULT
            ? defaultProperties.getAlgorithm()
            : fieldEncrypt.value());
        cryptoContext.setEncryptor(fieldEncrypt.encryptor().equals(IEncryptor.class)
            ? IEncryptor.class
            : fieldEncrypt.encryptor());
        cryptoContext.setPassword(fieldEncrypt.password().isEmpty()
            ? defaultProperties.getPassword()
            : fieldEncrypt.password());
        cryptoContext.setPrivateKey(fieldEncrypt.privateKey().isEmpty()
            ? defaultProperties.getPrivateKey()
            : fieldEncrypt.privateKey());
        cryptoContext.setPublicKey(fieldEncrypt.publicKey().isEmpty()
            ? defaultProperties.getPublicKey()
            : fieldEncrypt.publicKey());
        return cryptoContext;
    }

    /**
     * 构建加密上下文
     *
     * @return 加密上下文
     */
    private static CryptoContext buildCryptoContext() {
        CryptoContext cryptoContext = new CryptoContext();
        cryptoContext.setAlgorithm(defaultProperties.getAlgorithm());
        cryptoContext.setEncryptor(IEncryptor.class);
        cryptoContext.setPassword(defaultProperties.getPassword());
        cryptoContext.setPrivateKey(defaultProperties.getPrivateKey());
        cryptoContext.setPublicKey(defaultProperties.getPublicKey());
        return cryptoContext;
    }
}
