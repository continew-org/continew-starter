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

package top.continew.starter.extension.tenant.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

import java.util.List;

/**
 * {@link TenantProperties} 配置属性测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantPropertiesTest {

    @Test
    @DisplayName("默认配置：启用租户、行级隔离、约定列名与请求头")
    void shouldInitializeWithConventionDefaults() {
        TenantProperties properties = new TenantProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getIsolationLevel()).isEqualTo(TenantIsolationLevel.LINE);
        assertThat(properties.getTenantIdColumn()).isEqualTo("tenant_id");
        assertThat(properties.getTenantIdHeader()).isEqualTo("X-Tenant-Id");
        assertThat(properties.getIgnoreTables()).isNull();
    }

    @Test
    @DisplayName("配置项设置后读取一致")
    void shouldRoundTripAllProperties() {
        TenantProperties properties = new TenantProperties();
        properties.setEnabled(false);
        properties.setIsolationLevel(TenantIsolationLevel.DATASOURCE);
        properties.setTenantIdColumn("tid");
        properties.setTenantIdHeader("X-Tid");
        properties.setIgnoreTables(List.of("sys_config"));
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getIsolationLevel()).isEqualTo(TenantIsolationLevel.DATASOURCE);
        assertThat(properties.getTenantIdColumn()).isEqualTo("tid");
        assertThat(properties.getTenantIdHeader()).isEqualTo("X-Tid");
        assertThat(properties.getIgnoreTables()).containsExactly("sys_config");
    }

    @Test
    @DisplayName("配置前缀锁定为 continew-starter.tenant")
    void shouldBindToTenantPrefix() {
        ConfigurationProperties annotation = TenantProperties.class
            .getAnnotation(ConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(PropertiesConstants.TENANT);
    }
}
