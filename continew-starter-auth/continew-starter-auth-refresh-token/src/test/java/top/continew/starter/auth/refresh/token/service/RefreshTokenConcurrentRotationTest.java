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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.auth.refresh.token.api.AuthSessionRevocationNotifier;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.enums.RefreshTokenModeEnum;
import top.continew.starter.auth.refresh.token.model.AuthSecurityVersion;
import top.continew.starter.auth.refresh.token.model.IssuedAccessToken;
import top.continew.starter.auth.refresh.token.model.RefreshIssueResult;
import top.continew.starter.auth.refresh.token.model.RefreshRotationResult;
import top.continew.starter.auth.refresh.token.model.RefreshSession;
import top.continew.starter.auth.refresh.token.service.impl.RefreshTokenServiceImpl;
import top.continew.starter.auth.refresh.token.support.AccessSessionCache;
import top.continew.starter.auth.refresh.token.support.AuthPolicyLock;
import top.continew.starter.auth.refresh.token.support.RefreshSessionStore;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec.IssuedToken;
import top.continew.starter.auth.refresh.token.support.RefreshTokenRequestGuard;
import top.continew.starter.cache.redisson.util.RedisLockUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Refresh Token 轮换并发窗口测试：多线程同时出示同一代 Refresh Token。
 *
 * <p>硬契约（不依赖时序）：一轮并发中恰好前进一代，全部成功请求收到同一组
 * Access/Refresh Token；宽限期内无第二次代际前进。并发争抢结果由 Session 锁
 * 串行化决定，断言只锁集合级别的总量与一致性，不锁任何时序。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("Refresh Token 并发轮换窗口")
class RefreshTokenConcurrentRotationTest {

    private static final int CONCURRENCY = 8;

    private final Map<String, RefreshRotationResult> rotations = new ConcurrentHashMap<>();
    private final Map<String, RefreshRotationResult> latestRotations = new ConcurrentHashMap<>();
    private RefreshTokenProperties properties;
    private RefreshTokenCodec codec;
    private RefreshSessionStore sessionStore;
    private RefreshSession session;
    private RefreshTokenServiceImpl service;
    private IssuedToken initialToken;
    private AtomicInteger securityVersion;

