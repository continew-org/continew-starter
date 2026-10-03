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

import jakarta.annotation.PostConstruct;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import top.continew.starter.auth.refresh.token.api.AccessSessionValidator;
import top.continew.starter.auth.refresh.token.api.AuthSessionRevocationNotifier;
import top.continew.starter.auth.refresh.token.service.RefreshTokenService;
import top.continew.starter.auth.refresh.token.service.SessionInvalidationService;
import top.continew.starter.auth.refresh.token.service.SessionQueryService;
import top.continew.starter.auth.refresh.token.service.impl.RefreshTokenServiceImpl;
import top.continew.starter.auth.refresh.token.service.impl.SessionInvalidationServiceImpl;
import top.continew.starter.auth.refresh.token.service.impl.SessionQueryServiceImpl;
import top.continew.starter.auth.refresh.token.support.AccessSessionCache;
import top.continew.starter.auth.refresh.token.support.AuthPolicyWriteLockAspect;
import top.continew.starter.auth.refresh.token.support.RefreshSessionStore;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.auth.refresh.token.support.RefreshTokenRequestGuard;
import top.continew.starter.auth.refresh.token.support.TenantArgumentPolicyLockTargetResolver;
import top.continew.starter.auth.refresh.token.support.UserArgumentPolicyLockTargetResolver;
import top.continew.starter.auth.refresh.token.websocket.AuthWebSocketSessionService;
import top.continew.starter.auth.refresh.token.websocket.ConcurrentWebSocketSessionDao;
import top.continew.starter.auth.refresh.token.websocket.WebSocketClientServiceImpl;
import top.continew.starter.auth.refresh.token.websocket.WebSocketCredentialRegistry;
import top.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.messaging.websocket.autoconfigure.WebSocketAutoConfiguration;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;

