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

import java.util.List;

/**
 * {@link AccessLogProperties} 访问日志配置属性测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AccessLogPropertiesTest {

    /**
     * 默认关闭打印、关闭参数打印与脱敏，截断阈值为 2000、最大保留 50、后缀省略号
     */
    @Test
    @DisplayName("默认配置：全部关闭 + 阈值 2000 + 保留 50 + 后缀省略号")
    void propertiesShouldDeclareSensibleDefaults() {
        AccessLogProperties properties = new AccessLogProperties();
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isPrintRequestParam()).isFalse();
        assertThat(properties.isLongParamTruncate()).isFalse();
        assertThat(properties.getLongParamThreshold()).isEqualTo(2000);
        assertThat(properties.getLongParamMaxLength()).isEqualTo(50);
        assertThat(properties.getLongParamSuffix()).isEqualTo("...");
        assertThat(properties.isParamSensitive()).isFalse();
        assertThat(properties.getSensitiveParams()).isEmpty();
    }

    /**
     * 各开关与阈值应可独立设置
     */
    @Test
    @DisplayName("开关与阈值可独立设置")
    void propertiesShouldBeSettable() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setEnabled(true);
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(100);
        properties.setLongParamMaxLength(20);
        properties.setLongParamSuffix("…");
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password", "token"));
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isPrintRequestParam()).isTrue();
        assertThat(properties.isLongParamTruncate()).isTrue();
        assertThat(properties.getLongParamThreshold()).isEqualTo(100);
        assertThat(properties.getLongParamMaxLength()).isEqualTo(20);
        assertThat(properties.getLongParamSuffix()).isEqualTo("…");
        assertThat(properties.isParamSensitive()).isTrue();
        assertThat(properties.getSensitiveParams()).containsExactly("password", "token");
    }
}
