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

package top.continew.starter.extension.tenant.mp.it.service;

import top.continew.starter.extension.tenant.annotation.TenantIgnore;
import top.continew.starter.extension.tenant.mp.it.entity.ItTenantUser;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantUserMapper;

import java.util.List;

/**
 * 租户忽略测试服务
 *
 * <p>经 AOP 代理触发真实 {@code TenantIgnoreAspect} 切面链路。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class ItIgnoreService {

    private final ItTenantUserMapper tenantUserMapper;

    public ItIgnoreService(ItTenantUserMapper tenantUserMapper) {
        this.tenantUserMapper = tenantUserMapper;
    }

    /**
     * 查询全部用户（忽略租户行级隔离）。
     *
     * @return 全量用户列表
     */
    @TenantIgnore
    public List<ItTenantUser> listAllIgnoringTenant() {
        return tenantUserMapper.selectList(null);
    }

    /**
     * 按当前租户上下文查询用户（不忽略租户）。
     *
     * @return 当前租户下的用户列表
     */
    public List<ItTenantUser> listWithinTenant() {
        return tenantUserMapper.selectList(null);
    }
}
