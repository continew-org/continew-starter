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

package top.continew.starter.encrypt.field.autoconfigure;

import cn.hutool.core.text.CharSequenceUtil;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.encrypt.enums.Algorithm;
import top.continew.starter.encrypt.field.exception.FieldEncryptException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 字段加密配置校验器
 *
 * <p>对称加密算法（AES、DES、PBE_WITH_MD5_AND_DES）依赖全局密钥，密钥缺失或长度非法时
 * 任何加密调用都会失败，必须在启动期暴露而非等待首次写入时静默降级。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class FieldEncryptPropertiesValidator {

    /**
     * AES 密钥合法字节数
     */
    private static final List<Integer> AES_KEY_LENGTHS = List.of(16, 24, 32);

    /**
     * DES 密钥合法字节数
     */
    private static final int DES_KEY_LENGTH = 8;

    /**
     * 对称加密算法密钥配置项
     */
    private static final String PASSWORD_KEY = PropertiesConstants.ENCRYPT_FIELD
        + StringConstants.DOT + "password";

    private FieldEncryptPropertiesValidator() {
    }

    /**
     * 校验字段加密全局配置
     *
     * @param properties 字段加密配置属性
     * @throws FieldEncryptException 默认算法非法，或对称算法密钥缺失、长度非法
     */
    public static void validate(FieldEncryptProperties properties) {
        Algorithm algorithm = properties.getAlgorithm();
        if (algorithm == Algorithm.DEFAULT) {
            throw new FieldEncryptException(String
                .format("算法配置非法：%s 仅用于注解占位，请为 %s%salgorithm 指定具体算法", Algorithm.DEFAULT,
                    PropertiesConstants.ENCRYPT_FIELD, StringConstants.DOT));
        }
        if (isKeyNotRequired(algorithm)) {
            return;
        }
        String password = properties.getPassword();
        if (CharSequenceUtil.isBlank(password)) {
            throw new FieldEncryptException(String
                .format("未配置 %s，无法使用对称加密算法 %s", PASSWORD_KEY, algorithm));
        }
        int length = password.getBytes(StandardCharsets.UTF_8).length;
        if (algorithm == Algorithm.AES && !AES_KEY_LENGTHS.contains(length)) {
            throw new FieldEncryptException(String.format("%s 密钥长度非法：算法 %s 期望 %s 字节，实际 %d 字节",
                PASSWORD_KEY,
                algorithm,
                joinLengths(AES_KEY_LENGTHS),
                length));
        }
        if (algorithm == Algorithm.DES && length != DES_KEY_LENGTH) {
            throw new FieldEncryptException(String.format("%s 密钥长度非法：算法 %s 期望 %d 字节，实际 %d 字节",
                PASSWORD_KEY,
                algorithm,
                DES_KEY_LENGTH,
                length));
        }
        // PBE_WITH_MD5_AND_DES 由口令派生密钥，不校验固定长度
    }

    /**
     * 判断算法是否不依赖对称密钥（非对称算法与编码算法）
     *
     * @param algorithm 加密算法
     * @return 是否不依赖对称密钥
     */
    private static boolean isKeyNotRequired(Algorithm algorithm) {
        return algorithm == Algorithm.RSA || algorithm == Algorithm.BASE64;
    }

    /**
     * 拼接合法字节数列表为可读文本
     *
     * @param lengths 合法字节数列表
     * @return 形如 16/24/32 的文本
     */
    private static String joinLengths(List<Integer> lengths) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lengths.size(); i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(lengths.get(i));
        }
        return builder.toString();
    }
}
