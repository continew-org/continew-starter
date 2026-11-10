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

package top.continew.starter.extension.datapermission.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.extension.datapermission.enums.DataScope;

/**
 * {@link RoleData} 模型测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RoleDataTest {

    @Test
    @DisplayName("全参构造与 setter 均可正确存取字段")
    void shouldConstructAndAccessFields() {
        RoleData roleData = new RoleData(1L, DataScope.DEPT);
        assertThat(roleData.getRoleId()).isEqualTo(1L);
        assertThat(roleData.getDataScope()).isEqualTo(DataScope.DEPT);

        roleData.setRoleId(2L);
        roleData.setDataScope(DataScope.SELF);
        assertThat(roleData.getRoleId()).isEqualTo(2L);
        assertThat(roleData.getDataScope()).isEqualTo(DataScope.SELF);
    }

    @Test
    @DisplayName("无参构造后字段为 null")
    void shouldLeaveFieldsNullAfterDefaultConstruction() {
        RoleData roleData = new RoleData();
        assertThat(roleData.getRoleId()).isNull();
        assertThat(roleData.getDataScope()).isNull();
    }

    @Test
    @DisplayName("roleId 与数据范围均相同时等价且 hashCode 一致")
    void shouldBeEqualWithSameRoleIdAndDataScope() {
        RoleData first = new RoleData(1L, DataScope.DEPT);
        RoleData second = new RoleData(1L, DataScope.DEPT);
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second).isEqualTo(first);
    }

    @Test
    @DisplayName("roleId 不同时不等价")
    void shouldNotBeEqualWithDifferentRoleId() {
        assertThat(new RoleData(1L, DataScope.DEPT)).isNotEqualTo(new RoleData(2L, DataScope.DEPT));
    }

    @Test
    @DisplayName("数据范围不同时不等价")
    void shouldNotBeEqualWithDifferentDataScope() {
        assertThat(new RoleData(1L, DataScope.DEPT)).isNotEqualTo(new RoleData(1L, DataScope.SELF));
    }

    @Test
    @DisplayName("构造出的实例非 null")
    void shouldBeNotNullAfterConstruction() {
        RoleData roleData = new RoleData(1L, DataScope.DEPT);
        assertThat(roleData).isNotNull();
    }

    @Test
    @DisplayName("dataScope 为 null 时按引用比较返回 false 且不抛异常（锁定当前行为）")
    void shouldReturnFalseWithoutThrowingWhenDataScopeIsNull() {
        RoleData nullScope = new RoleData(1L, null);
        assertThat(nullScope).isNotEqualTo(new RoleData(1L, DataScope.ALL));
        assertThat(new RoleData(1L, DataScope.ALL)).isNotEqualTo(nullScope);
    }

    @Test
    @DisplayName("roleId 为 null 时 equals 抛 NPE（锁定当前行为）")
    void shouldThrowNpeOnEqualsWhenRoleIdIsNull() {
        RoleData nullRoleId = new RoleData(null, DataScope.ALL);
        Throwable thrown = catchThrowableOfType(NullPointerException.class, () -> nullRoleId
            .equals(new RoleData(1L, DataScope.ALL)));
        assertThat(thrown).isNotNull();
    }

    @Test
    @DisplayName("roleId 或 dataScope 为 null 时 hashCode 抛 NPE（锁定当前行为）")
    void shouldThrowNpeOnHashCodeWhenFieldIsNull() {
        RoleData nullRoleId = new RoleData(null, DataScope.ALL);
        RoleData nullScope = new RoleData(1L, null);
        assertThat(catchThrowableOfType(NullPointerException.class, nullRoleId::hashCode))
            .isNotNull();
        assertThat(catchThrowableOfType(NullPointerException.class, nullScope::hashCode))
            .isNotNull();
    }
}
