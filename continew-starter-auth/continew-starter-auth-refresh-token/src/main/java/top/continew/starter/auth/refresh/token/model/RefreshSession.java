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

import top.continew.starter.auth.refresh.token.enums.RefreshTokenModeEnum;

import java.io.Serial;
import java.io.Serializable;

/**
 * Redis 中保存的 Refresh Token 会话。
 *
 * <p>一条记录对应一次登录会话。Refresh Token 使用 {@code sessionId.secret} 格式，
 * Redis 只保存 secret 的指纹，轮换只需要原子更新本对象。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public class RefreshSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录会话 ID，同时是 Refresh Token 的公开定位部分。
     */
    private String sessionId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 登录时的用户名快照，用于登录会话管理。
     */
    private String username;

    /**
     * 登录时的用户昵称快照，用于登录会话管理。
     */
    private String nickname;

    /**
     * 客户端 ID
     */
    private String clientId;

    /**
     * 客户端类型，用于执行同类型设备互斥登录策略。
     */
    private String clientType;

    /**
     * 登录时确定的租户 ID，避免刷新时跨租户使用
     */
    private Long tenantId;

    /**
     * 当前客户端使用的 Refresh Token 传输模式
     */
    private RefreshTokenModeEnum mode;

    /**
     * 登录会话创建时间（毫秒时间戳），用于最大登录数量的稳定淘汰顺序。
     */
    private long createdAt;

    /**
     * 最近一次成功发起令牌轮换的时间（毫秒时间戳）。
     */
    private long lastRefreshAt;

    /**
     * 初次登录 IP。
     */
    private String ip;

    /**
     * 初次登录 IP 归属地。
     */
    private String address;

    /**
     * 初次登录浏览器或客户端。
     */
    private String browser;

    /**
     * 初次登录操作系统。
     */
    private String os;

    /**
     * 整个登录会话的绝对过期时间（毫秒时间戳），轮换不会无限延长会话寿命
     */
    private long expiresAt;

    /**
     * 创建会话时的用户安全版本。
     */
    private long userSecurityVersion;

    /**
     * 创建会话时的客户端安全版本。
     */
    private long clientSecurityVersion;

    /**
     * 创建会话时的租户安全版本。
     */
    private long tenantSecurityVersion;

    /**
     * 当前 Refresh Token secret 的 HMAC-SHA256 指纹。
     */
    private String currentTokenFingerprint;

    /**
     * 上一个 Refresh Token secret 的指纹，仅用于识别最近一次重放。
     */
    private String previousTokenFingerprint;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public RefreshTokenModeEnum getMode() {
        return mode;
    }

    public void setMode(RefreshTokenModeEnum mode) {
        this.mode = mode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }

    public void setLastRefreshAt(long lastRefreshAt) {
        this.lastRefreshAt = lastRefreshAt;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getUserSecurityVersion() {
        return userSecurityVersion;
    }

    public void setUserSecurityVersion(long userSecurityVersion) {
        this.userSecurityVersion = userSecurityVersion;
    }

    public long getClientSecurityVersion() {
        return clientSecurityVersion;
    }

    public void setClientSecurityVersion(long clientSecurityVersion) {
        this.clientSecurityVersion = clientSecurityVersion;
    }

    public long getTenantSecurityVersion() {
        return tenantSecurityVersion;
    }

    public void setTenantSecurityVersion(long tenantSecurityVersion) {
        this.tenantSecurityVersion = tenantSecurityVersion;
    }

    public String getCurrentTokenFingerprint() {
        return currentTokenFingerprint;
    }

    public void setCurrentTokenFingerprint(String currentTokenFingerprint) {
        this.currentTokenFingerprint = currentTokenFingerprint;
    }

    public String getPreviousTokenFingerprint() {
        return previousTokenFingerprint;
    }

    public void setPreviousTokenFingerprint(String previousTokenFingerprint) {
        this.previousTokenFingerprint = previousTokenFingerprint;
    }
}
