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
import top.continew.starter.encrypt.field.exception.FieldEncryptException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密助手
 *
 * @author lishuyan
 * @since 2.13.2
 */
public class EncryptHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptHelper.class);

    /**
     * 加密配置快照（不可变），初始化时固化以隔离多上下文与运行期配置变更
     */
    private static volatile EncryptionSnapshot snapshot;

    /**
     * 加密器缓存：以加密上下文为键，上下文已实现字段级 equals 与 hashCode，精确匹配无碰撞
     */
    private static final Map<CryptoContext, IEncryptor> ENCRYPTOR_CACHE = new ConcurrentHashMap<>();

    private EncryptHelper() {
    }

    /**
     * 初始化默认配置
     *
     * @param properties 加密配置
     */
    public static void init(FieldEncryptProperties properties) {
        snapshot = new EncryptionSnapshot(Objects.requireNonNull(properties, "properties 不能为空"));
    }

    /**
     * 注册加密执行者到缓存
     *
     * @param cryptoContext 加密执行者需要的相关配置参数
     * @return 加密执行者
     */
    public static IEncryptor registerAndGetEncryptor(CryptoContext cryptoContext) {
        Class<? extends IEncryptor> encryptorClass =
            cryptoContext.getEncryptor().equals(IEncryptor.class)
                ? cryptoContext.getAlgorithm().getEncryptor()
                : cryptoContext.getEncryptor();
        return ENCRYPTOR_CACHE.computeIfAbsent(cryptoContext,
            k -> ReflectUtil.newInstance(encryptorClass, k));
    }

    /**
     * 加密方法
     *
     * @param value        待加密字符串
     * @param fieldEncrypt 待加密字段注解
     * @return 加密后的字符串
     */
    public static String encrypt(String value, FieldEncrypt fieldEncrypt) {
        return encrypt(value, fieldEncrypt, null);
    }

    /**
     * 加密方法
     *
     * @param value        待加密字符串
     * @param fieldEncrypt 待加密字段注解
     * @param fieldName    字段名称，用于失败时定位
     * @return 加密后的字符串
     * @throws FieldEncryptException 加密失败（fail-fast 模式下）
     * @since 3.1.0
     */
    public static String encrypt(String value, FieldEncrypt fieldEncrypt, String fieldName) {
        if (CharSequenceUtil.isBlank(value) || fieldEncrypt == null) {
            return value;
        }
        try {
            CryptoContext cryptoContext = buildCryptoContext(fieldEncrypt);
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            return encryptor.encrypt(value);
        } catch (Exception e) {
            failOrWarn("加密", fieldName, resolveAlgorithm(fieldEncrypt), e);
            return value;
        }
    }

    /**
     * 加密方法
     *
     * @param value 待加密字符串
     * @return 加密后的字符串
     * @throws FieldEncryptException 加密失败（fail-fast 模式下）
     */
    public static String encrypt(String value) {
        if (CharSequenceUtil.isBlank(value)) {
            return value;
        }
        try {
            CryptoContext cryptoContext = buildCryptoContext();
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            return encryptor.encrypt(value);
        } catch (Exception e) {
            failOrWarn("加密", null, resolveGlobalAlgorithm(), e);
            return value;
        }
    }

    /**
     * 解密方法
     *
     * @param value        待解密字符串
     * @param fieldEncrypt 待解密字段注解
     * @return 解密后的字符串
     */
    public static String decrypt(String value, FieldEncrypt fieldEncrypt) {
        return decrypt(value, fieldEncrypt, null);
    }

    /**
     * 解密方法
     *
     * @param value        待解密字符串
     * @param fieldEncrypt 待解密字段注解
     * @param fieldName    字段名称，用于失败时定位
     * @return 解密后的字符串
     * @throws FieldEncryptException 解密失败（fail-fast 模式下）
     * @since 3.1.0
     */
    public static String decrypt(String value, FieldEncrypt fieldEncrypt, String fieldName) {
        if (CharSequenceUtil.isBlank(value) || fieldEncrypt == null) {
            return value;
        }
        try {
            CryptoContext cryptoContext = buildCryptoContext(fieldEncrypt);
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            return encryptor.decrypt(value);
        } catch (Exception e) {
            failOrWarn("解密", fieldName, resolveAlgorithm(fieldEncrypt), e);
            return value;
        }
    }

    /**
     * 解密方法
     *
     * @param value 待解密字符串
     * @return 解密后的字符串
     * @throws FieldEncryptException 解密失败（fail-fast 模式下）
     */
    public static String decrypt(String value) {
        if (CharSequenceUtil.isBlank(value)) {
            return value;
        }
        try {
            CryptoContext cryptoContext = buildCryptoContext();
            IEncryptor encryptor = registerAndGetEncryptor(cryptoContext);
            return encryptor.decrypt(value);
        } catch (Exception e) {
            failOrWarn("解密", null, resolveGlobalAlgorithm(), e);
            return value;
        }
    }

    /**
     * 失败处理：fail-fast 模式抛出异常终止操作，降级模式仅记录警告
     *
     * @param action    动作描述（加密/解密）
     * @param fieldName 字段名称，可为空
     * @param algorithm 加密算法
     * @param e         原始异常
     */
    private static void failOrWarn(String action,
        String fieldName,
        Algorithm algorithm,
        Exception e) {
        String message = buildFailureMessage(action, fieldName, algorithm);
        if (isFailFast()) {
            throw new FieldEncryptException(message, e);
        }
        LOGGER.warn("{}，请检查加密配置：{}", message, e.getMessage(), e);
    }

    /**
     * 构建失败描述，包含字段名称与算法
     *
     * @param action    动作描述（加密/解密）
     * @param fieldName 字段名称，可为空
     * @param algorithm 加密算法
     * @return 失败描述
     */
    private static String buildFailureMessage(String action,
        String fieldName,
        Algorithm algorithm) {
        if (CharSequenceUtil.isBlank(fieldName)) {
            return String.format("%s失败，算法 [%s]", action, algorithm);
        }
        return String.format("%s失败，字段 [%s]，算法 [%s]", action, fieldName, algorithm);
    }

    /**
     * 判断是否 fail-fast 模式，未初始化配置时视为严格模式
     *
     * @return 是否 fail-fast 模式
     */
    private static boolean isFailFast() {
        return snapshot == null || snapshot.isFailFast();
    }

    /**
     * 解析注解实际使用的算法（DEFAULT 表示跟随全局配置）
     *
     * @param fieldEncrypt 字段加密注解
     * @return 加密算法
     */
    private static Algorithm resolveAlgorithm(FieldEncrypt fieldEncrypt) {
        Algorithm algorithm = fieldEncrypt.value();
        if (algorithm == Algorithm.DEFAULT && snapshot != null) {
            algorithm = snapshot.getAlgorithm();
        }
        return algorithm;
    }

    /**
     * 解析全局默认算法
     *
     * @return 加密算法
     */
    private static Algorithm resolveGlobalAlgorithm() {
        return snapshot != null ? snapshot.getAlgorithm() : null;
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
            ? snapshot.getAlgorithm()
            : fieldEncrypt.value());
        cryptoContext.setEncryptor(fieldEncrypt.encryptor().equals(IEncryptor.class)
            ? IEncryptor.class
            : fieldEncrypt.encryptor());
        cryptoContext.setPassword(fieldEncrypt.password().isEmpty()
            ? snapshot.getPassword()
            : fieldEncrypt.password());
        cryptoContext.setPrivateKey(fieldEncrypt.privateKey().isEmpty()
            ? snapshot.getPrivateKey()
            : fieldEncrypt.privateKey());
        cryptoContext.setPublicKey(fieldEncrypt.publicKey().isEmpty()
            ? snapshot.getPublicKey()
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
        cryptoContext.setAlgorithm(snapshot.getAlgorithm());
        cryptoContext.setEncryptor(IEncryptor.class);
        cryptoContext.setPassword(snapshot.getPassword());
        cryptoContext.setPrivateKey(snapshot.getPrivateKey());
        cryptoContext.setPublicKey(snapshot.getPublicKey());
        return cryptoContext;
    }

    /**
     * 加密配置快照，字段一经构造不可变更
     */
    private static final class EncryptionSnapshot {

        private final Algorithm algorithm;

        private final String password;

        private final String publicKey;

        private final String privateKey;

        private final boolean failFast;

        EncryptionSnapshot(FieldEncryptProperties properties) {
            this.algorithm = properties.getAlgorithm();
            this.password = properties.getPassword();
            this.publicKey = properties.getPublicKey();
            this.privateKey = properties.getPrivateKey();
            this.failFast = !Boolean.FALSE.equals(properties.getFailFast());
        }

        Algorithm getAlgorithm() {
            return algorithm;
        }

        String getPassword() {
            return password;
        }

        String getPublicKey() {
            return publicKey;
        }

        String getPrivateKey() {
            return privateKey;
        }

        boolean isFailFast() {
            return failFast;
        }
    }
}
