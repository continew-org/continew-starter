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

package top.continew.starter.web.autoconfigure;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 消息转换器默认配置
 * <p>
 * Spring Boot 4 默认使用 Jackson 3，本项目在 3.0.0 过渡期将 HTTP 层的 JSON 转换器锚定到 Jackson 2。
 * 此处以最低优先级写入默认值，应用可通过同名配置覆盖。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class Jackson2PreferenceEnvironmentPostProcessor implements EnvironmentPostProcessor,
    Ordered {

    private static final String PROPERTY_SOURCE_NAME = "continew-starter-web-defaults";
    private static final String PREFERRED_JSON_MAPPER_PROPERTY =
        "spring.http.converters.preferred-json-mapper";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
        SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Map<String, Object> defaults = new HashMap<>(1);
        defaults.put(PREFERRED_JSON_MAPPER_PROPERTY, "jackson2");
        environment.getPropertySources()
            .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
