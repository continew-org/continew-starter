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

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.enums.LogoutReasonEnum;
import top.continew.starter.auth.refresh.token.enums.RefreshTokenModeEnum;
import top.continew.starter.auth.refresh.token.model.AuthSecurityVersion;
import top.continew.starter.auth.refresh.token.model.RefreshClientPolicy;
import top.continew.starter.auth.refresh.token.model.RefreshRotationResult;
import top.continew.starter.auth.refresh.token.model.IssuedAccessToken;
import top.continew.starter.auth.refresh.token.model.RefreshIssueResult;
import top.continew.starter.auth.refresh.token.model.RefreshSession;
import top.continew.starter.auth.refresh.token.api.AuthSessionConstants;
import top.continew.starter.auth.refresh.token.enums.SessionReplacementScope;
import top.continew.starter.auth.refresh.token.service.impl.RefreshTokenServiceImpl;
import top.continew.starter.auth.refresh.token.service.RefreshTokenService.LoginAttempt;
import top.continew.starter.auth.refresh.token.support.AccessSessionCache;
import top.continew.starter.auth.refresh.token.support.RefreshSessionStore;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.auth.refresh.token.support.RefreshTokenRequestGuard;
import top.continew.starter.auth.refresh.token.support.AuthPolicyLock;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec.IssuedToken;
import top.continew.starter.auth.refresh.token.api.AuthSessionRevocationNotifier;
import top.continew.starter.auth.refresh.token.exception.RefreshTokenException;
import top.continew.starter.cache.redisson.util.RedisLockUtils;
import top.continew.starter.core.exception.BusinessException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

