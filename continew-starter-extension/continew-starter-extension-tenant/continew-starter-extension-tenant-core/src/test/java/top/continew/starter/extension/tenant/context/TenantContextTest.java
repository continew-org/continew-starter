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

package top.continew.starter.extension.tenant.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import top.continew.starter.extension.tenant.config.TenantDataSource;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

/**
 * {@link TenantContext} 上下文模型测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantContextTest {

    @Test
    @DisplayName("新建实例默认行级隔离，租户 ID 与数据源为空")
    void shouldInitializeWithLineLevelAndBlankFields() {
        TenantContext context = new TenantContext();
        assertThat(context.getTenantId()).isNull();
        assertThat(context.getIsolationLevel()).isEqualTo(TenantIsolationLevel.LINE);
        assertThat(context.getDataSource()).isNull();
    }

    @ParameterizedTest(name = "[{index}] 隔离级别 {0} 设置后读取一致")
    @EnumSource(TenantIsolationLevel.class)
    void shouldRoundTripIsolationLevel(TenantIsolationLevel level) {
        TenantContext context = new TenantContext();
        context.setIsolationLevel(level);
        assertThat(context.getIsolationLevel()).isSameAs(level);
    }

    @Test
    @DisplayName("租户 ID 与数据源配置设置后读取一致")
    void shouldRoundTripTenantIdAndDataSource() {
        TenantContext context = new TenantContext();
        TenantDataSource dataSource = new TenantDataSource();
        dataSource.setPoolName("tenant_9");
        context.setTenantId(9L);
        context.setDataSource(dataSource);
        assertThat(context.getTenantId()).isEqualTo(9L);
        assertThat(context.getDataSource()).isSameAs(dataSource);
    }
}
