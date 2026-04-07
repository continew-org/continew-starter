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

package top.continew.starter.auth.openapi.interceptor;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.continew.starter.auth.openapi.annotation.OpenApi;
import top.continew.starter.auth.openapi.autoconfigure.OpenApiProperties;
import top.continew.starter.auth.openapi.enums.SignAlgorithm;
import top.continew.starter.auth.openapi.exception.OpenApiException;
import top.continew.starter.auth.openapi.model.OpenApiApp;
import top.continew.starter.auth.openapi.dao.OpenApiAppDao;
import top.continew.starter.auth.openapi.verifier.SignVerifier;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 开放 API 签名验证拦截器
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class OpenApiInterceptor implements HandlerInterceptor {

    private final OpenApiProperties properties;

    public OpenApiInterceptor(OpenApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        OpenApi annotation = handlerMethod.getMethodAnnotation(OpenApi.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(OpenApi.class);
        }
        if (annotation == null || !annotation.value()) {
            return true;
        }
        OpenApiAppDao appDao = SpringUtil.getBean(OpenApiAppDao.class);
        String appId = request.getParameter(properties.getAppIdParamName());
        String sign = request.getParameter(properties.getSignParamName());
        String timestampStr = request.getParameter(properties.getTimestampParamName());
        String nonce = request.getParameter(properties.getNonceParamName());
        if (appId == null || appId.isEmpty()) {
            throw new OpenApiException("缺少 appId 参数");
        }
        if (sign == null || sign.isEmpty()) {
            throw new OpenApiException("缺少 sign 参数");
        }
        OpenApiApp app = appDao.getByAppId(appId);
        if (app == null) {
            throw new OpenApiException("应用不存在");
        }
        if (app.getStatus() == null || app.getStatus() != 1) {
            throw new OpenApiException("应用已禁用");
        }
        Long timestamp = null;
        if (timestampStr != null && !timestampStr.isEmpty()) {
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                throw new OpenApiException("时间戳格式错误");
            }
        }
        if (properties.isNonceEnabled() && nonce != null && !nonce.isEmpty()) {
            if (appDao.isNonceUsed(nonce, appId, properties.getTimestampExpire())) {
                throw new OpenApiException("请求已被使用，请勿重复提交");
            }
        }
        Map<String, String> params = extractParams(request);
        SignAlgorithm algorithm = resolveAlgorithm(app.getSignAlgorithm());
        boolean verified = SignVerifier.verify(params, sign, app, algorithm, timestamp, nonce, properties
            .getTimestampExpire());
        if (!verified) {
            throw new OpenApiException("签名验证失败");
        }
        if (properties.isNonceEnabled() && nonce != null && !nonce.isEmpty()) {
            appDao.recordNonce(nonce, appId, properties.getTimestampExpire());
        }
        request.setAttribute(OpenApiConstants.REQUEST_ATTR_APP_ID, appId);
        request.setAttribute(OpenApiConstants.REQUEST_ATTR_APP, app);
        return true;
    }

    /**
     * 提取请求参数
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (properties.getSignParamName().equals(paramName)) {
                continue;
            }
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }
        return params;
    }

    /**
     * 解析签名算法
     */
    private SignAlgorithm resolveAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return properties.getDefaultAlgorithm();
        }
        try {
            return SignAlgorithm.valueOf(algorithm.toUpperCase());
        } catch (IllegalArgumentException e) {
            return properties.getDefaultAlgorithm();
        }
    }
}
