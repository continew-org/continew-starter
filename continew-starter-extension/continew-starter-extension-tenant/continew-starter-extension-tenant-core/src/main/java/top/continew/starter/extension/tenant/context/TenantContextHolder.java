package top.continew.starter.extension.tenant.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 租户上下文 Holder
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class TenantContextHolder {

    private static final TransmittableThreadLocal<TenantContext> CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 设置上下文
     *
     * @param context 上下文
     */
    public static void setContext(TenantContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取上下文
     *
     * @return 上下文
     */
    public static TenantContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除上下文
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 获取租户 ID
     *
     * @return 租户 ID
     */
    public static Long getTenantId() {
        return Optional.ofNullable(getContext()).map(TenantContext::getTenantId).orElse(null);
    }
}