/**
 * Refresh Token 轮换、重放和传输边界测试。
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
class RefreshTokenServiceImplTest {

    private final Map<String, RefreshRotationResult> rotations = new HashMap<>();
    private final Map<String, RefreshRotationResult> latestRotations = new HashMap<>();
    private RefreshSessionStore sessionStore;
    private RefreshTokenProperties properties;
    private RefreshTokenCodec codec;
    private RefreshTokenServiceImpl service;
    private AuthSessionRevocationNotifier sessionRevocationNotifier;
    private AccessSessionCache accessSessionCache;
    private RefreshSession session;
    private IssuedToken initialToken;

    @BeforeEach
    void setUp() {
        properties = new RefreshTokenProperties();
        properties.setSecret("test-only-refresh-token-secret-with-32-bytes");
        properties.setIpRateLimit(0);
        properties.setSessionRateLimit(10);
        properties.setRotationGracePeriod(5);
        codec = new RefreshTokenCodec(properties);

        sessionStore = mock(RefreshSessionStore.class);
        RedisLockUtils sessionLock = mock(RedisLockUtils.class);
        AuthPolicyLock policyLock = mock(AuthPolicyLock.class);
        when(sessionStore.lockSession(anyString())).thenReturn(sessionLock);
        when(sessionStore.lockUserPolicy(anyLong())).thenReturn(policyLock);
        when(sessionStore.lockTenantPolicyRead(anyLong())).thenReturn(policyLock);
        when(sessionStore.lockTenantPolicyWrite(anyLong())).thenReturn(policyLock);
        when(sessionStore.lockClientPolicyRead(anyString())).thenReturn(policyLock);
        when(sessionStore.lockClientPolicyWrite(anyString())).thenReturn(policyLock);
        when(sessionStore.tryAcquireRateLimit(anyString(), anyInt(), any(Duration.class)))
            .thenReturn(true);
        when(sessionStore.getSecurityVersion(anyLong(), anyString(), anyLong()))
            .thenReturn(new AuthSecurityVersion(0, 0, 0));
        when(sessionStore.isSecurityVersionCurrent(any(RefreshSession.class))).thenReturn(true);
        when(sessionStore.getRotation(anyString())).thenAnswer(invocation -> rotations
            .get(invocation.getArgument(0, String.class)));
        org.mockito.Mockito.doAnswer(invocation -> {
            rotations.put(invocation.getArgument(0, String.class),
                invocation.getArgument(1, RefreshRotationResult.class));
            return null;
        }).when(sessionStore).saveRotation(anyString(), any(RefreshRotationResult.class),
            anyLong());
        when(sessionStore.getLatestRotation(anyString())).thenAnswer(invocation -> latestRotations
            .get(invocation.getArgument(0, String.class)));
        org.mockito.Mockito.doAnswer(invocation -> {
            latestRotations.put(invocation.getArgument(0, String.class),
                invocation.getArgument(1, RefreshRotationResult.class));
            return null;
        }).when(sessionStore).saveLatestRotation(anyString(), any(RefreshRotationResult.class),
            anyLong());

        initialToken = codec.issue(codec.newSessionId());
        session = new RefreshSession();
        session.setSessionId(initialToken.sessionId());
        session.setUserId(1L);
        session.setClientId("web");
        session.setMode(RefreshTokenModeEnum.BODY);
        session.setExpiresAt(System.currentTimeMillis() + Duration.ofDays(1).toMillis());
        session.setCurrentTokenFingerprint(initialToken.fingerprint());
        when(sessionStore.get(initialToken.sessionId())).thenAnswer(invocation -> session);

        sessionRevocationNotifier = mock(AuthSessionRevocationNotifier.class);
        accessSessionCache = mock(AccessSessionCache.class);
        RefreshTokenRequestGuard requestGuard = new RefreshTokenRequestGuard(properties, codec,
            sessionStore);
        service = new RefreshTokenServiceImpl(properties, codec, sessionStore, requestGuard,
            sessionRevocationNotifier, accessSessionCache);
    }

    @Test
    void shouldRotateAndReplayExactlyTheSameResult() {
        RefreshIssueResult first = service.rotate(initialToken.rawToken(), response(),
            issuer("access-1"));

        assertEquals("access-1", first.getAccessToken());
        assertEquals(initialToken.sessionId(), codec.parse(first.getRefreshToken()).sessionId());
        assertNotEquals(initialToken.rawToken(), first.getRefreshToken());
        assertEquals(initialToken.fingerprint(), session.getPreviousTokenFingerprint());
        assertTrue(codec.matches(codec.parse(first.getRefreshToken()).fingerprint(),
            session.getCurrentTokenFingerprint()));
        RefreshIssueResult replay =
            service.rotate(initialToken.rawToken(), response(), ignored -> {
                throw new AssertionError("幂等重放不应再次签发 Access Token");
            });
        assertEquals(first.getAccessToken(), replay.getAccessToken());
        assertEquals(first.getRefreshToken(), replay.getRefreshToken());
        verify(sessionStore, times(1)).tryAcquireRateLimit(anyString(), anyInt(),
            any(Duration.class));
    }

    @Test
    void shouldAcquirePolicyLocksBeforeSessionLockWhenRotating() {
        session.setTenantId(2L);

        service.rotate(initialToken.rawToken(), response(), issuer("access-1"));

        InOrder order = inOrder(sessionStore);
        order.verify(sessionStore).lockUserPolicy(1L);
        order.verify(sessionStore).lockTenantPolicyRead(2L);
        order.verify(sessionStore).lockClientPolicyRead("web");
        order.verify(sessionStore).lockSession(initialToken.sessionId());
    }

    @Test
    void shouldNotAdvanceAnotherGenerationInsideGracePeriod() {
        RefreshIssueResult first =
            service.rotate(initialToken.rawToken(), response(), issuer("access-1"));

        RefreshIssueResult replay = service.rotate(first.getRefreshToken(), response(), ignored -> {
            throw new AssertionError("宽限期内不应再次签发 Access Token");
        });

        assertEquals(first.getAccessToken(), replay.getAccessToken());
        assertEquals(first.getRefreshToken(), replay.getRefreshToken());
        verify(sessionStore, times(1)).tryAcquireRateLimit(anyString(), anyInt(),
            any(Duration.class));
    }

    @Test
    void shouldNotRevokeSessionForUnknownSecret() {
        IssuedToken unknown = codec.issue(initialToken.sessionId());
        Executable rotate = () -> service.rotate(unknown.rawToken(), response(), issuer("unused"));

        assertThrows(BusinessException.class, rotate);

        verify(sessionStore, never()).lockSession(anyString());
        verify(sessionStore, never()).save(session);
        verify(sessionStore, never()).delete(session.getSessionId());
        verify(sessionStore, never()).removeIndexes(session);
    }

    @Test
    void shouldRejectOldGenerationTokenWhenSessionAlreadyRevoked() {
        // R0→R1→R2 后会话被撤销（强退/顶人），R0 指纹的轮换快照仍在宽限期内残留。
        // 凭证已无对应会话，必须按无效令牌处理，不能进入候选会话空指针。
        rotations.put(initialToken.fingerprint(), new RefreshRotationResult());
        when(sessionStore.get(initialToken.sessionId())).thenReturn(null);
        Executable rotate =
            () -> service.rotate(initialToken.rawToken(), response(), issuer("unused"));

        RefreshTokenException exception = assertThrows(RefreshTokenException.class, rotate);

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(sessionStore, never()).lockSession(anyString());
        verify(sessionStore, never()).lockUserPolicy(anyLong());
    }

    @Test
    void shouldRevokePreviousTokenWhenGraceSnapshotIsGone() {
        service.rotate(initialToken.rawToken(), response(), issuer("access-1"));
        rotations.clear();
        latestRotations.clear();
        // 超出宽限期的明确重放必须直接撤销，不能被已耗尽的刷新配额挡住。
        when(sessionStore.tryAcquireRateLimit(anyString(), anyInt(), any(Duration.class)))
            .thenReturn(false);
        Executable rotate =
            () -> service.rotate(initialToken.rawToken(), response(), issuer("unused"));

        assertThrows(BusinessException.class, rotate);

        verify(sessionStore).delete(session.getSessionId());
        verify(sessionStore).removeIndexes(session);
        verify(sessionStore, times(1)).tryAcquireRateLimit(anyString(), anyInt(),
            any(Duration.class));
    }

    @Test
    void shouldPreserveSessionAndResumeSameRotationAfterTemporaryFailure() {
        RuntimeException failure = new RuntimeException("temporary issuer failure");
        Executable rotate = () -> service.rotate(initialToken.rawToken(), response(),
            ignored -> {
                throw failure;
            });

        assertThrows(RuntimeException.class, rotate);

        verify(sessionStore, never()).delete(session.getSessionId());
        RefreshRotationResult pending = rotations.get(initialToken.fingerprint());
        assertTrue(pending != null && !pending.isComplete());
        String pendingRefreshToken = codec.decrypt(pending.getEncryptedRefreshToken());

        RefreshIssueResult recovered = service.rotate(initialToken.rawToken(), response(),
            issuer("access-recovered"));

        assertEquals("access-recovered", recovered.getAccessToken());
        assertEquals(pendingRefreshToken, recovered.getRefreshToken());
    }

    @Test
    void shouldRevokeWhenSecurityVersionChangesDuringRefresh() {
        when(sessionStore.isSecurityVersionCurrent(session)).thenReturn(true, false);
        Executable rotate =
            () -> service.rotate(initialToken.rawToken(), response(), issuer("access-1"));

        RefreshTokenException exception = assertThrows(RefreshTokenException.class, rotate);

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(sessionStore).delete(session.getSessionId());
        verify(sessionStore).removeIndexes(session);
    }

    @Test
    void shouldRejectCookieAndBodyConflict() {
        MockHttpServletRequest request = request();
        request.setCookies(new Cookie("refresh_token", initialToken.rawToken()));
        Executable resolve = () -> service.resolve(initialToken.rawToken(), request);

        RefreshTokenException exception = assertThrows(RefreshTokenException.class, resolve);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void shouldRequireTrustedOriginForCookieMode() {
        session.setMode(RefreshTokenModeEnum.COOKIE);
        MockHttpServletRequest request = request();
        request.setCookies(new Cookie("refresh_token", initialToken.rawToken()));
        request.addHeader("Origin", "https://attacker.example");
        Executable validate = () -> service.validateRequest(initialToken.rawToken(), request);

        assertThrows(BusinessException.class, validate);

        request.removeHeader("Origin");
        request.addHeader("Origin", "https://admin.example");
        service.validateRequest(initialToken.rawToken(), request);
    }

    @Test
    void shouldValidateCookieOriginBeforeParsingMalformedToken() {
        MockHttpServletRequest request = request();
        request.setCookies(new Cookie("refresh_token", "malformed"));
        request.addHeader("Origin", "https://admin.example");

        service.validateCookieOrigin(request);
        assertThrows(BusinessException.class,
            () -> service.validateRequest("malformed", request));

        request.removeHeader("Origin");
        request.addHeader("Origin", "https://attacker.example");
        RefreshTokenException exception = assertThrows(RefreshTokenException.class,
            () -> service.validateCookieOrigin(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void shouldRequireTrustedOriginForCachedOlderCookieToken() {
        session.setMode(RefreshTokenModeEnum.COOKIE);
        session.setCurrentTokenFingerprint(codec.issue(session.getSessionId()).fingerprint());
        RefreshRotationResult cached = new RefreshRotationResult();
        cached.setEncryptedAccessToken(codec.encrypt("access-1"));
        rotations.put(initialToken.fingerprint(), cached);
        MockHttpServletRequest request = request();
        request.setCookies(new Cookie("refresh_token", initialToken.rawToken()));
        request.addHeader("Origin", "https://attacker.example");
        Executable validate = () -> service.validateRequest(initialToken.rawToken(), request);

        assertThrows(BusinessException.class, validate);
    }

    @Test
    void shouldRejectWildcardCookieOriginConfiguration() {
        properties.setCookieAllowedOrigins(List.of("*"));

        assertFalse(properties.isCookieAllowedOriginsValid());
    }

    @Test
    void shouldAllowCookieOriginWildcardForConfiguredSubdomains() {
        properties.setCookieAllowedOrigins(List.of("http://*.luoqiz.top"));
        assertTrue(properties.isCookieAllowedOriginsValid());
        RefreshTokenRequestGuard requestGuard = new RefreshTokenRequestGuard(properties, codec,
            sessionStore);
        MockHttpServletRequest request = request();
        request.setCookies(new Cookie("refresh_token", initialToken.rawToken()));
        request.addHeader("Origin", "http://admin.luoqiz.top");

        assertDoesNotThrow(() -> requestGuard.validateCookieOrigin(request));

        request.removeHeader("Origin");
        request.addHeader("Origin", "http://admin.luoqiz.top.attacker.example");
        assertThrows(RefreshTokenException.class, () -> requestGuard.validateCookieOrigin(request));

        request.removeHeader("Origin");
        request.addHeader("Origin", "http://nested.admin.luoqiz.top");
        assertThrows(RefreshTokenException.class, () -> requestGuard.validateCookieOrigin(request));
    }

    @Test
    void shouldRevokeRefreshSessionEvenWhenAccessTokenIndexAlreadyExpired() {
        RefreshSession oldSession = session("old-web", "WEB", 1L);
        when(sessionStore.findByUser(1L)).thenReturn(Set.of(oldSession.getSessionId()));
        when(sessionStore.get(oldSession.getSessionId())).thenReturn(oldSession);

        RefreshClientPolicy client = client(false, SessionReplacementScope.ALL_CLIENT_TYPES, -1);
        String result = service.executeLoginPolicy(1L, client.clientId(), 2L,
            version -> new LoginAttempt<>(1L, client, null, null, () -> "issued"));

        assertEquals("issued", result);
        verify(sessionStore).delete(oldSession.getSessionId());
    }

    @Test
    void shouldOnlyRevokeSameClientTypeForCurrentDevicePolicy() {
        RefreshSession webSession = session("old-web", "WEB", 1L);
        RefreshSession appSession = session("old-app", "APP", 2L);
        when(sessionStore.findByUser(1L))
            .thenReturn(Set.of(webSession.getSessionId(), appSession.getSessionId()));
        when(sessionStore.get(webSession.getSessionId())).thenReturn(webSession);
        when(sessionStore.get(appSession.getSessionId())).thenReturn(appSession);

        RefreshClientPolicy client = client(false, SessionReplacementScope.CURRENT_CLIENT_TYPE, -1);
        service.executeLoginPolicy(1L, client.clientId(), 2L,
            version -> new LoginAttempt<>(1L, client, null, null, () -> null));

        verify(sessionStore).delete(webSession.getSessionId());
        verify(sessionStore, never()).delete(appSession.getSessionId());
    }

    @Test
    void shouldApplyMaxLoginCountOnlyToCurrentClientType() {
        RefreshSession oldest = session("oldest", "WEB", 1L);
        RefreshSession newest = session("newest", "WEB", 2L);
        RefreshSession app = session("app", "APP", 3L);
        when(sessionStore.findByUser(1L))
            .thenReturn(Set.of(newest.getSessionId(), oldest.getSessionId(), app.getSessionId()));
        when(sessionStore.get(oldest.getSessionId())).thenReturn(oldest);
        when(sessionStore.get(newest.getSessionId())).thenReturn(newest);
        when(sessionStore.get(app.getSessionId())).thenReturn(app);

        RefreshClientPolicy client = client(true, null, 2);
        service.executeLoginPolicy(1L, client.clientId(), 2L,
            version -> new LoginAttempt<>(1L, client, null, null, () -> null));

        verify(sessionStore).delete(oldest.getSessionId());
        verify(sessionStore, never()).delete(newest.getSessionId());
        verify(sessionStore, never()).delete(app.getSessionId());
    }

    @Test
    void shouldAcquireLoginLocksInFixedOrderAndReleaseInReverseOrder() {
        AuthPolicyLock userLock = mock(AuthPolicyLock.class);
        AuthPolicyLock tenantLock = mock(AuthPolicyLock.class);
        AuthPolicyLock clientLock = mock(AuthPolicyLock.class);
        when(sessionStore.lockUserPolicy(1L)).thenReturn(userLock);
        when(sessionStore.lockTenantPolicyRead(2L)).thenReturn(tenantLock);
        when(sessionStore.lockClientPolicyRead("web")).thenReturn(clientLock);

        RefreshClientPolicy client = client(true, null, -1);
        String result = service.executeLoginPolicy(1L, "web", 2L,
            version -> new LoginAttempt<>(1L, client, null, null, () -> "issued"));

        assertEquals("issued", result);
        org.mockito.InOrder order = inOrder(sessionStore, userLock, tenantLock, clientLock);
        order.verify(sessionStore).lockUserPolicy(1L);
        order.verify(sessionStore).lockTenantPolicyRead(2L);
        order.verify(sessionStore).lockClientPolicyRead("web");
        order.verify(clientLock).close();
        order.verify(tenantLock).close();
        order.verify(userLock).close();
    }

    @Test
    void shouldHoldInvalidationLockUntilTransactionCompletion() {
        AuthPolicyLock userLock = mock(AuthPolicyLock.class);
        RefreshSession oldSession = session("old-web", "WEB", 1L);
        when(sessionStore.lockUserPolicy(1L)).thenReturn(userLock);
        when(sessionStore.findByUser(1L)).thenReturn(Set.of(oldSession.getSessionId()));
        when(sessionStore.get(oldSession.getSessionId())).thenReturn(oldSession);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.revokeByUser(1L);

            verify(sessionStore, times(1)).incrementUserSecurityVersion(1L);
            verify(userLock, never()).close();
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager
                .getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
            verify(userLock).close();
            // 不再依赖 afterCommit 二次执行 Redis 失效操作。
            verify(sessionStore, times(1)).incrementUserSecurityVersion(1L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void shouldFailClosedWhenTransactionSynchronizationIsUnavailable() {
        AuthPolicyLock userLock = mock(AuthPolicyLock.class);
        when(sessionStore.lockUserPolicy(1L)).thenReturn(userLock);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThrows(IllegalStateException.class, () -> service.revokeByUser(1L));

            verify(sessionStore, never()).incrementUserSecurityVersion(1L);
            verify(userLock).close();
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void shouldNotifyWebSocketClusterWhenSessionIsRevoked() {
        RefreshSession oldSession = session("old-web", "WEB", 1L);
        when(sessionStore.findByUser(1L)).thenReturn(Set.of(oldSession.getSessionId()));
        when(sessionStore.get(oldSession.getSessionId())).thenReturn(oldSession);

        service.revokeByUser(1L);

        verify(sessionRevocationNotifier).notifyRevoked(oldSession.getSessionId());
        // 用户级强制下线在 Redis 中记录失效原因，让被踢方下次请求看到准确提示。
        verify(sessionStore).saveLogoutReason(oldSession.getSessionId(), LogoutReasonEnum.KICKOUT);
    }

    @Test
    void shouldSkipSessionStoreWhenAccessSessionCacheHit() {
        String sessionId = initialToken.sessionId();
        when(accessSessionCache.isValid(sessionId)).thenReturn(true);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getExtra("access-token-1",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn(sessionId);
            assertNull(service.getInvalidReason("access-token-1"));
        }
        verify(sessionStore, never()).get(anyString());
    }

    @Test
    void shouldMarkValidAfterFullValidation() {
        String sessionId = initialToken.sessionId();
        when(accessSessionCache.isValid(sessionId)).thenReturn(false);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getExtra("access-token-1",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn(sessionId);
            stpUtil.when(() -> StpUtil.getLoginIdByToken("access-token-1")).thenReturn(1L);
            assertNull(service.getInvalidReason("access-token-1"));
        }
        verify(sessionStore).get(sessionId);
        verify(accessSessionCache).markValid(sessionId);
    }

    @Test
    void shouldTreatExpiredOrTamperedAccessTokenAsInvalidWithoutException() {
        // 热路径上过期/被篡改的 Access Token 会触发 Sa-Token 抛 NotLoginException，
        // 必须静默降级为「会话已失效」，不得冒泡成 500。
        String sessionId = initialToken.sessionId();
        when(accessSessionCache.isValid(sessionId)).thenReturn(false);
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getExtra("expired-token",
                AuthSessionConstants.SESSION_ID_CLAIM)).thenReturn(sessionId);
            stpUtil.when(() -> StpUtil.getLoginIdByToken("expired-token"))
                .thenThrow(new RuntimeException("NotLoginException"));
            // 无效 Token 与用户不匹配，会话判为失效，返回失效提示而不抛异常
            assertNotNull(service.getInvalidReason("expired-token"));
        }
    }

    @Test
    void shouldExecuteSecurityHooksInOrderThroughRefreshTemplate() {
        // 模板方法 refresh() 必须按 resolve → 限流 → 来源校验 → rotate 固化顺序执行。
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = response();
        Function<RefreshSession, IssuedAccessToken> issuer = issuer("access-template");
        String rawToken = initialToken.rawToken();

        RefreshTokenServiceImpl spy = spy(service);
        spy.refresh(rawToken, request, response, issuer);

        InOrder order = inOrder(spy);
        order.verify(spy).resolve(rawToken, request);
        order.verify(spy).checkRequestRateLimit(request);
        order.verify(spy).validateRequest(rawToken, request);
        order.verify(spy).rotate(rawToken, response, issuer);
    }

    @Test
    void shouldInvalidateAccessSessionCacheWhenSessionRevoked() {
        service.revokeBySessionId(initialToken.sessionId());
        verify(accessSessionCache).invalidate(initialToken.sessionId());
    }

    @Test
    void shouldRejectTokenComposedFromForeignSessionIdAndOwnSecretOnRotate() {
        // 攻击者持有自己的会话且旧 secret 的轮换快照仍在宽限期内，将旧 secret 与
        // 受害者 sid 拼接。指纹绑定会话后，拼接令牌既不命中受害者 current/previous
        // 指纹，也无法命中攻击者自己的全局快照键，跨会话混淆在第一道门禁即被拒绝。
        IssuedToken attackerToken = codec.issue(codec.newSessionId());
        rotations.put(attackerToken.fingerprint(), completedSnapshot("attacker-access",
            attackerToken.rawToken()));
        String craftedToken = initialToken.sessionId() + "." + this
            .secretOf(attackerToken.rawToken());
        MockHttpServletResponse rotateResponse = response();
        Function<RefreshSession, IssuedAccessToken> unusedIssuer = issuer("unused");
        Executable rotate = () -> service.rotate(craftedToken, rotateResponse, unusedIssuer);

        assertThrows(BusinessException.class, rotate);

        verify(sessionStore, never()).lockSession(anyString());
        verify(sessionStore, never()).save(any(RefreshSession.class));
        verify(sessionRevocationNotifier, never()).notifyRevoked(anyString());
    }

    @Test
    void shouldNotReturnVictimLatestSnapshotForComposedTokenInCookieMode() {
        // 受害者刚完成过一次轮换、latest 幂等快照仍在宽限期内：攻击者拼接令牌即使
        // 携带自己的存活快照，也不得经受害者会话重放出受害者的最新令牌；COOKIE 模式
        // 下同样不得向响应写入任何凭证 Cookie。
        session.setMode(RefreshTokenModeEnum.COOKIE);
        IssuedToken attackerToken = codec.issue(codec.newSessionId());
        latestRotations.put(initialToken.sessionId(), completedSnapshot("victim-access",
            initialToken.rawToken()));
        rotations.put(attackerToken.fingerprint(), completedSnapshot("attacker-access",
            attackerToken.rawToken()));
        String craftedToken = initialToken.sessionId() + "." + this
            .secretOf(attackerToken.rawToken());
        MockHttpServletResponse victimResponse = response();
        Function<RefreshSession, IssuedAccessToken> unusedIssuer = issuer("unused");
        Executable rotate = () -> service.rotate(craftedToken, victimResponse, unusedIssuer);

        assertThrows(BusinessException.class, rotate);

        assertEquals(0, victimResponse.getCookies().length);
        verify(sessionRevocationNotifier, never()).notifyRevoked(anyString());
    }

    @Test
    void shouldNotRevokeForeignSessionWithComposedTokenOnLogout() {
        // 注销路径同样不得把"存在攻击者自己的轮换快照"当作受害者会话的凭证证明。
        IssuedToken attackerToken = codec.issue(codec.newSessionId());
        rotations.put(attackerToken.fingerprint(), completedSnapshot("attacker-access",
            attackerToken.rawToken()));
        String craftedToken = initialToken.sessionId() + "." + this
            .secretOf(attackerToken.rawToken());

        service.revokeCurrent(null, craftedToken);

        verify(sessionStore, never()).delete(initialToken.sessionId());
        verify(sessionStore, never()).removeIndexes(session);
        verify(sessionRevocationNotifier, never()).notifyRevoked(anyString());
    }

    @Test
    void shouldUseLongerTtlForPendingSnapshotThanIdempotentGrace() {
        service.rotate(initialToken.rawToken(), response(), issuer("access-1"));

        // 同一指纹的快照保存两次：切代前的待恢复快照使用独立恢复窗口（必须覆盖崩溃
        // 节点 watchdog 锁残留），完成后的幂等快照回到短期幂等宽限期。
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(sessionStore, times(2)).saveRotation(eq(initialToken.fingerprint()),
            any(RefreshRotationResult.class), ttlCaptor.capture());
        assertEquals(properties.getRotationPendingPeriod(), ttlCaptor.getAllValues().get(0)
            .longValue());
        assertEquals(properties.getRotationGracePeriod(), ttlCaptor.getAllValues().get(1)
            .longValue());
    }

    private RefreshRotationResult completedSnapshot(String accessToken, String refreshToken) {
        RefreshRotationResult snapshot = new RefreshRotationResult();
        snapshot.setEncryptedAccessToken(codec.encrypt(accessToken));
        snapshot.setEncryptedRefreshToken(codec.encrypt(refreshToken));
        return snapshot;
    }

    private String secretOf(String rawToken) {
        return rawToken.substring(rawToken.indexOf('.') + 1);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("admin.example");
        request.setServerPort(443);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private RefreshSession session(String sessionId, String clientType, long createdAt) {
        RefreshSession value = new RefreshSession();
        value.setSessionId(sessionId);
        value.setUserId(1L);
        value.setClientId(clientType.toLowerCase());
        value.setClientType(clientType);
        value.setCreatedAt(createdAt);
        value.setExpiresAt(System.currentTimeMillis() + Duration.ofDays(1).toMillis());
        return value;
    }

    private RefreshClientPolicy client(boolean concurrent, SessionReplacementScope replacementScope,
        int maxLoginCount) {
        return new RefreshClientPolicy("web", "WEB", 2592000L,
            RefreshTokenModeEnum.COOKIE, concurrent, replacementScope, maxLoginCount,
            LogoutReasonEnum.REPLACED);
    }

    private MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }

    private Function<RefreshSession, IssuedAccessToken> issuer(String accessToken) {
        return ignored -> new IssuedAccessToken(accessToken, 900L, 1L);
    }
}
