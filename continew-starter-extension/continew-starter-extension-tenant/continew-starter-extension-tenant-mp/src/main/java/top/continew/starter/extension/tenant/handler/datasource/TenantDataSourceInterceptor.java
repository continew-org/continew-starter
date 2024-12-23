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

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;
import top.continew.starter.extension.tenant.handler.TenantDataSourceHandler;

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
        if (TenantIsolationLevel.LINE.equals(TenantContextHolder.getIsolationLevel())) {
            return invocation.proceed();
        }
        // 切换数据源
        boolean isPush = false;
        try {
            tenantDataSourceHandler.changeDataSource(TenantContextHolder.getDataSource());
            isPush = true;
            return invocation.proceed();
        } finally {
            if (isPush) {
                DynamicDataSourceContextHolder.poll();
            }
        }
    }
}
