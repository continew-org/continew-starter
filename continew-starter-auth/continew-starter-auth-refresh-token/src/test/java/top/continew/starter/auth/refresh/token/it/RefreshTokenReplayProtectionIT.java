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

package top.continew.starter.auth.refresh.token.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.exception.RefreshTokenException;
import top.continew.starter.auth.refresh.token.model.AuthSecurityVersion;
import top.continew.starter.auth.refresh.token.model.IssuedAccessToken;
import top.continew.starter.auth.refresh.token.model.RefreshIssueResult;
import top.continew.starter.auth.refresh.token.service.impl.RefreshTokenServiceImpl;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;

import java.util.List;

/**
 * Refresh Token 重放保护真实 Redis 行为集成测试。
 *
 * <p>依赖真实 TTL 到期与真实键位清理：宽限期过期后重放旧令牌必须撤销会话，
 * 伪造 secret 重用已知 sessionId 不得撤销会话，二者构成重放防护的两条边界。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("Refresh Token 重放保护真实 Redis 行为集成测试")
class RefreshTokenReplayProtectionIT extends RefreshTokenRedisSupport {

    /**
     * 以下键名前缀锁定 RefreshSessionStore 的新部署命名空间，防止键位契约被静默破坏
     */
    private static final String DATA_PREFIX = "AUTH:REFRESH:V1:DATA:";
    private static final String REASON_PREFIX = "AUTH:REFRESH:V1:REASON:";
    private static final String LATEST_ROTATION_PREFIX = "AUTH:REFRESH:V1:ROTATION:LATEST:";
    private static final String ROTATION_PREFIX = "AUTH:REFRESH:V1:ROTATION:";
    private static final String ALL_INDEX_KEY = "AUTH:REFRESH:V1:ALL";
    private static final String USER_INDEX_PREFIX = "AUTH:REFRESH:V1:USER:";
    private static final String CLIENT_INDEX_PREFIX = "AUTH:REFRESH:V1:CLIENT:";

    @Test
    @DisplayName("宽限期过期后重放旧令牌：撤销会话、键位清理、新令牌随之失效")
    void shouldKickoutSessionWhenReplayingExpiredOldToken() throws InterruptedException {
        RefreshTokenProperties properties = testProperties(1);
        RefreshTokenServiceImpl service = newService(properties, List.of());
        RefreshTokenCodec codec = new RefreshTokenCodec(properties);
        String sessionId = service.newSessionId();
        String oldToken = service.issue(sessionId, new ItPrincipal(9301L, "it-user"),
            bodyPolicy("it-web"), new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());
        RefreshIssueResult first = service.rotate(oldToken, new MockHttpServletResponse(),
            ignored -> new IssuedAccessToken("it-access-1", 900, null));
        String newToken = first.getRefreshToken();
        RefreshTokenCodec.ParsedToken oldParsed = codec.parse(oldToken);
        RefreshTokenCodec.ParsedToken newParsed = codec.parse(newToken);

        awaitKeyExpiry(ROTATION_PREFIX + oldParsed.fingerprint());

        // 宽限期外的旧令牌重放：请求被拒，会话被撤销并留下 KICKOUT 原因标记
        RefreshTokenException exception = assertThrows(RefreshTokenException.class,
            () -> service.rotate(oldToken, new MockHttpServletResponse(),
                ignored -> new IssuedAccessToken("it-access-2", 900, null)));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        assertNull(rawValue(DATA_PREFIX + sessionId));
        assertEquals("KICKOUT", rawValue(REASON_PREFIX + sessionId));
        assertNull(rawValue(ROTATION_PREFIX + newParsed.fingerprint()));
        assertNull(rawValue(LATEST_ROTATION_PREFIX + sessionId));
        assertIndexNotContains(ALL_INDEX_KEY, sessionId);
        assertIndexNotContains(USER_INDEX_PREFIX + "9301", sessionId);
        assertIndexNotContains(CLIENT_INDEX_PREFIX + "it-web", sessionId);

        // 会话已不存在：合法持有的新令牌同样失效
        assertThrows(RefreshTokenException.class, () -> service.rotate(newToken,
            new MockHttpServletResponse(), ignored -> new IssuedAccessToken("it-access-3", 900,
                null)));
    }

    @Test
    @DisplayName("已知 sessionId 拼接伪造 secret：请求被拒且会话不被撤销")
    void shouldKeepSessionWhenForgedSecretReusesKnownSessionId() {
        RefreshTokenProperties properties = testProperties(30);
        RefreshTokenServiceImpl service = newService(properties, List.of());
        RefreshTokenCodec codec = new RefreshTokenCodec(properties);
        String sessionId = service.newSessionId();
        String oldToken = service.issue(sessionId, new ItPrincipal(9302L, "it-user"),
            bodyPolicy("it-web"), new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());

        String forgedToken = sessionId + "." + "A".repeat(43);
        assertThrows(RefreshTokenException.class, () -> service.rotate(forgedToken,
            new MockHttpServletResponse(), ignored -> new IssuedAccessToken("it-access-forged",
                900, null)));
        // 伪造凭证不得撤销会话：会话记录保持、无失效原因标记，仅凭公开 sessionId 无法强制下线
        assertNotNull(rawValue(DATA_PREFIX + sessionId));
        assertNull(rawValue(REASON_PREFIX + sessionId));

        // 原始合法凭证仍可正常轮换
        RefreshIssueResult legit = service.rotate(oldToken, new MockHttpServletResponse(),
            ignored -> new IssuedAccessToken("it-access-legit", 900, null));
        assertEquals("it-access-legit", legit.getAccessToken());
        assertFalse(JSONUtil.parseObj(rawValue(DATA_PREFIX + sessionId)).getStr(
            "currentTokenFingerprint").equals(codec.parse(oldToken).fingerprint()));
    }
}
