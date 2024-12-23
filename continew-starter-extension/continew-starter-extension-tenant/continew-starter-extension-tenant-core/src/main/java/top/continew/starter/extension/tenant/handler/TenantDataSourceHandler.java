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

package top.continew.starter.extension.tenant.handler;

import top.continew.starter.extension.tenant.config.TenantDataSource;

import javax.sql.DataSource;

/**
 * 租户数据源级隔离处理器
 *
 * @author Charles7c
 * @since 2.7.0
 */
public interface TenantDataSourceHandler {

    /**
     * 切换数据源
     *
     * @param tenantDataSource 数据源配置
     */
    void changeDataSource(TenantDataSource tenantDataSource);

    /**
     * 是否存在指定数据源
     *
     * @param dataSourceName 数据源名称
     * @return 是否存在指定数据源
     */
    boolean containsDataSource(String dataSourceName);

    /**
     * 创建数据源
     *
     * @param tenantDataSource 数据源配置
     * @return 数据源
     */
    DataSource createDataSource(TenantDataSource tenantDataSource);

    /**
     * 移除数据源
     *
     * @param dataSourceName 数据源名称
     */
    void removeDataSource(String dataSourceName);
}
