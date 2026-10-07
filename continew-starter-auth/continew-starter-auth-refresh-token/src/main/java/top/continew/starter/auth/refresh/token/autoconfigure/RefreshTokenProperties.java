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

package top.continew.starter.auth.refresh.token.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.continew.starter.auth.refresh.token.support.OriginUtils;
import top.continew.starter.core.constant.PropertiesConstants;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Refresh Token 配置。
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
@Validated
@ConfigurationProperties(prefix = PropertiesConstants.AUTH_REFRESH_TOKEN)
public class RefreshTokenProperties {

    private static final String DNS_LABEL = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";
    /**
     * 子域通配符正则（匹配 {@code http[s]://*.example.com}）。
     *
     * <p>标签列表用独占量词避免大输入回溯：合法的 DNS 主机名不会被正则引擎
     * 反复重试，恶意输入无法构造出指数级回溯的栈溢出路径。</p>
     */
    private static final Pattern WILDCARD_COOKIE_ORIGIN = Pattern.compile(
        "^https?://\\*\\." + DNS_LABEL + "(?:\\." + DNS_LABEL + ")++$",
        Pattern.CASE_INSENSITIVE);

    /**
     * 是否启用 Refresh Token 登录会话。
     */
    private boolean enabled = true;

    /**
     * Refresh Token 服务端密钥。
     *
     * <p>用于派生 Token 指纹密钥和轮换快照加密密钥。生产环境必须通过环境变量单独配置
     * 高熵随机值，且禁止与 Access Token 的 JWT 密钥复用。</p>
     */
    @NotBlank(message = "Refresh Token 服务端密钥不能为空")
    @Size(min = 32, message = "Refresh Token 服务端密钥长度不能少于 32 个字符")
    private String secret;

    /**
     * 浏览器 Refresh Token Cookie 名称
     */
    @NotBlank(message = "Refresh Token Cookie 名称不能为空")
    private String cookieName = "refresh_token";

    /**
     * Cookie 作用路径。
     *
     * <p>前端开发环境通常通过 /api 或 /dev-api 代理访问后端，浏览器判断 Cookie
     * Path 时使用的是代理后的前端 URL，因此不能设置为 /auth，否则刷新请求不会携带
     * Cookie。生产环境如使用固定网关前缀，可通过配置覆盖该值。</p>
     */
    @NotBlank(message = "Refresh Token Cookie Path 不能为空")
    private String cookiePath = "/";

    /**
     * 生产环境必须开启 Secure；开发环境可关闭以支持 HTTP 本地调试
     */
    private boolean cookieSecure;

    /**
     * 显式确认接受不安全的 Refresh Token Cookie（仅本地开发）。
     *
     * <p>{@code cookie-secure=false} 时若未显式开启本项，启动即输出告警，把"默认不安全"
     * 变成"显式选择不安全"。生产环境切勿开启。</p>
     */
    private boolean cookieInsecureAllowed;

    /**
     * Cookie SameSite 属性
     */
    private String cookieSameSite = "Lax";

    /**
     * 允许携带 Refresh Token Cookie 的跨源前端 Origin。
     *
     * <p>空列表表示只允许请求自身同源。每一项可以是无路径、查询和片段的明确 HTTP(S)
     * Origin，或格式为 {@code http[s]://*.example.com} 的子域通配符。通配符仅匹配最左侧
     * 的一个 DNS 标签，例如 {@code http://*.luoqiz.top} 可匹配
     * {@code http://admin.luoqiz.top}。</p>
     */
    private List<String> cookieAllowedOrigins = new ArrayList<>();

    /**
     * 同一个旧 Token 的并发请求可重放第一次轮换结果的时间（秒）。
     *
     * <p>默认 30 秒，覆盖移动端弱网与小程序的重试窗口（OAuth 2.0 Security BCP
     * §4.14 要求宽限期覆盖客户端网络延迟与重试）。</p>
     */
    @Min(value = 1, message = "Refresh Token 轮换宽限期不能少于 1 秒")
    @Max(value = 120, message = "Refresh Token 轮换宽限期不能超过 120 秒")
    private int rotationGracePeriod = 30;

    /**
     * 未完成轮换的待恢复快照保留时间（秒）。
     *
     * <p>轮换在"会话已切代、Access Token 尚未签发完成"时进程退出，客户端只能用旧
     * Token 重试恢复；此时会话锁为 watchdog 模式，崩溃节点的锁租约（Redisson 默认
     * 30 秒）耗尽前其他节点无法进入恢复分支。本值必须覆盖锁残留与客户端合理重试窗口，
     * 显著大于幂等宽限期（{@code rotationGracePeriod}）；轮换成功完成后快照即切换为
     * 短期幂等语义，不影响重放攻击判定。</p>
     */
    @Min(value = 10, message = "Refresh Token 待恢复快照保留时间不能少于 10 秒")
    @Max(value = 3600, message = "Refresh Token 待恢复快照保留时间不能超过 3600 秒")
    private int rotationPendingPeriod = 60;

