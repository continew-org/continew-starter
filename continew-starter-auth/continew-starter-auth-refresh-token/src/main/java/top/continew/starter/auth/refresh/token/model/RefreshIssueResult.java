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
 * Refresh Token 签发或轮换完成后的结果。
 *
 * <p>调用方（通常是登录/刷新接口）将该结果映射为实际响应体。COOKIE 模式下
 * {@code refreshToken} 为 {@code null}，明文只写入 HttpOnly Cookie；宽限期内的并发
 * 重复请求会得到与首次完全相同的结果。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
public class RefreshIssueResult {

    /**
     * 签发的 Access Token
     */
    private String accessToken;

    /**
     * Access Token 类型，固定为 {@code Bearer}
     */
    private String tokenType;

    /**
     * Access Token 有效期（秒）
     */
    private Long expiresIn;

    /**
     * Refresh Token 有效期（秒），即当前登录会话的剩余寿命
     */
    private Long refreshExpiresIn;

    /**
     * BODY 模式下返回给客户端的明文 Refresh Token；COOKIE 模式为 {@code null}
     */
    private String refreshToken;

    /**
     * 租户 ID；无租户场景为 {@code null}
     */
    private Long tenantId;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(Long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
