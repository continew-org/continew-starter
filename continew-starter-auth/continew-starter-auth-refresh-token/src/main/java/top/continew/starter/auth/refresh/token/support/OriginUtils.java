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

package top.continew.starter.auth.refresh.token.support;

import java.net.URI;

/**
 * Origin 校验工具。
 *
 * <p>请求守卫的来源解析与配置属性的来源白名单校验共用同一份语义，避免两份校验漂移。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public final class OriginUtils {

    private OriginUtils() {
    }

    /**
     * 校验字符串是否为严格的无路径/查询/片段的 HTTP(S) Origin。
     *
     * <p>端口只允许省略（-1）或 1-65535（拒绝 0），并拒绝 {@code https://host:} 这类
     * 以冒号结尾的空端口写法。</p>
     *
     * @param value 待校验的 Origin
     * @return true：合法 HTTP(S) Origin；false：非法
     */
    public static boolean isValidOrigin(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            int port = uri.getPort();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && uri.getHost() != null
                && uri.getUserInfo() == null
                && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null
                && (port == -1 || port > 0 && port <= 65535)
                && !uri.getRawAuthority().endsWith(":");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
