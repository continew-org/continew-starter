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

package top.continew.starter.extension.tenant.handler;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import top.continew.starter.extension.tenant.TenantDataSourceHandler;
import top.continew.starter.extension.tenant.TenantHandler;
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContext;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

/**
 * 租户处理器
 *
 * @author 小熊
 * @since 2.8.0
 */
public class DefaultTenantHandler implements TenantHandler {

    private final TenantProperties tenantProperties;
    private final TenantDataSourceHandler dataSourceHandler;
    private final TenantProvider tenantProvider;

    public DefaultTenantHandler(TenantProperties tenantProperties,
                                TenantDataSourceHandler dataSourceHandler,
                                TenantProvider tenantProvider) {
        this.tenantProperties = tenantProperties;
        this.dataSourceHandler = dataSourceHandler;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void execute(Long tenantId, Runnable runnable) {
        if (!tenantProperties.isEnabled()) {
            return;
        }
        TenantContext tenantHandler = tenantProvider.getByTenantId(tenantId.toString(), false);
        // 保存当前的租户上下文
        TenantContext originalContext = TenantContextHolder.getContext();
        boolean isPush = false;
        try {
            // 设置新的租户上下文
            TenantContextHolder.setContext(tenantHandler);
            // 切换数据源
            if (TenantIsolationLevel.DATASOURCE.equals(tenantHandler.getIsolationLevel())) {
                dataSourceHandler.changeDataSource(tenantHandler.getDataSource());
                isPush = true;
            }
            // 执行业务逻辑
            runnable.run();
        } finally {
            // 恢复原始的租户上下文
            if (originalContext != null) {
                TenantContextHolder.setContext(originalContext);
            } else {
                TenantContextHolder.clearContext();
            }
            if (isPush) {
                DynamicDataSourceContextHolder.poll();
            }
        }
    }
}
