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

package top.continew.starter.auth.openapi.verifier;

import top.continew.starter.auth.openapi.enums.SignAlgorithm;
import top.continew.starter.auth.openapi.exception.OpenApiException;
import top.continew.starter.auth.openapi.model.OpenApiApp;
import top.continew.starter.auth.openapi.signer.Signer;

import java.util.Map;

/**
 * 签名验证器
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class SignVerifier {

    private SignVerifier() {
    }

    /**
     * 验证签名
     *
     * @param params          请求参数
     * @param sign            待验证的签名
     * @param app             应用信息
     * @param algorithm       签名算法
     * @param timestamp       时间戳
     * @param nonce           随机字符串
     * @param timestampExpire 时间戳过期时间（毫秒）
     * @return 验证是否通过
     */
    public static boolean verify(Map<String, String> params,
                                 String sign,
                                 OpenApiApp app,
                                 SignAlgorithm algorithm,
                                 Long timestamp,
                                 String nonce,
                                 Long timestampExpire) {
        if (sign == null || sign.isEmpty()) {
            throw new OpenApiException("签名不能为空");
        }
        if (app == null) {
            throw new OpenApiException("应用不存在");
        }
        if (timestampExpire != null && timestampExpire > 0) {
            if (timestamp == null) {
                throw new OpenApiException("时间戳不能为空");
            }
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - timestamp) > timestampExpire) {
                throw new OpenApiException("请求已过期");
            }
        }
        String expectedSign = Signer.sign(params, app.getAppSecret(), algorithm);
        return expectedSign.equalsIgnoreCase(sign);
    }

    /**
     * 验证签名（简化版）
     *
     * @param params    请求参数
     * @param sign      待验证的签名
     * @param appSecret 应用密钥
     * @param algorithm 签名算法
     * @return 验证是否通过
     */
    public static boolean verify(Map<String, String> params, String sign, String appSecret, SignAlgorithm algorithm) {
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        String expectedSign = Signer.sign(params, appSecret, algorithm);
        return expectedSign.equalsIgnoreCase(sign);
    }
}
