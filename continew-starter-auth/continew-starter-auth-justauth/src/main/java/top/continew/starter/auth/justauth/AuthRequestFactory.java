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

package top.continew.starter.auth.justauth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.xkcoding.http.config.HttpConfig;
import me.zhyd.oauth.AuthRequestBuilder;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthDefaultSource;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.enums.AuthResponseStatus;
import me.zhyd.oauth.exception.AuthException;
import top.continew.starter.auth.justauth.autoconfigure.JustAuthExtendProperties;
import top.continew.starter.auth.justauth.autoconfigure.JustAuthHttpProperties;
import top.continew.starter.auth.justauth.autoconfigure.JustAuthProperties;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthRequest 工厂类
 *
 * @author <a href="https://gitee.com/justauth/justauth-spring-boot-starter">yangkai.shen</a>
 * @author Charles7c
 * @since 2.15.0
 */
public class AuthRequestFactory {

    private final JustAuthProperties properties;
    private final AuthStateCache stateCache;

    public AuthRequestFactory(JustAuthProperties properties, AuthStateCache stateCache) {
        this.properties = properties;
        this.stateCache = stateCache;
    }

    /**
     * 获取 AuthRequest
     *
     * @param source {@link AuthSource}
     * @return {@link AuthRequest}
     */
    public AuthRequest getAuthRequest(String source) {
        if (StrUtil.isBlank(source)) {
            throw new AuthException(AuthResponseStatus.NO_AUTH_SOURCE);
        }

        // 获取内置 AuthRequest
        AuthRequest authRequest = this.getDefaultAuthRequest(source);

        // 获取自定义 AuthRequest
        if (authRequest == null) {
            authRequest = this.getExtendAuthRequest(properties.getExtend().getEnumClass(), source);
        }

        if (authRequest == null) {
            throw new AuthException(AuthResponseStatus.UNSUPPORTED);
        }
        return authRequest;
    }

    /**
     * 获取自定义 AuthRequest
     *
     * @param clazz  枚举类 {@link AuthSource}
     * @param source {@link AuthSource}
     * @return {@link AuthRequest}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private AuthRequest getExtendAuthRequest(Class clazz, String source) {
        String upperSource = source.toUpperCase();
        try {
            EnumUtil.fromString(clazz, upperSource);
        } catch (IllegalArgumentException e) {
            // 无自定义匹配
            return null;
        }

        Map<String, JustAuthExtendProperties.ExtendRequestConfig> extendConfig =
            properties.getExtend().getConfig();
        Map<String, JustAuthExtendProperties.ExtendRequestConfig> upperConfig = new HashMap<>(6);
        extendConfig.forEach((k, v) -> upperConfig.put(k.toUpperCase(), v));
        JustAuthExtendProperties.ExtendRequestConfig extendRequestConfig =
            upperConfig.get(upperSource);
        if (extendRequestConfig != null) {
            // 配置 HTTP
            this.configureHttpConfig(upperSource, extendRequestConfig, properties.getHttp());

            Class<? extends AuthRequest> requestClass = extendRequestConfig.getRequestClass();

            if (requestClass != null) {
                // 反射获取 Request 对象，所以必须实现 2 个参数的构造方法
                return ReflectUtil.newInstance(requestClass, extendRequestConfig, stateCache);
            }
        }
        return null;
    }

    /**
     * 获取内置 AuthRequest
     *
     * @param source {@link AuthSource}
     * @return {@link AuthRequest}
     */
    private AuthRequest getDefaultAuthRequest(String source) {
        AuthDefaultSource authSource;
        try {
            authSource = EnumUtil.fromString(AuthDefaultSource.class, source.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 无自定义匹配
            return null;
        }

        AuthConfig config = properties.getType().get(authSource.name());
        // 找不到对应关系，直接返回空
        if (config == null) {
            return null;
        }

        // 配置 HTTP
        this.configureHttpConfig(authSource.name(), config, properties.getHttp());
        return AuthRequestBuilder.builder().source(source).authConfig(config).build();
    }

    /**
     * 配置 HTTP 相关的配置
     *
     * @param authSource {@link AuthSource}
     * @param authConfig {@link AuthConfig}
     * @param httpConfig {@link JustAuthHttpProperties}
     */
    private void configureHttpConfig(String authSource, AuthConfig authConfig,
        JustAuthHttpProperties httpConfig) {
        if (null == httpConfig) {
            return;
        }

        Map<String, JustAuthHttpProperties.JustAuthProxyConfig> proxyConfigMap =
            httpConfig.getProxy();
        if (CollUtil.isEmpty(proxyConfigMap)) {
            return;
        }

        JustAuthHttpProperties.JustAuthProxyConfig proxyConfig = proxyConfigMap.get(authSource);
        if (null == proxyConfig) {
            return;
        }

        authConfig.setHttpConfig(HttpConfig.builder()
            .timeout(httpConfig.getTimeout())
            .proxy(new Proxy(Proxy.Type.valueOf(proxyConfig.getType()),
                new InetSocketAddress(proxyConfig
                    .getHostname(), proxyConfig.getPort())))
            .build());
    }
}
