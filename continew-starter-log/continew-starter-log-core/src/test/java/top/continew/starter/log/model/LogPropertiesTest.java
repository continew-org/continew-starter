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

package top.continew.starter.log.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.log.enums.Include;

import java.util.List;
import java.util.Set;

/**
 * {@link LogProperties} 配置属性测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LogPropertiesTest {

    /**
     * 默认启用、包含信息为默认集、无放行路由且嵌套访问日志配置已初始化
     */
    @Test
    @DisplayName("默认配置：启用 + 默认包含集 + 空放行路由")
    void propertiesShouldDeclareSensibleDefaults() {
        LogProperties properties = new LogProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getIncludes()).isEqualTo(Include.defaultIncludes());
        assertThat(properties.getExcludePatterns()).isEmpty();
        assertThat(properties.getAccessLog()).isNotNull();
    }

    /**
     * 配置的放行路由模式应命中匹配 URI
     */
    @Test
    @DisplayName("放行路由命中匹配 URI")
    void excludeUriShouldMatchConfiguredPattern() {
        LogProperties properties = new LogProperties();
        properties.setExcludePatterns(List.of("/api/**"));
        assertThat(properties.isMatchExcludeUri("/api/users")).isTrue();
    }

    /**
     * 不匹配放行路由的 URI 应返回不匹配
     */
    @Test
    @DisplayName("不匹配放行路由的 URI 返回 false")
    void excludeUriShouldNotMatchUnrelatedUri() {
        LogProperties properties = new LogProperties();
        properties.setExcludePatterns(List.of("/api/**"));
        assertThat(properties.isMatchExcludeUri("/other/users")).isFalse();
    }

    /**
     * 未配置放行路由时任何 URI 都不匹配
     */
    @Test
    @DisplayName("未配置放行路由时返回 false")
    void excludeUriShouldNotMatchWithoutPatterns() {
        assertThat(new LogProperties().isMatchExcludeUri("/api/users")).isFalse();
    }

    /**
     * 启用开关与包含信息可设置
     */
    @Test
    @DisplayName("启用开关与包含信息可设置")
    void enabledAndIncludesShouldBeSettable() {
        LogProperties properties = new LogProperties();
        properties.setEnabled(false);
        properties.setIncludes(Set.of(Include.REQUEST_BODY));
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getIncludes()).containsExactly(Include.REQUEST_BODY);
    }
}
