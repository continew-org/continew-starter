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

package top.continew.starter.auth.refresh.token.model;

/**
 * 创建登录会话时由业务方提供的主体快照。
 *
 * <p>认证会话模块不依赖任何用户实体，登录签发时由业务方把已认证主体的必要信息映射为
 * 本快照，随 Refresh Session 一并保存，用于会话管理展示与失效原因判定。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
public interface RefreshSessionPrincipal {

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    Long getUserId();

    /**
     * 获取登录时的用户名快照，用于登录会话管理。
     *
     * @return 登录时的用户名快照
     */
    String getUsername();

    /**
     * 获取登录时的用户昵称快照，用于登录会话管理。
     *
     * @return 登录时的用户昵称快照
     */
    String getNickname();

    /**
     * 获取登录时确定的租户 ID，避免刷新时跨租户使用。
     *
     * @return 租户 ID；无租户场景返回 {@code null}
     */
    Long getTenantId();

    /**
     * 获取初次登录 IP。
     *
     * @return 初次登录 IP
     */
    String getIp();

    /**
     * 获取初次登录 IP 归属地。
     *
     * @return 初次登录 IP 归属地
     */
    String getAddress();

    /**
     * 获取初次登录浏览器或客户端。
     *
     * @return 初次登录浏览器或客户端
     */
    String getBrowser();

    /**
     * 获取初次登录操作系统。
     *
     * @return 初次登录操作系统
     */
    String getOs();
}
