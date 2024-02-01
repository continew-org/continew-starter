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

/**
 * SaToken 权限认证配置属性
 *
 * @author Charles7c
 * @since 1.3.0
 */
public class SaTokenPermissionProperties {

    /**
     * 是否启用权限认证
     */
    private boolean enabled = false;

    /**
     * 自定义权限认证实现类（当 enabled 为 true 时必填）
     */
    private Class<? extends StpInterface> impl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Class<? extends StpInterface> getImpl() {
        return impl;
    }

    public void setImpl(Class<? extends StpInterface> impl) {
        this.impl = impl;
    }

    @Override
    public String toString() {
        return "SaTokenPermissionProperties{" + "enabled=" + enabled + ", impl=" + impl + '}';
    }
}