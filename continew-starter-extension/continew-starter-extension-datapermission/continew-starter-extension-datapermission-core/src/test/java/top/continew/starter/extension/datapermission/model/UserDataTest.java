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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import top.continew.starter.extension.datapermission.enums.DataScope;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link UserData} 模型测试
 *
 * <p>
 * 重点锁定角色列表的 null 归一化与 {@link UserData#isValid()} 的有效性判定语义。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class UserDataTest {

    @Test
    @DisplayName("无参构造时角色列表默认为空集合")
    void shouldDefaultRolesToEmptySet() {
        assertThat(new UserData().getRoles()).isEmpty();
    }

    @Test
    @DisplayName("构造传入 null 角色列表时归一为空集合")
    void shouldNormalizeNullRolesInConstructor() {
        assertThat(new UserData(1L, 2L, null).getRoles()).isEmpty();
    }

    @Test
    @DisplayName("setter 传入 null 角色列表时归一为空集合")
    void shouldNormalizeNullRolesInSetter() {
        UserData userData = new UserData();
        userData.setRoles(null);
        assertThat(userData.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("字段存取往返一致且角色集合按原引用保留")
    void shouldAccessAndKeepFields() {
        Set<RoleData> roles = Set.of(new RoleData(10L, DataScope.SELF));
        UserData userData = new UserData(1L, 2L, roles);
        assertThat(userData.getUserId()).isEqualTo(1L);
        assertThat(userData.getDeptId()).isEqualTo(2L);
        assertThat(userData.getRoles()).isSameAs(roles);

        userData.setUserId(3L);
        userData.setDeptId(4L);
        assertThat(userData.getUserId()).isEqualTo(3L);
        assertThat(userData.getDeptId()).isEqualTo(4L);
    }

    @ParameterizedTest(name = "[{index}] userId={0}, deptId={1}, roles 非空={2} 时有效性应为 {3}")
    @MethodSource("validityProvider")
    void shouldValidateUserDataSetComprehensively(Long userId, Long deptId, Set<RoleData> roles,
        boolean expected) {
        assertThat(new UserData(userId, deptId, roles).isValid()).isEqualTo(expected);
    }

    private static Stream<Arguments> validityProvider() {
        Set<RoleData> roles = Set.of(new RoleData(10L, DataScope.CUSTOM));
        return Stream.of(arguments(null, null, Collections.emptySet(), false),
            arguments(1L, null, Collections.emptySet(), false),
            arguments(1L, 2L, Collections.emptySet(), false),
            arguments(null, 2L, roles, false),
            arguments(1L, null, roles, false),
            arguments(1L, 2L, roles, true));
    }
}
