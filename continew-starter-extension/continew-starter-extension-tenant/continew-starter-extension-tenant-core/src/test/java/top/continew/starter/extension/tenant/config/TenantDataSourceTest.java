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

package top.continew.starter.extension.tenant.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TenantDataSource} 数据源配置模型测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantDataSourceTest {

    @Test
    @DisplayName("新建实例全部字段为空")
    void shouldInitializeWithBlankFields() {
        TenantDataSource dataSource = new TenantDataSource();
        assertThat(dataSource.getPoolName()).isNull();
        assertThat(dataSource.getDriverClassName()).isNull();
        assertThat(dataSource.getUrl()).isNull();
        assertThat(dataSource.getUsername()).isNull();
        assertThat(dataSource.getPassword()).isNull();
    }

    @Test
    @DisplayName("数据源连接信息设置后读取一致")
    void shouldRoundTripAllFields() {
        TenantDataSource dataSource = new TenantDataSource();
        dataSource.setPoolName("tenant_9");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/tenant_9");
        dataSource.setUsername("root");
        dataSource.setPassword("secret");
        assertThat(dataSource.getPoolName()).isEqualTo("tenant_9");
        assertThat(dataSource.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(dataSource.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/tenant_9");
        assertThat(dataSource.getUsername()).isEqualTo("root");
        assertThat(dataSource.getPassword()).isEqualTo("secret");
    }
}
