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

package top.continew.starter.extension.tenant.config;

import top.continew.starter.extension.tenant.context.TenantContext;

/**
 * 租户提供者
 *
 * @author Charles7c
 * @since 2.7.0
 */
public interface TenantProvider {

    /**
     * 根据租户 ID 获取租户上下文
     *
     * @param tenantId 租户 ID
     * @param isVerify 是否验证有效性
     * @return 租户上下文
     */
    TenantContext getByTenantId(String tenantId, boolean isVerify);
}
