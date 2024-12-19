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

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContext;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

/**
 * @description: 租户处理器
 * @author: 小熊
 * @create: 2024-12-18 19:43
 */
public class DefaultTenantHandler implements TenantHandler {

    private final TenantDataSourceHandler dataSourceHandler;
    private final TenantProvider tenantProvider;

    public DefaultTenantHandler(TenantDataSourceHandler dataSourceHandler, TenantProvider tenantProvider) {
        this.dataSourceHandler = dataSourceHandler;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void executeInTenant(Long tenantId, Runnable runnable) {
        boolean enabled = SpringUtil.getProperty("continew-starter.tenant.enabled", Boolean.class, false);
        if (enabled) {
            TenantContext tenantHandler = tenantProvider.getByTenantId(tenantId.toString(), false);
            // 保存当前的租户上下文
            TenantContext originalContext = TenantContextHolder.getContext();
            // 切换数据源
            boolean isPush = false;
            try {
                // 设置新的租户上下文
                TenantContextHolder.setContext(tenantHandler);
                if (TenantIsolationLevel.DATASOURCE.equals(tenantHandler.getIsolationLevel())) {
                    //切换数据源
                    dataSourceHandler.changeDataSource(tenantHandler.getDataSource());
                    isPush = true;
                }
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

}
