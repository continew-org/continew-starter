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

package top.continew.starter.security.sensitivewords.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 敏感词配置属性测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SensitiveWordsPropertiesTest {

    /**
     * 特殊检测开关默认关闭，避免默认承担误报成本
     */
    @Test
    @DisplayName("特殊检测开关默认关闭")
    void specialChecksDefaultToFalse() {
        SensitiveWordsProperties properties = new SensitiveWordsProperties();
        assertThat(properties.isIgnoreRepeat()).isFalse();
        assertThat(properties.isNumCheck()).isFalse();
        assertThat(properties.isEmailCheck()).isFalse();
        assertThat(properties.isUrlCheck()).isFalse();
    }

    /**
     * 基础开关默认启用
     */
    @Test
    @DisplayName("启用开关与内置词库默认启用")
    void enabledFlagsDefaultToTrue() {
        SensitiveWordsProperties properties = new SensitiveWordsProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isUseDefaultDeny()).isTrue();
    }

    /**
     * 特殊检测开关可双向设置
     */
    @Test
    @DisplayName("特殊检测开关 setter 双向传播")
    void specialChecksPropagateThroughSetters() {
        SensitiveWordsProperties properties = new SensitiveWordsProperties();

        properties.setIgnoreRepeat(true);
        properties.setNumCheck(true);
        properties.setEmailCheck(true);
        properties.setUrlCheck(true);
        assertThat(properties.isIgnoreRepeat()).isTrue();
        assertThat(properties.isNumCheck()).isTrue();
        assertThat(properties.isEmailCheck()).isTrue();
        assertThat(properties.isUrlCheck()).isTrue();

        properties.setIgnoreRepeat(false);
        properties.setNumCheck(false);
        properties.setEmailCheck(false);
        properties.setUrlCheck(false);
        assertThat(properties.isIgnoreRepeat()).isFalse();
        assertThat(properties.isNumCheck()).isFalse();
        assertThat(properties.isEmailCheck()).isFalse();
        assertThat(properties.isUrlCheck()).isFalse();
    }
}