    /**
     * 组装线程安全的内存仓储：Session 记录、轮换快照与安全版本全部原子化，
     * 分布式锁退化为空操作，让并发请求真正进入产品代码的窗口竞争。
     */
    private void setUpConcurrentService() {
        properties = new RefreshTokenProperties();
        properties.setSecret("concurrent-rotation-test-secret-with-32-bytes!");
        properties.setIpRateLimit(0);
        properties.setSessionRateLimit(100);
        properties.setRotationGracePeriod(5);
        codec = new RefreshTokenCodec(properties);

        sessionStore = Mockito.mock(RefreshSessionStore.class);
        AuthPolicyLock policyLock = Mockito.mock(AuthPolicyLock.class);
        // Session 锁是轮换窗口互斥的真实载体：以本地 ReentrantLock 等价替换 Redis 分布式锁，
        // 空实现会让并发线程同时进入临界区，测的就不是产品契约了
        ConcurrentMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
        Mockito.when(sessionStore.lockSession(Mockito.anyString())).thenAnswer(invocation -> {
            String sessionId = invocation.getArgument(0, String.class);
            ReentrantLock lock =
                sessionLocks.computeIfAbsent(sessionId, key -> new ReentrantLock());
            lock.lock();
            RedisLockUtils handle = Mockito.mock(RedisLockUtils.class);
            Mockito.doAnswer(release -> {
                lock.unlock();
                return null;
            }).when(handle).close();
            return handle;
        });
        Mockito.when(sessionStore.lockUserPolicy(Mockito.anyLong())).thenReturn(policyLock);
        Mockito.when(sessionStore.lockClientPolicyRead(Mockito.anyString()))
            .thenReturn(policyLock);
        Mockito.when(sessionStore.tryAcquireRateLimit(Mockito.anyString(), Mockito.anyInt(),
            Mockito.any(Duration.class))).thenReturn(true);

        securityVersion = new AtomicInteger();
        Mockito.when(sessionStore.getSecurityVersion(Mockito.anyLong(), Mockito.anyString(),
            Mockito.any()))
            .thenAnswer(invocation -> new AuthSecurityVersion(securityVersion.get(),
                securityVersion.get(), securityVersion.get()));
        Mockito.when(sessionStore.isSecurityVersionCurrent(Mockito.any(RefreshSession.class)))
            .thenAnswer(invocation -> {
                RefreshSession candidate = invocation.getArgument(0, RefreshSession.class);
                return candidate.getUserSecurityVersion() == securityVersion.get()
                    && candidate.getClientSecurityVersion() == securityVersion.get()
                    && candidate.getTenantSecurityVersion() == securityVersion.get();
            });

        Mockito.when(sessionStore.getRotation(Mockito.anyString()))
            .thenAnswer(invocation -> rotations.get(invocation.getArgument(0, String.class)));
        Mockito.doAnswer(invocation -> rotations.put(invocation.getArgument(0, String.class),
            invocation.getArgument(1, RefreshRotationResult.class)))
            .when(sessionStore)
            .saveRotation(Mockito.anyString(), Mockito.any(RefreshRotationResult.class),
                Mockito.anyLong());
        Mockito.when(sessionStore.getLatestRotation(Mockito.anyString()))
            .thenAnswer(invocation -> latestRotations
                .get(invocation.getArgument(0, String.class)));
        Mockito.doAnswer(invocation -> latestRotations.put(invocation.getArgument(0, String.class),
            invocation.getArgument(1, RefreshRotationResult.class)))
            .when(sessionStore)
            .saveLatestRotation(Mockito.anyString(), Mockito.any(RefreshRotationResult.class),
                Mockito.anyLong());

        ConcurrentMap<String, RefreshSession> sessions = new ConcurrentHashMap<>();
        Mockito.when(sessionStore.get(Mockito.anyString())).thenAnswer(invocation -> sessions
            .get(invocation.getArgument(0, String.class)));
        Mockito.doAnswer(invocation -> {
            RefreshSession saved = invocation.getArgument(0, RefreshSession.class);
            return sessions.put(saved.getSessionId(), saved);
        }).when(sessionStore).save(Mockito.any(RefreshSession.class));
        Mockito.doAnswer(invocation -> sessions.remove(invocation.getArgument(0, String.class)))
            .when(sessionStore)
            .delete(Mockito.anyString());
        Mockito.when(sessionStore.findByUser(Mockito.anyLong())).thenReturn(Set.of());

        // 测试需要直接操作初始 Session，登记入口与产品代码一致走 sessions Map
        this.session = registerSession(sessions, codec.issue(codec.newSessionId()));
        this.initialToken = codec.issue(this.session.getSessionId());
        this.session.setCurrentTokenFingerprint(initialToken.fingerprint());

        RefreshTokenRequestGuard requestGuard = new RefreshTokenRequestGuard(properties, codec,
            sessionStore);
        List<AuthSessionRevocationNotifier> notifiers = List.of();
        AccessSessionCache accessSessionCache = Mockito.mock(AccessSessionCache.class);
        service = new RefreshTokenServiceImpl(properties, codec, sessionStore, requestGuard,
            notifiers, accessSessionCache);
    }

    /**
     * 构建并登记一个 BODY 模式的测试会话。
     *
     * @param sessions 并发会话登记表
     * @param token    会话初始令牌
     * @return 已登记的会话
     */
    private RefreshSession registerSession(ConcurrentMap<String, RefreshSession> sessions,
        IssuedToken token) {
        RefreshSession refreshSession = new RefreshSession();
        refreshSession.setSessionId(token.sessionId());
        refreshSession.setUserId(1L);
        refreshSession.setClientId("web");
        refreshSession.setMode(RefreshTokenModeEnum.BODY);
        refreshSession.setExpiresAt(System.currentTimeMillis() + Duration.ofDays(1).toMillis());
        refreshSession.setUserSecurityVersion(0);
        refreshSession.setClientSecurityVersion(0);
        refreshSession.setTenantSecurityVersion(0);
        refreshSession.setCurrentTokenFingerprint(token.fingerprint());
        sessions.put(token.sessionId(), refreshSession);
        return refreshSession;
    }

