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

/**
 * 租户数据源提供者
 *
 * @author Charles7c
 * @since 2.7.0
 */
public interface TenantDataSourceProvider {

    /**
     * 根据租户 ID 获取数据源配置
     *
     * @param tenantId 租户 ID
     * @return 数据源配置
     */
    TenantDataSource getByTenantId(String tenantId);
}
