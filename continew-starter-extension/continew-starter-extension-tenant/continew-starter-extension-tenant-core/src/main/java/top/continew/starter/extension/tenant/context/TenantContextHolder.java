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

package top.continew.starter.extension.tenant.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

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

    /**
     * 获取隔离级别
     */
    public static TenantIsolationLevel getIsolationLevel() {
        return Optional.ofNullable(getContext())
            .map(TenantContext::getIsolationLevel)
            .orElse(TenantIsolationLevel.LINE);
    }
}
