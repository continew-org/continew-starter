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

import static org.assertj.core.api.Assertions.assertThat;

import com.xkcoding.http.config.HttpConfig;
import me.zhyd.oauth.cache.AuthDefaultStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.continew.starter.auth.justauth.autoconfigure.JustAuthHttpProperties;
import top.continew.starter.auth.justauth.autoconfigure.JustAuthProperties;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

/**
 * AuthRequest 工厂单元测试。
 *
 * <p>覆盖按 source 配置 HTTP 代理的分支：代理映射为空、source 无对应代理、代理配置生效。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("AuthRequestFactory 单元测试")
class AuthRequestFactoryTest {

    private static final String SOURCE = "github";

    @Test
    @DisplayName("未配置 HTTP 时不设置 HttpConfig")
    void shouldSkipHttpConfigWhenHttpPropertiesMissing() {
        JustAuthProperties properties = propertiesWithGithubConfig(null);
        AuthConfig config = requestConfig(properties);

        assertThat(config.getHttpConfig()).isNull();
    }

    @Test
    @DisplayName("代理映射为空时不设置 HttpConfig")
    void shouldSkipHttpConfigWhenProxyMapEmpty() {
        JustAuthHttpProperties http = new JustAuthHttpProperties();
        http.setTimeout(3000);
        AuthConfig config = requestConfig(propertiesWithGithubConfig(http));

        assertThat(config.getHttpConfig()).isNull();
    }

    @Test
    @DisplayName("source 无对应代理配置时不设置 HttpConfig")
    void shouldSkipHttpConfigWhenNoProxyForSource() {
        JustAuthHttpProperties http = new JustAuthHttpProperties();
        http.setTimeout(3000);
        http.setProxy(Map.of("gitee", proxyConfig("HTTP", "127.0.0.1", 8080)));
        AuthConfig config = requestConfig(propertiesWithGithubConfig(http));

        assertThat(config.getHttpConfig()).isNull();
    }

    @Test
    @DisplayName("按 source 设置带代理的 HttpConfig")
    void shouldConfigureHttpWithProxyForSource() {
        JustAuthHttpProperties http = new JustAuthHttpProperties();
        http.setTimeout(5000);
        http.setProxy(Map.of("GITHUB", proxyConfig("HTTP", "proxy.example.com", 3128)));
        AuthConfig config = requestConfig(propertiesWithGithubConfig(http));

        HttpConfig httpConfig = config.getHttpConfig();
        assertThat(httpConfig).isNotNull();
        assertThat(httpConfig.getTimeout()).isEqualTo(5000);
        assertThat(httpConfig.getProxy()).isEqualTo(new Proxy(Proxy.Type.HTTP,
            new InetSocketAddress("proxy.example.com", 3128)));
    }

    private static AuthConfig requestConfig(JustAuthProperties properties) {
        AuthRequestFactory factory =
            new AuthRequestFactory(properties, AuthDefaultStateCache.INSTANCE);
        AuthRequest request = factory.getAuthRequest(SOURCE);
        return (AuthConfig) ReflectionTestUtils.getField(request, "config");
    }

    private static JustAuthProperties propertiesWithGithubConfig(JustAuthHttpProperties http) {
        JustAuthProperties properties = new JustAuthProperties();
        properties.setType(Map.of("GITHUB", AuthConfig.builder()
            .clientId("client-id")
            .clientSecret("client-secret")
            .redirectUri("http://localhost/callback")
            .build()));
        properties.setHttp(http);
        return properties;
    }

    private static JustAuthHttpProperties.JustAuthProxyConfig proxyConfig(String type,
        String hostname, int port) {
        JustAuthHttpProperties.JustAuthProxyConfig proxy =
            new JustAuthHttpProperties.JustAuthProxyConfig();
        proxy.setType(type);
        proxy.setHostname(hostname);
        proxy.setPort(port);
        return proxy;
    }
}
