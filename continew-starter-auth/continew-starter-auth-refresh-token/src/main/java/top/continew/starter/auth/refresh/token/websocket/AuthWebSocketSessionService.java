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

package top.continew.starter.auth.refresh.token.websocket;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import top.continew.starter.auth.refresh.token.api.AccessSessionValidator;
import top.continew.starter.auth.refresh.token.api.AuthSessionRevocationNotifier;
import top.continew.starter.auth.refresh.token.support.AccessTokenClaimsReader;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refresh Session 与 WebSocket 连接联动服务。
 *
 * <p>WebSocket 客户端 ID 是 Access Token 的服务端指纹（不可逆，可安全进入 DAO 索引
 * 与生命周期日志）。本服务经 {@link WebSocketCredentialRegistry} 反查原始 Token，通过
 * 其 {@code sid} 找到同一 Refresh Session 的连接，并使用 Redis Topic 通知所有应用实例，
 * 避免用户禁用、强退或密码修改后已建立的连接继续存活。</p>
 *
 * <p>周期校验基于 Spring 的任务调度（{@link Scheduled}），需业务项目启用
 * {@code @EnableScheduling}；未启用时撤销广播与 Access Token 过期仍然生效。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public class AuthWebSocketSessionService implements AuthSessionRevocationNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthWebSocketSessionService.class);

    private static final String REVOCATION_TOPIC = "AUTH:REFRESH:V1:WEBSOCKET:REVOKED";

    private final RedissonClient redissonClient;
    private final ObjectProvider<WebSocketSessionDao> sessionDaoProvider;
    private final ObjectProvider<AccessSessionValidator> accessSessionValidatorProvider;
    private final WebSocketCredentialRegistry credentialRegistry;

    /**
     * 本实例客户端标识 → Refresh Session ID 的本地索引，避免每次撤销通知回源 Redis。
     */
    private final Map<String, String> tokenSessionIdCache = new ConcurrentHashMap<>();

    private RTopic revocationTopic;
    private Integer listenerId;

    public AuthWebSocketSessionService(RedissonClient redissonClient,
        ObjectProvider<WebSocketSessionDao> sessionDaoProvider,
        ObjectProvider<AccessSessionValidator> accessSessionValidatorProvider,
        WebSocketCredentialRegistry credentialRegistry) {
        this.redissonClient = redissonClient;
        this.sessionDaoProvider = sessionDaoProvider;
        this.accessSessionValidatorProvider = accessSessionValidatorProvider;
        this.credentialRegistry = credentialRegistry;
    }

    /**
     * 订阅集群内 Refresh Session 撤销通知。
     */
    @PostConstruct
    public void subscribe() {
        revocationTopic = redissonClient.getTopic(REVOCATION_TOPIC);
        listenerId = revocationTopic.addListener(String.class, (channel, sessionId) -> {
            try {
                this.closeLocal(sessionId);
            } catch (RuntimeException e) {
                // 与 notifyRevoked 的降级策略一致：DAO/Redis 抖动时不阻断监听器，
                // 本次撤销消息丢失由周期校验与本地缓存 TTL 兜底。
                LOGGER.warn("收到撤销通知后关闭会话 [{}] 的本地 WebSocket 连接失败", sessionId, e);
            }
        });
    }

    /**
     * 释放 Redis Topic 监听器。
     */
    @PreDestroy
    public void unsubscribe() {
        if (revocationTopic != null && listenerId != null) {
            revocationTopic.removeListener(listenerId);
        }
    }

    /**
     * 关闭本实例连接并通知集群内其他实例关闭同一登录会话的连接。
     *
     * @param sessionId Refresh Session ID
     */
    @Override
    public void notifyRevoked(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            revocationTopic.publish(sessionId);
        } catch (RuntimeException e) {
            // Refresh Session 已经由认证服务删除，实时通知只是加速关闭连接的旁路机制。
            LOGGER.warn("发布 Refresh Session [{}] 的 WebSocket 撤销通知失败", sessionId, e);
        }
        try {
            this.closeLocal(sessionId);
        } catch (RuntimeException e) {
            LOGGER.warn("关闭 Refresh Session [{}] 的本地 WebSocket 连接失败", sessionId, e);
        }
    }

    /**
     * 周期校验本实例的 WebSocket 凭证，兜底 Redis Pub/Sub 丢消息和 Token 自然过期。
     */
    @Scheduled(
        fixedDelayString = "${continew-starter.refresh-token.websocket-validation-interval:60000}")
    public void validateLocalSessions() {
        WebSocketSessionDao sessionDao = sessionDaoProvider.getIfAvailable();
        AccessSessionValidator accessSessionValidator =
            accessSessionValidatorProvider.getIfAvailable();
        if (sessionDao == null || accessSessionValidator == null) {
            return;
        }
        for (String clientId : new LinkedHashSet<>(sessionDao.listAllSessionIds())) {
            try {
                String rawToken = credentialRegistry.resolve(clientId);
                if (rawToken == null) {
                    // 凭证登记缺失（如自定义 DAO 存在跨进程残留 Key）时无法校验，
                    // 保守保留连接，交由撤销广播与 Access Token 自然过期兜底。
                    continue;
                }
                if (accessSessionValidator.isInvalid(rawToken)) {
                    this.closeLocalAccessToken(sessionDao, clientId);
                    this.tokenSessionIdCache.remove(clientId);
                }
            } catch (RuntimeException e) {
                // 基础设施短暂异常时保留连接，下一轮继续校验，避免误杀全部实时连接。
                LOGGER.warn("校验 WebSocket 客户端 [{}] 的连接凭证失败", clientId, e);
            }
        }
        // 连接已关闭的客户端不再保留本地索引与凭证登记，避免缓存无限增长。
        Set<String> onlineClientIds = new LinkedHashSet<>(sessionDao.listAllSessionIds());
        this.tokenSessionIdCache.keySet()
            .removeIf(clientId -> !onlineClientIds.contains(clientId));
        this.credentialRegistry.retainAll(onlineClientIds);
    }

    private void closeLocal(String sessionId) {
        WebSocketSessionDao sessionDao = sessionDaoProvider.getIfAvailable();
        if (sessionDao == null) {
            return;
        }
        // DAO 的 Key 是握手时登记的客户端标识。复制一份，避免关闭回调同步删除时
        // 修改正在遍历的集合。
        Set<String> clientIds = new LinkedHashSet<>(sessionDao.listAllSessionIds());
        for (String clientId : clientIds) {
            if (!this.belongsToSession(clientId, sessionId)) {
                continue;
            }
            this.closeLocalAccessToken(sessionDao, clientId);
            this.tokenSessionIdCache.remove(clientId);
        }
        // 连接已关闭的客户端不再保留本地索引条目，避免缓存无限增长。
        this.tokenSessionIdCache.keySet().removeIf(clientId -> !clientIds.contains(clientId));
    }

    private void closeLocalAccessToken(WebSocketSessionDao sessionDao, String clientId) {
        if (sessionDao instanceof MultiWebSocketSessionDao multiSessionDao) {
            // 原子摘取后关闭：摘取与索引移除是同一步操作，摘取之后新增的连接进入
            // 全新登记，既不会被误关也不会脱离撤销与周期校验索引。
            for (WebSocketSession session : multiSessionDao.removeAll(clientId)) {
                this.closeSession(session);
            }
            return;
        }
        this.closeSession(sessionDao.get(clientId));
        sessionDao.delete(clientId);
    }

    private void closeSession(WebSocketSession webSocketSession) {
        if (webSocketSession == null) {
            return;
        }
        try {
            if (webSocketSession.isOpen()) {
                webSocketSession.close(CloseStatus.POLICY_VIOLATION);
            }
        } catch (IOException e) {
            LOGGER.warn("关闭失效认证会话的 WebSocket 连接失败", e);
        }
    }

    private boolean belongsToSession(String clientId, String sessionId) {
        String cachedSessionId = tokenSessionIdCache.get(clientId);
        if (cachedSessionId != null) {
            return Objects.equals(sessionId, cachedSessionId);
        }
        String rawToken = credentialRegistry.resolve(clientId);
        if (rawToken == null) {
            return false;
        }
        String claimedSessionId = AccessTokenClaimsReader.resolveSessionIdQuietly(rawToken);
        // Access Token 的会话声明在签发后不可变，可以安全缓存，批量撤销时
        // 只需首次回源 Redis，后续通知全部命中本地索引。
        if (claimedSessionId != null) {
            tokenSessionIdCache.put(clientId, claimedSessionId);
        }
        return Objects.equals(sessionId, claimedSessionId);
    }
}
