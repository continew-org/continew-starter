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
