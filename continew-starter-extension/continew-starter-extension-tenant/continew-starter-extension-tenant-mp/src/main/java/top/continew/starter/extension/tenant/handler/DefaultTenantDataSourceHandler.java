package top.continew.starter.extension.tenant.handler;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.extension.tenant.config.TenantDataSource;
import top.continew.starter.extension.tenant.config.TenantDataSourceProvider;

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
    private final TenantDataSourceProvider tenantDataSourceProvider;

    public DefaultTenantDataSourceHandler(TenantDataSourceProvider tenantDataSourceProvider, DynamicRoutingDataSource dynamicRoutingDataSource, DefaultDataSourceCreator dataSourceCreator) {
        this.tenantDataSourceProvider = tenantDataSourceProvider;
        this.dynamicRoutingDataSource = dynamicRoutingDataSource;
        this.dataSourceCreator = dataSourceCreator;
    }

    @Override
    public void changeDataSource(String dataSourceName) {
        if (!this.containsDataSource(dataSourceName)) {
            TenantDataSource tenantDataSource = tenantDataSourceProvider.getByTenantId(dataSourceName);
            if (null == tenantDataSource) {
                throw new IllegalArgumentException("Data source [%s] configuration not found".formatted(dataSourceName));
            }
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