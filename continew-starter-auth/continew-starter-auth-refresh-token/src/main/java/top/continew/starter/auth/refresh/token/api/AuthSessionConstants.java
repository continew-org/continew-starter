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

package top.continew.starter.auth.refresh.token.api;

/**
 * 认证会话常量。
 *
 * @author luoqiz
 * @since 2.17.0
 */
public final class AuthSessionConstants {

    /**
     * Access Token 中绑定 Refresh Session ID 的声明名称
     */
    public static final String SESSION_ID_CLAIM = "sid";

    /**
     * 会话失效广播 Topic 前缀，完整 Topic 默认追加应用名：{prefix}:{spring.application.name}。
     *
     * <p>命名风格有意与存储 Key 区分：广播 Topic（Pub/Sub 消息通道）用全小写连字符，
     * 存储 Key（{@code AUTH:REFRESH:V1:*}）用大写冒号分层，两类命名空间语义不同，不强求统一。</p>
     */
    public static final String ACCESS_SESSION_INVALID_TOPIC_PREFIX = "auth:access-session-invalid";

    private AuthSessionConstants() {
    }
}