/**
 * Refresh Token 认证会话自动配置。
 *
 * <p>提供登录会话的创建、原子轮换、重放保护、统一撤销与 WebSocket 实时联动。业务系统
 * 需要提供 {@code IssuedAccessToken} 签发适配（{@link top.continew.starter.auth.refresh.token.service.RefreshAccessTokenIssuer}
 * 的实现）和登录主体快照映射（{@code RefreshSessionPrincipal} 的实现），其余能力
 * 开箱即用。</p>
 *
 * <p><b>前置条件：</b>Access Token 的 {@code sid} 会话声明依赖 Sa-Token JWT 扩展参数
 * 能力，使用方必须集成 {@code sa-token-jwt}（本模块已传递声明）并开启 JWT 模式
 * （配置 {@code sa-token.jwt-secret-key} 等）。未开启 JWT 模式时
 * {@code StpUtil.getExtra} 会抛出 {@code ApiDisabledException}。</p>
 *
 * <p><b>副作用提示：</b>本自动配置先于 Starter 的 WebSocket 自动配置执行，会将默认的
 * 单连接 {@code WebSocketSessionDao} 替换为支持多标签页的
 * {@link top.continew.starter.auth.refresh.token.websocket.ConcurrentWebSocketSessionDao}，
 * 并接管 {@code WebSocketClientService}（握手时校验 Access Token 及其绑定的认证会话）。
 * 业务系统可按 {@code @ConditionalOnMissingBean} 约定覆盖。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
@AutoConfiguration(before = WebSocketAutoConfiguration.class,
    after = RedissonAutoConfiguration.class)
@ConditionalOnProperty(prefix = PropertiesConstants.AUTH_REFRESH_TOKEN,
    name = PropertiesConstants.ENABLED,
    havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(RedissonClient.class)
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class AuthRefreshAutoConfiguration {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(AuthRefreshAutoConfiguration.class);

    private final RefreshTokenProperties properties;

    public AuthRefreshAutoConfiguration(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    /**
     * 输出自动配置初始化日志与启动期安全提示。
     *
     * <p>Refresh Token 是长期凭证，生产环境必须通过 HTTPS + {@code Secure} 下发。
     * 未开启 {@code cookie-secure} 时要求显式声明 {@code cookie-insecure-allowed=true}
     * 才视为知情选择，否则启动即输出告警，把"默认不安全"变成"显式选择不安全"。</p>
     *
     * <p>本自动配置接管了 Starter 的默认 {@code WebSocketSessionDao} 与
     * {@code WebSocketClientService}（多标签页语义），此处一并提示；周期校验
     * （{@code @Scheduled}）依赖业务项目启用 {@code @EnableScheduling}，未启用时
     * 撤销广播与 Access Token 过期仍然生效。</p>
     */
    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'AuthRefreshToken' completed initialization.");
        if (!properties.isCookieSecure() && !properties.isCookieInsecureAllowed()) {
            LOGGER.warn("[ContiNew Starter] Refresh Token Cookie 未开启 Secure，长期凭证将以明文"
                + "信道传输，仅适用于本地开发。生产环境请开启 continew-starter.refresh-token."
                + "cookie-secure，或显式设置 continew-starter.refresh-token."
                + "cookie-insecure-allowed=true 以确认知情。");
        }
        LOGGER.info("[ContiNew Starter] - 已接管 Starter 默认 WebSocketSessionDao 与"
            + " WebSocketClientService（多标签页语义）；@Scheduled 周期校验需业务项目启用"
            + " @EnableScheduling 方可生效。");
    }

    /**
     * Refresh Token 编解码器。
     */
    @Bean
    @ConditionalOnMissingBean
    public RefreshTokenCodec refreshTokenCodec(RefreshTokenProperties properties) {
        return new RefreshTokenCodec(properties);
    }

    /**
     * Refresh Session Redis 仓储。
     */
    @Bean
    @ConditionalOnMissingBean
    public RefreshSessionStore refreshSessionStore(RedissonClient redissonClient) {
        return new RefreshSessionStore(redissonClient);
    }

    /**
     * Refresh Token HTTP 请求守卫。
     */
    @Bean
    @ConditionalOnMissingBean
    public RefreshTokenRequestGuard refreshTokenRequestGuard(RefreshTokenProperties properties,
        RefreshTokenCodec tokenCodec, RefreshSessionStore sessionStore) {
        return new RefreshTokenRequestGuard(properties, tokenCodec, sessionStore);
    }

    /**
     * Access Token 会话有效结论的本地缓存。
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessSessionCache accessSessionCache(RefreshTokenProperties properties,
        RedissonClient redissonClient,
        @Value("${spring.application.name:unknown}") String applicationName) {
        return new AccessSessionCache(properties, redissonClient, applicationName);
    }

    /**
     * Refresh Token 会话服务。
     */
    @Bean
    @ConditionalOnMissingBean(RefreshTokenService.class)
    public RefreshTokenService refreshTokenService(RefreshTokenProperties properties,
        RefreshTokenCodec tokenCodec, RefreshSessionStore sessionStore,
        RefreshTokenRequestGuard requestGuard,
        AuthSessionRevocationNotifier sessionRevocationNotifier,
        AccessSessionCache accessSessionCache) {
        return new RefreshTokenServiceImpl(properties, tokenCodec, sessionStore, requestGuard,
            sessionRevocationNotifier, accessSessionCache);
    }

    /**
     * 认证会话失效入口。
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionInvalidationService sessionInvalidationService(
        RefreshTokenService refreshTokenService) {
        return new SessionInvalidationServiceImpl(refreshTokenService);
    }

    /**
     * 认证会话查询服务。
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionQueryService sessionQueryService(RefreshTokenService refreshTokenService) {
        return new SessionQueryServiceImpl(refreshTokenService);
    }

    /**
     * 认证策略写锁切面。
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthPolicyWriteLockAspect authPolicyWriteLockAspect(
        ApplicationContext applicationContext,
        RefreshSessionStore sessionStore) {
        return new AuthPolicyWriteLockAspect(applicationContext, sessionStore);
    }

    /**
     * WebSocket 客户端标识到原始凭证的进程内登记。
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketCredentialRegistry webSocketCredentialRegistry() {
        return new WebSocketCredentialRegistry();
    }

    /**
     * WebSocket 连接凭证周期校验与撤销通知服务。
     */
    @Bean
    @ConditionalOnMissingBean(AuthSessionRevocationNotifier.class)
    public AuthWebSocketSessionService authWebSocketSessionService(RedissonClient redissonClient,
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider,
        ObjectProvider<AccessSessionValidator> accessSessionValidatorProvider,
        WebSocketCredentialRegistry credentialRegistry) {
        return new AuthWebSocketSessionService(redissonClient, sessionDaoProvider,
            accessSessionValidatorProvider, credentialRegistry);
    }

    /**
     * 当前登录用户 Provider：WebSocket 握手时校验 Access Token 及其绑定的认证会话，
     * 并以 Access Token 的服务端指纹作为客户端 ID（凭证与可记录标识分离）。
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketClientService.class)
    public WebSocketClientService webSocketClientService(
        AccessSessionValidator accessSessionValidator, RefreshTokenCodec tokenCodec,
        WebSocketCredentialRegistry credentialRegistry) {
        return new WebSocketClientServiceImpl(accessSessionValidator, tokenCodec,
            credentialRegistry);
    }

    /**
     * 支持多标签页的 WebSocket 会话 DAO。
     *
     * <p>本自动配置先于 Starter 的 WebSocket 自动配置执行，替换其默认的单连接覆盖
     * 语义：同一 Access Token 的多条连接互不覆盖，撤销时可全量关闭。</p>
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketSessionDao.class)
    public ConcurrentWebSocketSessionDao webSocketSessionDao() {
        return new ConcurrentWebSocketSessionDao();
    }

    /**
     * 用户维度认证策略锁目标解析器。
     */
    @Bean
    @ConditionalOnMissingBean
    public UserArgumentPolicyLockTargetResolver userArgumentPolicyLockTargetResolver() {
        return new UserArgumentPolicyLockTargetResolver();
    }

    /**
     * 租户维度认证策略锁目标解析器。
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantArgumentPolicyLockTargetResolver tenantArgumentPolicyLockTargetResolver() {
        return new TenantArgumentPolicyLockTargetResolver();
    }
}
