package top.continew.starter.extension.tenant.handler;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import top.continew.starter.extension.tenant.context.TenantContextHolder;

/**
 * 租户数据源级隔离拦截器
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class TenantDataSourceInterceptor implements MethodInterceptor {

    private final TenantDataSourceHandler tenantDataSourceHandler;

    public TenantDataSourceInterceptor(TenantDataSourceHandler tenantDataSourceHandler) {
        this.tenantDataSourceHandler = tenantDataSourceHandler;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        if (null == tenantId) {
            return invocation.proceed();
        }
        // 切换数据源
        boolean isPush = false;
        try {
            String dataSourceName = tenantId.toString();
            tenantDataSourceHandler.changeDataSource(dataSourceName);
            isPush = true;
            return invocation.proceed();
        } finally {
            if (isPush) {
                DynamicDataSourceContextHolder.poll();
            }
        }
    }
}
