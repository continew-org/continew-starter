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

package top.continew.starter.extension.tenant.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link TenantIsolationLevel} 枚举测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantIsolationLevelTest {

    @Test
    @DisplayName("枚举共 2 个值：行级与数据源级")
    void shouldContainExactlyTwoValues() {
        assertThat(TenantIsolationLevel.values())
            .containsExactly(TenantIsolationLevel.LINE, TenantIsolationLevel.DATASOURCE);
    }

    @ParameterizedTest(name = "[{index}] 枚举值 {0} 可按名称反查")
    @EnumSource(TenantIsolationLevel.class)
    void shouldSupportValueOfForEveryConstant(TenantIsolationLevel level) {
        assertThat(TenantIsolationLevel.valueOf(level.name())).isSameAs(level);
    }

    @Test
    @DisplayName("非法名称反查抛 IllegalArgumentException")
    void shouldRejectUnknownName() {
        assertThatThrownBy(() -> TenantIsolationLevel.valueOf("NO_SUCH")).isInstanceOf(
            IllegalArgumentException.class);
    }
}
