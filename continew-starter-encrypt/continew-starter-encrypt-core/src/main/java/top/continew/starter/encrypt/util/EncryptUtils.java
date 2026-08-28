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

package top.continew.starter.encrypt.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 *
 * @author Charles7c
 * @since 2.14.0
 */
public class EncryptUtils {

    /**
     * Base64 编码
     *
     * @param data 待编码数据
     * @return 编码后字符串
     * @author lishuyan
     */
    public static String encodeByBase64(String data) {
        return Base64.encode(data, StandardCharsets.UTF_8);
    }

    /**
     * Base64 解码
     *
     * @param data 待解码数据
     * @return 解码后字符串
     * @author lishuyan
     */
    public static String decodeByBase64(String data) {
        return Base64.decodeStr(data, StandardCharsets.UTF_8);
    }

    /**
     * AES 加密
     *
     * @param data     待加密数据
     * @param password 秘钥字符串
     * @return 加密后字符串, 采用 Base64 编码
     * @author lishuyan
     */
    public static String encryptByAes(String data, String password) {
        if (CharSequenceUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES需要传入秘钥信息");
        }
        // AES算法的秘钥要求是16位、24位、32位
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度要求为16位、24位、32位");
        }
        return SecureUtil.aes(password.getBytes(StandardCharsets.UTF_8)).encryptBase64(data,
            StandardCharsets.UTF_8);
    }

    /**
     * AES 解密
     *
     * @param data     待解密数据
     * @param password 秘钥字符串
     * @return 解密后字符串
     * @author lishuyan
     */
    public static String decryptByAes(String data, String password) {
        if (CharSequenceUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES需要传入秘钥信息");
        }
        // AES算法的秘钥要求是16位、24位、32位
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度要求为16位、24位、32位");
        }
        return SecureUtil.aes(password.getBytes(StandardCharsets.UTF_8)).decryptStr(data,
            StandardCharsets.UTF_8);
    }

    /**
     * RSA 公钥加密
     *
     * @param data      待加密数据
     * @param publicKey 公钥
     * @return 加密后字符串, 采用Base64编码
     * @author lishuyan
     */
    public static String encryptByRsa(String data, String publicKey) {
        if (CharSequenceUtil.isBlank(publicKey)) {
            throw new IllegalArgumentException("RSA需要传入公钥进行加密");
        }
        RSA rsa = SecureUtil.rsa(null, publicKey);
        return rsa.encryptBase64(data, StandardCharsets.UTF_8, KeyType.PublicKey);
    }

    /**
     * RSA 私钥解密
     *
     * @param data       待解密数据
     * @param privateKey 私钥
     * @return 解密后字符串
     * @author lishuyan
     */
    public static String decryptByRsa(String data, String privateKey) {
        if (CharSequenceUtil.isBlank(privateKey)) {
            throw new IllegalArgumentException("RSA需要传入私钥进行解密");
        }
        RSA rsa = SecureUtil.rsa(privateKey, null);
        return rsa.decryptStr(data, KeyType.PrivateKey, StandardCharsets.UTF_8);
    }

    private EncryptUtils() {
    }
}