    /**
     * 并发出示同一旧代 Token：代际恰好前进一次，全部成功响应持有同一组新凭证。
     */
    @RepeatedTest(value = 10, name = "{currentRepetition}/{totalRepetitions}")
    @DisplayName("并发同代轮换：恰好前进一代且全部幂等同结果")
    void shouldAdvanceExactlyOneGenerationUnderConcurrentRotation() throws Exception {
        setUpConcurrentService();
        runConcurrently(CONCURRENCY, () -> initialToken.rawToken(), result -> {
            issuerInvocations.incrementAndGet();
            return new IssuedAccessToken("access-" + issuerInvocations.get(), 900L, null);
        });
    }

    private final AtomicInteger issuerInvocations = new AtomicInteger();

    /**
     * 宽限期内旧/新代 Token 混合并发重放：全员收敛到首次轮换的同一组凭证。
     */
    @Test
    @DisplayName("宽限期内新旧代 Token 并发重放：全员幂等且代际不再前进")
    void shouldReplaySameResultForConcurrentTokensWithinGracePeriod() throws Exception {
        setUpConcurrentService();
        RefreshIssueResult first = service.rotate(initialToken.rawToken(),
            new MockHttpServletResponse(), ignored -> new IssuedAccessToken("access-1", 900L,
                null));
        String oldToken = initialToken.rawToken();
        String newRefreshToken = first.getRefreshToken();

        Set<String> accessTokens = ConcurrentHashMap.newKeySet();
        Set<String> refreshTokens = ConcurrentHashMap.newKeySet();
        AtomicInteger done = new AtomicInteger();
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<?>> futures = new ArrayList<>(CONCURRENCY);
        try {
            for (int i = 0; i < CONCURRENCY; i++) {
                String presented = i % 2 == 0 ? oldToken : newRefreshToken;
                futures.add(executor.submit(() -> {
                    try {
                        assertTrue(startLatch.await(5, TimeUnit.SECONDS), "启动信号未就位");
                        RefreshIssueResult result = service.rotate(presented,
                            new MockHttpServletResponse(), ignored -> {
                                throw new AssertionError("宽限期内不应再次签发 Access Token");
                            });
                        accessTokens.add(result.getAccessToken());
                        refreshTokens.add(result.getRefreshToken());
                        return null;
                    } finally {
                        done.incrementAndGet();
                    }
                }));
            }
            startLatch.countDown();
            awaitAll(executor, futures, done, CONCURRENCY);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, accessTokens.size(), "宽限期重放的 Access Token 种类数");
        assertEquals(1, refreshTokens.size(), "宽限期重放的 Refresh Token 种类数");
        assertEquals(first.getAccessToken(), accessTokens.iterator().next());
        assertEquals(newRefreshToken, refreshTokens.iterator().next());
        // 代际未前进：Session 的 current 指纹仍是首次轮换后的新代
        assertEquals(codec.parse(newRefreshToken).fingerprint(), session
            .getCurrentTokenFingerprint());
    }

