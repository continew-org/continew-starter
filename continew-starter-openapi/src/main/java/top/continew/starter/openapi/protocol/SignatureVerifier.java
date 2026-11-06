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

package top.continew.starter.openapi.protocol;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 签名验算（HMAC-SHA256 + 常量时间比较）
 *
 * <p>比较使用 {@link MessageDigest#isEqual(byte[], byte[])}，对签名 hex 解码后的
 * 32 字节比较，避免时序侧信道。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class SignatureVerifier {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private SignatureVerifier() {
    }

    /**
     * 验算签名
     *
     * @param secretKey    应用密钥
     * @param stringToSign 待签串
     * @param signatureHex 请求携带的签名（64 位 hex，合法性由头解析阶段保证）
     * @return 签名匹配时返回 true
     */
    public static boolean verify(String secretKey, String stringToSign, String signatureHex) {
        byte[] expected = hmacSha256(secretKey, stringToSign);
        byte[] actual;
        try {
            actual = HexFormat.of().parseHex(signatureHex);
        } catch (IllegalArgumentException e) {
            // 签名不是合法 hex，直接判不匹配
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * 计算待签串的 HMAC-SHA256 摘要
     *
     * @param secretKey    应用密钥
     * @param stringToSign 待签串
     * @return 摘要字节
     */
    public static byte[] hmacSha256(String secretKey, String stringToSign) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 初始化失败", e);
        }
    }
}
