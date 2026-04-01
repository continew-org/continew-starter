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

package top.continew.starter.auth.openapi.util;

import cn.hutool.core.util.IdUtil;
import top.continew.starter.auth.openapi.enums.SignAlgorithm;
import top.continew.starter.auth.openapi.interceptor.OpenApiConstants;
import top.continew.starter.auth.openapi.model.OpenApiApp;
import top.continew.starter.auth.openapi.signer.Signer;
import top.continew.starter.core.util.ServletUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 开放 API 工具类
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class OpenApiHelper {

    private OpenApiHelper() {
    }

    /**
     * 获取当前请求的应用 ID
     *
     * @return 应用 ID
     */
    public static String getAppId() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return null;
        }
        Object appId = request.getAttribute(OpenApiConstants.REQUEST_ATTR_APP_ID);
        return appId != null ? appId.toString() : null;
    }

    /**
     * 获取当前请求的应用信息
     *
     * @return 应用信息
     */
    public static OpenApiApp getApp() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return null;
        }
        Object app = request.getAttribute(OpenApiConstants.REQUEST_ATTR_APP);
        return app instanceof OpenApiApp ? (OpenApiApp)app : null;
    }

    /**
     * 生成签名参数
     *
     * @param params    业务参数
     * @param appId     应用 ID
     * @param appSecret 应用密钥
     * @param algorithm 签名算法
     * @return 完整的请求参数（包含签名相关参数）
     */
    public static Map<String, String> generateSignParams(Map<String, String> params,
                                                         String appId,
                                                         String appSecret,
                                                         SignAlgorithm algorithm) {
        return generateSignParams(params, appId, appSecret, algorithm, true);
    }

    /**
     * 生成签名参数
     *
     * @param params       业务参数
     * @param appId        应用 ID
     * @param appSecret    应用密钥
     * @param algorithm    签名算法
     * @param includeNonce 是否包含 nonce
     * @return 完整的请求参数（包含签名相关参数）
     */
    public static Map<String, String> generateSignParams(Map<String, String> params,
                                                         String appId,
                                                         String appSecret,
                                                         SignAlgorithm algorithm,
                                                         boolean includeNonce) {
        Map<String, String> allParams = new HashMap<>();
        if (params != null) {
            allParams.putAll(params);
        }
        allParams.put("appId", appId);
        allParams.put("timestamp", String.valueOf(System.currentTimeMillis()));
        if (includeNonce) {
            allParams.put("nonce", IdUtil.fastSimpleUUID());
        }
        String sign = Signer.sign(allParams, appSecret, algorithm);
        allParams.put("sign", sign);
        return allParams;
    }
}
