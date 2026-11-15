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

package top.continew.starter.security.xss.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.security.xss.autoconfigure.XssProperties;

import java.util.List;

/**
 * {@link XssProperties} 与 {@link XssMode} 单元测试：默认值与读写。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class XssPropertiesTest {

    /**
     * XSS 防护应默认开启、模式为清理、匹配列表为空。
     */
    @Test
    @DisplayName("Xss 默认值")
    void shouldDefaultEnabledCleanAndEmptyPatterns() {
        XssProperties properties = new XssProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getMode()).isEqualTo(XssMode.CLEAN);
        assertThat(properties.getIncludePatterns()).isEmpty();
        assertThat(properties.getExcludePatterns()).isEmpty();
    }

    /**
     * 各属性应可读写往返。
     */
    @Test
    @DisplayName("Xss 属性读写往返")
    void shouldSetAndGetProperties() {
        XssProperties properties = new XssProperties();
        properties.setEnabled(false);
        properties.setMode(XssMode.ESCAPE);
        properties.setIncludePatterns(List.of("/api/**"));
        properties.setExcludePatterns(List.of("/api/open/**"));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getMode()).isEqualTo(XssMode.ESCAPE);
        assertThat(properties.getIncludePatterns()).containsExactly("/api/**");
        assertThat(properties.getExcludePatterns()).containsExactly("/api/open/**");
    }

    /**
     * XssMode 应仅含 CLEAN 与 ESCAPE 两种模式。
     */
    @Test
    @DisplayName("XssMode 枚举面")
    void shouldContainTwoModes() {
        assertThat(XssMode.values()).containsExactly(XssMode.CLEAN, XssMode.ESCAPE);
    }
}