    /**
     * 刷新接口单个 IP 在限流窗口内允许的最大请求数，0 表示交由网关限流。
     */
    @Min(value = 0, message = "Refresh Token IP 限流次数不能小于 0")
    @Max(value = 10000, message = "Refresh Token IP 限流次数不能超过 10000")
    private int ipRateLimit = 60;

    /**
     * Refresh Token IP 限流窗口（秒）。
     */
    @Min(value = 1, message = "Refresh Token IP 限流窗口不能少于 1 秒")
    @Max(value = 3600, message = "Refresh Token IP 限流窗口不能超过 3600 秒")
    private int ipRateLimitPeriod = 60;

    /**
     * 单个登录会话在限流窗口内允许的最大刷新次数，0 表示交由网关限流。
     */
    @Min(value = 0, message = "Refresh Token 会话限流次数不能小于 0")
    @Max(value = 1000, message = "Refresh Token 会话限流次数不能超过 1000")
    private int sessionRateLimit = 10;

    /**
     * 单个登录会话刷新限流窗口（秒）。
     */
    @Min(value = 1, message = "Refresh Token 会话限流窗口不能少于 1 秒")
    @Max(value = 3600, message = "Refresh Token 会话限流窗口不能超过 3600 秒")
    private int sessionRateLimitPeriod = 60;

    /**
     * 允许解析转发地址的反向代理地址列表；空列表时始终使用连接对端地址。
     */
    private List<String> trustedProxyAddresses = new ArrayList<>();

    /**
     * 可信代理追加到 X-Forwarded-For 的跳数；0 表示不解析转发地址。
     */
    @Min(value = 0, message = "可信代理跳数不能小于 0")
    @Max(value = 10, message = "可信代理跳数不能超过 10")
    private int trustedProxyHops;

    /**
     * 热路径会话校验本地缓存开关。
     *
     * <p>开启后，每个已登录请求的会话校验命中本地缓存为 0 次 Redis 往返；会话撤销经
     * 广播子秒级失效，最坏情况由本地缓存 TTL（2 秒）兜底。关闭则恢复逐请求实时校验
     * （回源包含会话读取、登录态核对与安全版本批量核对，撤销立即生效）。</p>
     */
    private boolean accessSessionCacheEnabled = true;

    /**
     * 会话失效广播 Topic 名称（显式覆盖）。
     *
     * <p>留空（默认）时使用 {@code auth:access-session-invalid:{spring.application.name}}：
     * 同一服务多副本共用 Topic、不同服务天然隔离。仅当多个服务有意共享同一认证会话域
     * （例如网关与资源服务拆分、共用同一套 Refresh Session）时才显式配置为公共 Topic。
     * 必须与 JetCache 的 broadcastChannel 取值不同，二者消息载荷不兼容。</p>
     */
    private String accessSessionInvalidTopic = "";

    /**
     * 本实例 WebSocket 连接凭证周期校验间隔（毫秒）。
     *
     * <p>兜底 Redis Pub/Sub 撤销消息丢失和 Access Token 自然过期；默认 60 秒，
     * 与撤销广播配合时，未能及时收到通知的连接最迟在该间隔内被关闭。</p>
     */
    @Min(value = 1000, message = "WebSocket 连接校验间隔不能少于 1000 毫秒")
    @Max(value = 3600000, message = "WebSocket 连接校验间隔不能超过 3600000 毫秒")
    private long websocketValidationInterval = 60000;

    /**
     * SameSite 取值必须是 Strict、Lax 或 None（不区分大小写）。
     *
     * @return 校验通过返回 {@code true}
     */
    @AssertTrue(message = "Cookie SameSite 只能是 Strict、Lax 或 None")
    public boolean isCookieSameSiteValid() {
        return "Strict".equalsIgnoreCase(cookieSameSite) || "Lax".equalsIgnoreCase(cookieSameSite)
            || "None".equalsIgnoreCase(cookieSameSite);
    }

    /**
     * SameSite=None 只有在 HTTPS 下配合 Secure 才能被浏览器接受。
     *
     * @return 校验通过返回 {@code true}
     */
    @AssertTrue(message = "Cookie SameSite=None 时必须同时开启 Secure")
    public boolean isCookieSecurityValid() {
        return !"None".equalsIgnoreCase(cookieSameSite) || cookieSecure;
    }

