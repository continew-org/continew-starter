package top.continew.starter.extension.tenant.context;

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

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
