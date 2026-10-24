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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * 链路追踪配置属性测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class TracePropertiesTest {

    /**
     * 默认配置项
     */
    @Test
    @DisplayName("默认配置项")
    void shouldBindDefaults() {
        TraceProperties properties = new TraceProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getTraceIdName()).isEqualTo("traceId");
        assertThat(properties.getTlog()).isNotNull();
        assertThat(properties.getTlog().isConfigured()).isFalse();
    }

    /**
     * 绑定链路追踪配置
     */
    @Test
    @DisplayName("绑定链路追踪配置")
    void shouldBindTraceProperties() {
        MockEnvironment environment =
            new MockEnvironment().withProperty(PropertiesConstants.TRACE + ".enabled", "true")
                .withProperty(PropertiesConstants.TRACE + ".trace-id-name", "X-Trace-Id");

        TraceProperties properties = Binder.get(environment)
            .bind(PropertiesConstants.TRACE, Bindable.of(TraceProperties.class))
            .get();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getTraceIdName()).isEqualTo("X-Trace-Id");
    }

    /**
     * 绑定并识别旧版 tlog 配置
     */
    @Test
    @DisplayName("绑定并识别旧版 tlog 配置")
    void shouldBindLegacyTlogProperties() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(PropertiesConstants.TRACE + ".tlog.pattern", "[$spanId][$traceId]")
            .withProperty(PropertiesConstants.TRACE + ".tlog.mdc-enable", "true");

        TraceProperties properties = Binder.get(environment)
            .bind(PropertiesConstants.TRACE, Bindable.of(TraceProperties.class))
            .get();

        assertThat(properties.getTlog()).isNotNull();
        assertThat(properties.getTlog().isConfigured()).isTrue();
        assertThat(properties.getTlog().getPattern()).isEqualTo("[$spanId][$traceId]");
        assertThat(properties.getTlog().getMdcEnable()).isTrue();
    }
}
