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

package top.continew.starter.extension.tenant.util;

import cn.hutool.extra.spring.SpringUtil;
import top.continew.starter.extension.tenant.TenantDataSourceHandler;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContext;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

/**
 * 租户工具类
 *
 * @author 小熊
 * @author Charles7c
 * @since 2.13.1
 */
public class TenantUtils {

    private TenantUtils() {
    }

    /**
     * 使用指定租户执行业务逻辑
     *
     * <p>
     * 强制设置 {@code TenantContextHolder.setIgnore(false)}，执行完恢复原值。<br>
     * 适用于在非租户逻辑中执行有租户逻辑的业务逻辑。
     * </p>
     *
     * @param tenantId 租户 ID
     * @param runnable 业务逻辑
     */
    public static void execute(Long tenantId, Runnable runnable) {
        // 未启用租户，直接执行业务逻辑
        if (TenantContextHolder.isTenantDisabled()) {
            runnable.run();
            return;
        }
        // 原租户上下文
        TenantContext oldContext = TenantContextHolder.getContext();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        boolean isPush = false;
        try {
            TenantContext newContext = SpringUtil.getBean(TenantProvider.class)
                .getByTenantId(tenantId.toString(), false);
            // 设置新租户上下文
            TenantContextHolder.setContext(newContext);
            TenantContextHolder.setIgnore(false);
            // 数据源级隔离：切换数据源
            if (TenantIsolationLevel.DATASOURCE.equals(newContext.getIsolationLevel())) {
                SpringUtil.getBean(TenantDataSourceHandler.class)
                    .changeDataSource(newContext.getDataSource());
                isPush = true;
            }
            // 执行业务逻辑
            runnable.run();
        } finally {
            // 恢复原租户上下文
            TenantContextHolder.setContext(oldContext);
            TenantContextHolder.setIgnore(oldIgnore);
            if (isPush) {
                SpringUtil.getBean(TenantDataSourceHandler.class).poll();
            }
        }
    }

    /**
     * 忽略租户执行业务逻辑
     *
     * <p>
     * 适用于在租户逻辑中执行非租户逻辑的业务逻辑。
     * </p>
     *
     * @param runnable 业务逻辑
     */
    public static void executeIgnore(Runnable runnable) {
        // 未启用或忽略租户，直接执行业务逻辑
        if (TenantContextHolder.isTenantDisabled() || TenantContextHolder.isIgnore()) {
            runnable.run();
            return;
        }
        try {
            TenantContextHolder.setIgnore(true);
            // 执行业务逻辑
            runnable.run();
        } finally {
            TenantContextHolder.setIgnore(false);
        }
    }
}
