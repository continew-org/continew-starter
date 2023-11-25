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

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.StpInterface;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * SaToken 扩展配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "sa-token.extension")
public class SaTokenExtensionProperties {

    /**
     * 是否启用扩展
     */
    private boolean enabled = false;

    /**
     * 自定义缓存实现
     */
    private Class<? extends SaTokenDao> daoImpl;

    /**
     * 权限认证实现
     */
    private Class<? extends StpInterface> permissionImpl;

    /**
     * 安全配置
     */
    private SecurityProperties security;

    /**
     * 安全配置属性
     */
    @Data
    public static class SecurityProperties {

        /**
         * 排除（放行）路径配置
         */
        private String[] excludes = new String[0];
    }
}
