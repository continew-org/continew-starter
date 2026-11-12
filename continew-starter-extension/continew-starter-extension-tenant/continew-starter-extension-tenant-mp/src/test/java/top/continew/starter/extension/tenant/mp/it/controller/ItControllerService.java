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

package top.continew.starter.extension.tenant.mp.it.controller;

import top.continew.starter.extension.tenant.mp.it.entity.ItTenantUser;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantUserMapper;

import java.util.List;

/**
 * 租户数据源拦截器测试服务
 *
 * <p>位于 controller 包，命中 {@code TenantDataSourceAdvisor} 的真实 AOP 切点，
 * 方法调用经 {@code TenantDataSourceInterceptor} 执行数据源级隔离路由。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class ItControllerService {

    private final ItTenantUserMapper tenantUserMapper;

    public ItControllerService(ItTenantUserMapper tenantUserMapper) {
        this.tenantUserMapper = tenantUserMapper;
    }

    /**
     * 按当前租户上下文查询用户。
     *
     * @return 当前租户下的用户列表
     */
    public List<ItTenantUser> listWithinTenant() {
        return tenantUserMapper.selectList(null);
    }
}