    /**
     * __Host- Cookie 必须满足浏览器规定的 Secure + Path=/ 约束。
     *
     * @return 校验通过返回 {@code true}
     */
    @AssertTrue(message = "__Host- Refresh Token Cookie 必须开启 Secure 且 Path=/")
    public boolean isHostCookieValid() {
        return !cookieName.startsWith("__Host-") || cookieSecure && "/".equals(cookiePath);
    }

    /**
     * Cookie 来源白名单必须是严格 HTTP(S) Origin 或受限的子域通配符。
     *
     * @return 校验通过返回 {@code true}
     */
    @AssertTrue(
        message = "Refresh Token Cookie 允许来源必须是明确的 HTTP(S) Origin 或 http[s]://*.example.com 格式的子域通配符")
    public boolean isCookieAllowedOriginsValid() {
        return cookieAllowedOrigins != null && cookieAllowedOrigins.stream()
            .allMatch(this::isValidAllowedOrigin);
    }

    /**
     * 校验单个 Cookie 来源白名单项是否合法。
     *
     * @param value 待校验的 Origin 字符串
     * @return 合法返回 {@code true}
     */
    private boolean isValidAllowedOrigin(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
            || "*".equals(value)) {
            return false;
        }
        return OriginUtils.isValidOrigin(value) || WILDCARD_COOKIE_ORIGIN.matcher(value)
            .matches();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public boolean isCookieInsecureAllowed() {
        return cookieInsecureAllowed;
    }

    public void setCookieInsecureAllowed(boolean cookieInsecureAllowed) {
        this.cookieInsecureAllowed = cookieInsecureAllowed;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }

    public List<String> getCookieAllowedOrigins() {
        return cookieAllowedOrigins;
    }

    public void setCookieAllowedOrigins(List<String> cookieAllowedOrigins) {
        this.cookieAllowedOrigins = cookieAllowedOrigins;
    }

    public int getRotationGracePeriod() {
        return rotationGracePeriod;
    }

    public void setRotationGracePeriod(int rotationGracePeriod) {
        this.rotationGracePeriod = rotationGracePeriod;
    }

    public int getRotationPendingPeriod() {
        return rotationPendingPeriod;
    }

    public void setRotationPendingPeriod(int rotationPendingPeriod) {
        this.rotationPendingPeriod = rotationPendingPeriod;
    }

    public int getIpRateLimit() {
        return ipRateLimit;
    }

    public void setIpRateLimit(int ipRateLimit) {
        this.ipRateLimit = ipRateLimit;
    }

    public int getIpRateLimitPeriod() {
        return ipRateLimitPeriod;
    }

    public void setIpRateLimitPeriod(int ipRateLimitPeriod) {
        this.ipRateLimitPeriod = ipRateLimitPeriod;
    }

    public int getSessionRateLimit() {
        return sessionRateLimit;
    }

    public void setSessionRateLimit(int sessionRateLimit) {
        this.sessionRateLimit = sessionRateLimit;
    }

    public int getSessionRateLimitPeriod() {
        return sessionRateLimitPeriod;
    }

    public void setSessionRateLimitPeriod(int sessionRateLimitPeriod) {
        this.sessionRateLimitPeriod = sessionRateLimitPeriod;
    }

    public List<String> getTrustedProxyAddresses() {
        return trustedProxyAddresses;
    }

    public void setTrustedProxyAddresses(List<String> trustedProxyAddresses) {
        this.trustedProxyAddresses = trustedProxyAddresses;
    }

    public int getTrustedProxyHops() {
        return trustedProxyHops;
    }

    public void setTrustedProxyHops(int trustedProxyHops) {
        this.trustedProxyHops = trustedProxyHops;
    }

    public boolean isAccessSessionCacheEnabled() {
        return accessSessionCacheEnabled;
    }

    public void setAccessSessionCacheEnabled(boolean accessSessionCacheEnabled) {
        this.accessSessionCacheEnabled = accessSessionCacheEnabled;
    }

    public String getAccessSessionInvalidTopic() {
        return accessSessionInvalidTopic;
    }

    public void setAccessSessionInvalidTopic(String accessSessionInvalidTopic) {
        this.accessSessionInvalidTopic = accessSessionInvalidTopic;
    }

    public long getWebsocketValidationInterval() {
        return websocketValidationInterval;
    }

    public void setWebsocketValidationInterval(long websocketValidationInterval) {
        this.websocketValidationInterval = websocketValidationInterval;
    }
}