    /**
     * 多个 Session 的并发组互不串扰：每个 Session 各自恰好前进一代、签发一次。
     */
    @RepeatedTest(value = 5, name = "{currentRepetition}/{totalRepetitions}")
    @DisplayName("多 Session 并发轮换互不串扰")
    void shouldRotateIndependentSessionsConcurrentlyWithoutCrosstalk() throws Exception {
        setUpConcurrentService();
        int sessionCount = 4;
        ConcurrentMap<String, RefreshSession> sessions = new ConcurrentHashMap<>();
        Map<String, IssuedToken> tokens = new ConcurrentHashMap<>();
        for (int i = 0; i < sessionCount; i++) {
            IssuedToken token = codec.issue(codec.newSessionId());
            RefreshSession isolated = registerSession(sessions, token);
            isolated.setUserId((long) i);
            tokens.put(token.sessionId(), token);
        }
        // 多 Session 场景把仓储读取重指向本测试的登记表（setUp 桩指向其自建 Map）
        Mockito.when(sessionStore.get(Mockito.anyString())).thenAnswer(invocation -> sessions
            .get(invocation.getArgument(0, String.class)));

        int totalThreads = sessionCount * CONCURRENCY;
        ConcurrentMap<String, Set<String>> accessTokensBySession = new ConcurrentHashMap<>();
        ConcurrentMap<String, AtomicInteger> issuerCountBySession = new ConcurrentHashMap<>();
        for (String sessionId : tokens.keySet()) {
            accessTokensBySession.put(sessionId, ConcurrentHashMap.newKeySet());
            issuerCountBySession.put(sessionId, new AtomicInteger());
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger done = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(totalThreads);
        try {
            for (Map.Entry<String, IssuedToken> entry : tokens.entrySet()) {
                String sessionId = entry.getKey();
                IssuedToken token = entry.getValue();
                AtomicInteger issuerCount = issuerCountBySession.get(sessionId);
                for (int i = 0; i < CONCURRENCY; i++) {
                    futures.add(executor.submit(() -> {
                        try {
                            assertTrue(startLatch.await(10, TimeUnit.SECONDS), "启动信号未就位");
                            RefreshIssueResult result = service.rotate(token.rawToken(),
                                new MockHttpServletResponse(), ignored -> {
                                    int count = issuerCount.incrementAndGet();
                                    return new IssuedAccessToken("access-" + sessionId + "-"
                                        + count, 900L, null);
                                });
                            accessTokensBySession.get(sessionId).add(result.getAccessToken());
                            return null;
                        } finally {
                            done.incrementAndGet();
                        }
                    }));
                }
            }
            startLatch.countDown();
            awaitAll(executor, futures, done, totalThreads);
        } finally {
            executor.shutdownNow();
        }

        for (String sessionId : tokens.keySet()) {
            assertEquals(1, issuerCountBySession.get(sessionId).get(), "Session " + sessionId
                + " 的签发回调执行次数");
            assertEquals(1, accessTokensBySession.get(sessionId).size(), "Session " + sessionId
                + " 的凭证种类数");
        }
    }

    /**
     * 并发同代轮换的公共骨架：全员闸门同步发起轮换并收集结果。
     *
     * @param threads        并发线程数
     * @param presentedToken 出示的令牌供给器
     * @param issuer         签发回调
     */
    private void runConcurrently(int threads, java.util.function.Supplier<String> presentedToken,
        Function<RefreshSession, IssuedAccessToken> issuer) throws Exception {
        issuerInvocations.set(0);
        Set<String> accessTokens = ConcurrentHashMap.newKeySet();
        Set<String> refreshTokens = ConcurrentHashMap.newKeySet();
        AtomicInteger done = new AtomicInteger();
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>(threads);
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        assertTrue(startLatch.await(5, TimeUnit.SECONDS), "启动信号未就位");
                        RefreshIssueResult result = service.rotate(presentedToken.get(),
                            new MockHttpServletResponse(), issuer);
                        accessTokens.add(result.getAccessToken());
                        refreshTokens.add(result.getRefreshToken());
                        return null;
                    } finally {
                        done.incrementAndGet();
                    }
                }));
            }
            startLatch.countDown();
            awaitAll(executor, futures, done, threads);
        } finally {
            executor.shutdownNow();
        }

        // 硬契约 1：签发回调恰好执行一次（代际只前进一代）
        assertEquals(1, issuerInvocations.get(), "并发同代轮换的签发回调执行次数");
        // 硬契约 2：全部成功请求共享同一组凭证（幂等重放），不丢不重
        assertEquals(1, accessTokens.size(), "并发成功响应的 Access Token 种类数");
        assertEquals(1, refreshTokens.size(), "并发成功响应的 Refresh Token 种类数");
        // 硬契约 3：Session 指纹确实切换到新代，旧代进入 previous
        String newRefreshToken = refreshTokens.iterator().next();
        assertNotNull(newRefreshToken);
        assertNotEquals(initialToken.rawToken(), newRefreshToken);
        assertEquals(initialToken.fingerprint(), session.getPreviousTokenFingerprint());
        assertEquals(codec.parse(newRefreshToken).fingerprint(), session
            .getCurrentTokenFingerprint());
    }

    /**
     * 等待全部并发任务完成：断言失败经 Future 传播到主线程，超时兜底避免测试挂死。
     *
     * @param executor      执行器
     * @param futures       任务列表
     * @param done          完成计数
     * @param totalThreads  线程总数
     */
    private void awaitAll(ExecutorService executor, List<Future<?>> futures, AtomicInteger done,
        int totalThreads) throws Exception {
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        assertEquals(totalThreads, done.get(), "并发任务未全部完成");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "执行器未正常关闭");
    }
}
