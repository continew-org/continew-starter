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

import top.continew.starter.auth.refresh.token.enums.LogoutReasonEnum;
import top.continew.starter.auth.refresh.token.enums.RefreshTokenModeEnum;
import top.continew.starter.auth.refresh.token.enums.SessionReplacementScope;

/**
 * 认证会话模块所需的客户端令牌策略。
 *
 * <p>该模型刻意不复用系统管理模块的 {@code ClientResp}，避免认证会话模块反向依赖
 * 用户、客户端等业务实体。</p>
 *
 * @param clientId            客户端 ID
 * @param clientType          客户端类型
 * @param refreshTokenTimeout Refresh Token 有效期（秒）
 * @param refreshTokenMode    Refresh Token 模式（固定/轮换）
 * @param concurrent          是否允许多端同时在线
 * @param replacementScope    会话顶替范围
 * @param maxLoginCount       最大登录数
 * @param overflowLogoutMode  超出登录数限制时的注销方式
 * @author luoqiz
 * @since 2.17.0
 */
public record RefreshClientPolicy(String clientId, String clientType, long refreshTokenTimeout,
    RefreshTokenModeEnum refreshTokenMode, boolean concurrent,
    SessionReplacementScope replacementScope, int maxLoginCount,
    LogoutReasonEnum overflowLogoutMode) {
}
