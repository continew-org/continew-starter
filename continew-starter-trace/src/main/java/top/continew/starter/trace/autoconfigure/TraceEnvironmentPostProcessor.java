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

package top.continew.starter.trace.autoconfigure;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.constant.StringConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * 链路追踪环境后置处理器
 *
 * <p>
 * 启用链路追踪且使用方未显式配置采样率时，将 Micrometer Tracing 采样率默认补为 1.0。
 * Micrometer Tracing 默认采样率为 0.1，无上报后端的场景下会导致九成请求没有链路日志。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class TraceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * Micrometer Tracing 采样率配置项
     */
    public static final String SAMPLING_PROBABILITY = "management.tracing.sampling.probability";

    private static final String PROPERTY_SOURCE_NAME = "continewStarterTraceDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
        SpringApplication application) {
        String enabledKey =
            PropertiesConstants.TRACE + StringConstants.DOT + PropertiesConstants.ENABLED;
        boolean enabled =
            Boolean.TRUE.equals(environment.getProperty(enabledKey, Boolean.class, false));
        if (!enabled) {
            return;
        }
        if (environment.getProperty(SAMPLING_PROBABILITY) != null) {
            return;
        }
        Map<String, Object> defaults = new HashMap<>(1);
        defaults.put(SAMPLING_PROBABILITY, 1.0F);
        environment.getPropertySources()
            .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
