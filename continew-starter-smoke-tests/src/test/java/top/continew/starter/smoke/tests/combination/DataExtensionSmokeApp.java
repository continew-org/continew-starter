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

package top.continew.starter.smoke.tests.combination;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import top.continew.starter.extension.datapermission.model.UserData;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContext;

import java.util.Collections;

/**
 * data + extension 组合冒烟启动类：mp 与 tenant/datapermission 装配共存。
 *
 * <p>tenant 与 datapermission 各要求消费方提供一个业务提供者 Bean，冒烟侧
 * 注册最小实现满足装配（这也是真实消费方的集成姿态）。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootApplication
public class DataExtensionSmokeApp {

    /**
     * 租户提供者最小实现：按租户 ID 返回空上下文。
     *
     * @return 租户提供者
     */
    @Bean
    public TenantProvider tenantProvider() {
        return (tenantIdAsString, isVerify) -> {
            TenantContext context = new TenantContext();
            context.setTenantId(Long.valueOf(tenantIdAsString));
            return context;
        };
    }

    /**
     * 数据权限用户数据提供者最小实现：不过滤、空用户。
     *
     * @return 用户数据提供者
     */
    @Bean
    public DataPermissionUserDataProvider dataPermissionUserDataProvider() {
        return new DataPermissionUserDataProvider() {

            @Override
            public boolean isFilter() {
                return false;
            }

            @Override
            public UserData getUserData() {
                return new UserData(1L, null, Collections.emptySet());
            }
        };
    }
}
