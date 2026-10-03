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
 * 可供系统管理模块使用的认证会话安全视图。
 *
 * <p>不包含 Refresh Token、指纹、安全版本和轮换快照。字段与 {@link RefreshSession}
 * 的非敏感子集同名同义，setter 仅供映射框架填充；对外语义经 {@link RefreshSession#toView()}
 * 工厂方法显式建立，不与实体共享访问器实现。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public class SessionView {

    private final String sessionId;
    private final Long userId;
    private final String username;
    private final String nickname;
    private final String clientId;
    private final String clientType;
    private final Long tenantId;
    private final long createdAt;
    private final long lastRefreshAt;
    private final String ip;
    private final String address;
    private final String browser;
    private final String os;
    private final long expiresAt;

    private SessionView(RefreshSession session) {
        this.sessionId = session.getSessionId();
        this.userId = session.getUserId();
        this.username = session.getUsername();
        this.nickname = session.getNickname();
        this.clientId = session.getClientId();
        this.clientType = session.getClientType();
        this.tenantId = session.getTenantId();
        this.createdAt = session.getCreatedAt();
        this.lastRefreshAt = session.getLastRefreshAt();
        this.ip = session.getIp();
        this.address = session.getAddress();
        this.browser = session.getBrowser();
        this.os = session.getOs();
        this.expiresAt = session.getExpiresAt();
    }

    /**
     * 从 Refresh Session 实体构建安全视图（仅复制非敏感字段）。
     *
     * @param session Refresh Session 实体
     * @return 安全视图
     */
    public static SessionView from(RefreshSession session) {
        return new SessionView(session);
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientType() {
        return clientType;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }

    public String getIp() {
        return ip;
    }

    public String getAddress() {
        return address;
    }

    public String getBrowser() {
        return browser;
    }

    public String getOs() {
        return os;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
