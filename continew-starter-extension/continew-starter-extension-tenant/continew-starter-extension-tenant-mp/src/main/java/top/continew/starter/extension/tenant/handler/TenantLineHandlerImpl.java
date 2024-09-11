package top.continew.starter.extension.tenant.handler;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.context.TenantContextHolder;

/**
 * 租户行级隔离处理器
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class TenantLineHandlerImpl implements TenantLineHandler {

    private final TenantProperties tenantProperties;

    public TenantLineHandlerImpl(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (null != tenantId) {
            return new LongValue(tenantId);
        }
        return null;
    }

    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getTenantIdColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (null != tenantId && tenantId.equals(tenantProperties.getSuperTenantId())) {
            return true;
        }
        return CollUtil.contains(tenantProperties.getIgnoreTables(), tableName);
    }
}