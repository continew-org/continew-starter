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
 * 轮换回调中由签发方回传的 Access Token 签发结果。
 *
 * <p>签发方（如业务系统的认证服务）只需提供这三项信息，Refresh Token 的新令牌签发、
 * Cookie 写入与轮换快照由本模块完成。</p>
 *
 * @param accessToken 签发的 Access Token
 * @param expiresIn   Access Token 有效期（秒）
 * @param tenantId    租户 ID，用于轮换结果回放时保持响应一致；无租户场景为 {@code null}
 * @author Charles7c
 * @since 2.17.0
 */
public record IssuedAccessToken(String accessToken, long expiresIn, Long tenantId) {
}
