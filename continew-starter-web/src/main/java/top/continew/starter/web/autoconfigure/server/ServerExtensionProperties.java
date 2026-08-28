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

package top.continew.starter.web.autoconfigure.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务器配置属性
 *
 * @author Charles7c
 * @since 2.11.0
 */
@ConfigurationProperties("server.extension")
public class ServerExtensionProperties {

    /**
     * 默认禁止三个不安全的 HTTP 方法（如 CONNECT、TRACE、TRACK）
     */
    private static final List<String> DEFAULT_ALLOWED_METHODS =
        List.of("CONNECT", "TRACE", "TRACK");

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 不允许的请求方式
     */
    private List<String> disallowedMethods = new ArrayList<>(DEFAULT_ALLOWED_METHODS);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDisallowedMethods() {
        return disallowedMethods;
    }

    public void setDisallowedMethods(List<String> disallowedMethods) {
        this.disallowedMethods = disallowedMethods;
    }
}
