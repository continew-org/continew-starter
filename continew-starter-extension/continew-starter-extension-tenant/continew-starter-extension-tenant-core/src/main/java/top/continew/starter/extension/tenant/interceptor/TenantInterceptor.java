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

package top.continew.starter.extension.tenant.interceptor;

import cn.hutool.core.annotation.AnnotationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.continew.starter.extension.tenant.annotation.TenantIgnore;
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContextHolder;

/**
 * 租户拦截器
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantProperties tenantProperties;
    private final TenantProvider tenantProvider;

    public TenantInterceptor(TenantProperties tenantProperties, TenantProvider tenantProvider) {
        this.tenantProperties = tenantProperties;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
        Object handler) {
        // 设置上下文
        String tenantId = request.getHeader(tenantProperties.getTenantIdHeader());
        TenantContextHolder.setContext(tenantProvider.getByTenantId(tenantId, true));
        // 设置是否忽略租户
        TenantContextHolder.setIgnore(this.isIgnore(handler));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception e) {
        // 清除上下文
        TenantContextHolder.clear();
    }

    /**
     * 是否忽略租户
     *
     * @param handler 处理器
     * @return 是否忽略租户
     */
    private boolean isIgnore(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            TenantIgnore methodAnnotation = handlerMethod.getMethodAnnotation(TenantIgnore.class);
            if (methodAnnotation != null) {
                return true;
            }
            return AnnotationUtil.getAnnotation(handlerMethod.getBeanType(),
                TenantIgnore.class) != null;
        }
        return false;
    }
}
