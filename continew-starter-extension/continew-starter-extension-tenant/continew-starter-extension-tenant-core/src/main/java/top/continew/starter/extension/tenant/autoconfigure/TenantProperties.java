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

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;

import java.util.List;

/**
 * 租户配置属性
 *
 * @author Charles7c
 * @since 2.7.0
 */
@ConfigurationProperties(PropertiesConstants.TENANT)
public class TenantProperties {

    /**
     * 是否启用多租户
     */
    private boolean enabled = true;

    /**
     * 租户 ID 列名
     */
    private String tenantIdColumn = "tenant_id";

    /**
     * 请求头中租户 ID 键名
     */
    private String tenantIdHeader = "X-Tenant-Id";

    /**
     * 超级租户 ID
     */
    private Long superTenantId = 1L;

    /**
     * 忽略表（忽略拼接多租户条件）
     */
    private List<String> ignoreTables;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantIdColumn() {
        return tenantIdColumn;
    }

    public void setTenantIdColumn(String tenantIdColumn) {
        this.tenantIdColumn = tenantIdColumn;
    }

    public String getTenantIdHeader() {
        return tenantIdHeader;
    }

    public void setTenantIdHeader(String tenantIdHeader) {
        this.tenantIdHeader = tenantIdHeader;
    }

    public Long getSuperTenantId() {
        return superTenantId;
    }

    public void setSuperTenantId(Long superTenantId) {
        this.superTenantId = superTenantId;
    }

    public List<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(List<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }
}
