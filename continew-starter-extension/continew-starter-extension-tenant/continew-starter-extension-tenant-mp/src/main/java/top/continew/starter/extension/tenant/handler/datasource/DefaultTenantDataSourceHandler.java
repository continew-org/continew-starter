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

package top.continew.starter.extension.tenant.handler.datasource;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.extension.tenant.config.TenantDataSource;
import top.continew.starter.extension.tenant.handler.TenantDataSourceHandler;

import javax.sql.DataSource;

/**
 * 默认租户数据源级隔离处理器
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class DefaultTenantDataSourceHandler implements TenantDataSourceHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultTenantDataSourceHandler.class);
    private final DynamicRoutingDataSource dynamicRoutingDataSource;
    private final DefaultDataSourceCreator dataSourceCreator;

    public DefaultTenantDataSourceHandler(DynamicRoutingDataSource dynamicRoutingDataSource,
                                          DefaultDataSourceCreator dataSourceCreator) {
        this.dynamicRoutingDataSource = dynamicRoutingDataSource;
        this.dataSourceCreator = dataSourceCreator;
    }

    @Override
    public void changeDataSource(TenantDataSource tenantDataSource) {
        if (tenantDataSource == null) {
            return;
        }
        String dataSourceName = tenantDataSource.getPoolName();
        if (!this.containsDataSource(dataSourceName)) {
            DataSource datasource = this.createDataSource(tenantDataSource);
            dynamicRoutingDataSource.addDataSource(dataSourceName, datasource);
            log.info("Load data source: {}", dataSourceName);
        }
        DynamicDataSourceContextHolder.push(dataSourceName);
        log.info("Change data source: {}", dataSourceName);
    }

    @Override
    public boolean containsDataSource(String dataSourceName) {
        return CharSequenceUtil.isNotBlank(dataSourceName) && dynamicRoutingDataSource.getDataSources()
            .containsKey(dataSourceName);
    }

    @Override
    public DataSource createDataSource(TenantDataSource tenantDataSource) {
        DataSourceProperty dataSourceProperty = new DataSourceProperty();
        dataSourceProperty.setPoolName(tenantDataSource.getPoolName());
        dataSourceProperty.setDriverClassName(tenantDataSource.getDriverClassName());
        dataSourceProperty.setUrl(tenantDataSource.getUrl());
        dataSourceProperty.setUsername(tenantDataSource.getUsername());
        dataSourceProperty.setPassword(tenantDataSource.getPassword());
        return dataSourceCreator.createDataSource(dataSourceProperty);
    }

    @Override
    public void removeDataSource(String dataSourceName) {
        dynamicRoutingDataSource.removeDataSource(dataSourceName);
    }
}