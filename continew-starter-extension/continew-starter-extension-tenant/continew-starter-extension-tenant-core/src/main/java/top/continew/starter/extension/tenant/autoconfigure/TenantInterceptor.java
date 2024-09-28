package top.continew.starter.extension.tenant.autoconfigure;

import cn.hutool.core.convert.Convert;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import top.continew.starter.extension.tenant.context.TenantContext;
import top.continew.starter.extension.tenant.context.TenantContextHolder;

/**
 * 租户拦截器
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantProperties tenantProperties;

    public TenantInterceptor(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(tenantProperties.getTenantIdHeader());
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId(Convert.toLong(tenantId));
        TenantContextHolder.setContext(tenantContext);
        return true;
    }
}