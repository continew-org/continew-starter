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

package top.charles7c.continew.starter.auth.satoken.autoconfigure;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import top.charles7c.continew.starter.auth.satoken.properties.SaTokenDaoProperties;
import top.charles7c.continew.starter.auth.satoken.properties.SaTokenSecurityProperties;

/**
 * SaToken 扩展配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "sa-token.extension")
public class SaTokenExtensionProperties {

    /**
     * 是否启用扩展
     */
    private boolean enabled = false;

    /**
     * 权限认证实现
     */
    private Class<? extends StpInterface> permissionImpl;

    /**
     * 持久层配置
     */
    @NestedConfigurationProperty
    private SaTokenDaoProperties dao;

    /**
     * 安全配置
     */
    @NestedConfigurationProperty
    private SaTokenSecurityProperties security;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Class<? extends StpInterface> getPermissionImpl() {
        return permissionImpl;
    }

    public void setPermissionImpl(Class<? extends StpInterface> permissionImpl) {
        this.permissionImpl = permissionImpl;
    }

    public SaTokenDaoProperties getDao() {
        return dao;
    }

    public void setDao(SaTokenDaoProperties dao) {
        this.dao = dao;
    }

    public SaTokenSecurityProperties getSecurity() {
        return security;
    }

    public void setSecurity(SaTokenSecurityProperties security) {
        this.security = security;
    }

    @Override
    public String toString() {
        return "SaTokenExtensionProperties{" + "enabled=" + enabled + ", permissionImpl=" + permissionImpl + ", dao=" + dao + ", security=" + security + '}';
    }
}
