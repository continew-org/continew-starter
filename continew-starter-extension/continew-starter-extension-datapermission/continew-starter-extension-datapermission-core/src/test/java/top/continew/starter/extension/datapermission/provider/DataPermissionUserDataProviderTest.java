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

package top.continew.starter.extension.datapermission.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.model.RoleData;
import top.continew.starter.extension.datapermission.model.UserData;

import java.util.Set;

/**
 * {@link DataPermissionUserDataProvider} 接口契约测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DataPermissionUserDataProviderTest {

    @Test
    @DisplayName("契约仅包含 isFilter 与 getUserData 两个方法")
    void shouldExposeOnlyTwoContractMethods() {
        assertThat(DataPermissionUserDataProvider.class.isInterface()).isTrue();
        assertThat(DataPermissionUserDataProvider.class.getDeclaredMethods()).hasSize(2);
    }

    @Test
    @DisplayName("匿名实现按契约返回过滤开关与用户数据")
    void shouldImplementContractSemantics() {
        UserData userData = new UserData(1L, 2L, Set.of(new RoleData(10L, DataScope.SELF)));
        DataPermissionUserDataProvider provider = new DataPermissionUserDataProvider() {

            @Override
            public boolean isFilter() {
                return true;
            }

            @Override
            public UserData getUserData() {
                return userData;
            }
        };
        assertThat(provider.isFilter()).isTrue();
        assertThat(provider.getUserData()).isSameAs(userData);
    }

    @Test
    @DisplayName("Mockito 桩定后按桩返回且交互次数可验证")
    void shouldStubAndVerifyWithMockito() {
        UserData userData = new UserData(1L, 2L, Set.of());
        DataPermissionUserDataProvider provider = mock(DataPermissionUserDataProvider.class);
        when(provider.isFilter()).thenReturn(false);
        when(provider.getUserData()).thenReturn(userData);

        assertThat(provider.isFilter()).isFalse();
        assertThat(provider.getUserData()).isSameAs(userData);
        verify(provider, times(1)).isFilter();
        verify(provider, times(1)).getUserData();
    }
}
