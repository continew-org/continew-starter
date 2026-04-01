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

package top.continew.starter.auth.openapi.signer;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import top.continew.starter.auth.openapi.enums.SignAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名器
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class Signer {

    private Signer() {
    }

    /**
     * 生成签名
     *
     * @param params    请求参数
     * @param appSecret 应用密钥
     * @param algorithm 签名算法
     * @return 签名值
     */
    public static String sign(Map<String, String> params, String appSecret, SignAlgorithm algorithm) {
        String signingContent = buildSigningContent(params);
        return doSign(signingContent, appSecret, algorithm);
    }

    /**
     * 构建待签名内容
     *
     * @param params 请求参数
     * @return 待签名内容
     */
    public static String buildSigningContent(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && !key.isEmpty() && value != null) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(key).append("=").append(value);
            }
        }
        return sb.toString();
    }

    /**
     * 执行签名
     *
     * @param content   待签名内容
     * @param appSecret 应用密钥
     * @param algorithm 签名算法
     * @return 签名值
     */
    private static String doSign(String content, String appSecret, SignAlgorithm algorithm) {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] secretBytes = appSecret.getBytes(StandardCharsets.UTF_8);
        switch (algorithm) {
            case MD5:
                return DigestUtil.md5Hex(contentBytes + appSecret);
            case SHA256:
                return DigestUtil.sha256Hex(contentBytes + appSecret);
            case HMAC_MD5:
                HMac hmacMd5 = new HMac(HmacAlgorithm.HmacMD5, secretBytes);
                return hmacMd5.digestHex(contentBytes);
            case HMAC_SHA256:
                HMac hmacSha256 = new HMac(HmacAlgorithm.HmacSHA256, secretBytes);
                return hmacSha256.digestHex(contentBytes);
            default:
                return DigestUtil.md5Hex(contentBytes + appSecret);
        }
    }
}
