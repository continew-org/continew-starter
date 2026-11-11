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

package top.continew.starter.extension.datapermission.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link DataScope} 枚举全值遍历测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DataScopeTest {

    @Test
    @DisplayName("枚举共 5 个值且声明顺序固定")
    void shouldContainExactlyFiveValuesInDeclaredOrder() {
        assertThat(DataScope.values())
            .containsExactly(DataScope.ALL, DataScope.DEPT_AND_CHILD, DataScope.DEPT,
                DataScope.SELF,
                DataScope.CUSTOM);
    }

    @ParameterizedTest(name = "[{index}] 枚举值 {0} 可按名称反查且名称非空")
    @EnumSource(DataScope.class)
    void shouldSupportValueOfForEveryConstant(DataScope dataScope) {
        assertThat(dataScope.name()).isNotBlank();
        assertThat(DataScope.valueOf(dataScope.name())).isSameAs(dataScope);
    }
}
