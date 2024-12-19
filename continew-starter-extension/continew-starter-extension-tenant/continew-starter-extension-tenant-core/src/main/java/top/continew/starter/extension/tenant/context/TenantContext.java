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

import top.continew.starter.extension.tenant.config.TenantDataSource;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

/**
 * 租户上下文
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class TenantContext {

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 隔离级别
     */
    private TenantIsolationLevel isolationLevel;

    /**
     * 数据源信息
     */
    private TenantDataSource dataSource;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public TenantIsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    public void setIsolationLevel(TenantIsolationLevel isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public TenantDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(TenantDataSource dataSource) {
        this.dataSource = dataSource;
    }
}
