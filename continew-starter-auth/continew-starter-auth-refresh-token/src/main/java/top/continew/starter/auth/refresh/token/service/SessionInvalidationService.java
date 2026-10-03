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

package top.continew.starter.auth.refresh.token.service;

/**
 * 认证会话失效入口。
 *
 * <p>业务方在用户、租户、客户端等安全策略变更的数据库事务提交后调用，撤销对应的
 * 全部登录会话。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public interface SessionInvalidationService {

    /**
     * 撤销指定客户端的全部登录会话。
     *
     * @param clientId 客户端 ID
     */
    void invalidateClient(String clientId);

    /**
     * 撤销指定租户的全部登录会话。
     *
     * @param tenantId 租户 ID
     */
    void invalidateTenant(Long tenantId);

    /**
     * 撤销指定用户的全部登录会话。
     *
     * @param userId 用户 ID
     */
    void invalidateUser(Long userId);

    /**
     * 撤销指定登录会话。
     *
     * @param sessionId 会话 ID
     */
    void revokeSession(String sessionId);
}
