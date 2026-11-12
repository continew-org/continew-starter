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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.model.AuthSecurityVersion;
import top.continew.starter.auth.refresh.token.model.IssuedAccessToken;
import top.continew.starter.auth.refresh.token.model.RefreshIssueResult;
import top.continew.starter.auth.refresh.token.model.RefreshRotationResult;
import top.continew.starter.auth.refresh.token.model.RefreshSession;
import top.continew.starter.auth.refresh.token.service.impl.RefreshTokenServiceImpl;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;

import java.time.Duration;
import java.util.List;

/**
 * Refresh Token 轮换真实 Redis 状态集成测试。
 *
 * <p>会话记录、管理索引与轮换快照的键名、字段与 TTL 均以 {@code RefreshSessionStore}
 * 的真实落库结果为准，锁定单测无法证明的持久化契约。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("Refresh Token 轮换真实 Redis 状态集成测试")
class RefreshTokenRotationIT extends RefreshTokenRedisSupport {

    /**
     * 以下键名前缀锁定 RefreshSessionStore 的新部署命名空间，防止键位契约被静默破坏
     */
    private static final String DATA_PREFIX = "AUTH:REFRESH:V1:DATA:";
    private static final String ALL_INDEX_KEY = "AUTH:REFRESH:V1:ALL";
    private static final String USER_INDEX_PREFIX = "AUTH:REFRESH:V1:USER:";
    private static final String CLIENT_INDEX_PREFIX = "AUTH:REFRESH:V1:CLIENT:";
    private static final String ROTATION_PREFIX = "AUTH:REFRESH:V1:ROTATION:";

    @Test
    @DisplayName("签发会话后，会话记录、TTL 与管理索引以约定键名落入真实 Redis")
    void shouldPersistIssuedSessionInRealRedis() {
        RefreshTokenProperties properties = testProperties(30);
        RefreshTokenServiceImpl service = newService(properties, List.of());
        RefreshTokenCodec codec = new RefreshTokenCodec(properties);
        String sessionId = service.newSessionId();

        String rawToken = service.issue(sessionId, new ItPrincipal(9201L, "it-user"),
            bodyPolicy("it-web"), new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());

        // 会话记录：真实键存在、字段与令牌指纹一致、TTL 落在 refreshTokenTimeout（600 秒）窗口内
        String sessionJson = rawValue(DATA_PREFIX + sessionId);
        assertNotNull(sessionJson);
        JSONObject session = JSONUtil.parseObj(sessionJson);
        assertEquals("9201", session.getStr("userId"));
        RefreshTokenCodec.ParsedToken parsed = codec.parse(rawToken);
        assertEquals(parsed.fingerprint(), session.getStr("currentTokenFingerprint"));
        long ttl = client().getBucket(DATA_PREFIX + sessionId).remainTimeToLive();
        assertTrue(ttl > 0 && ttl <= Duration.ofMinutes(10).toMillis(), "会话 TTL 应在配置窗口内："
            + ttl);

        // 管理索引：全量、用户、客户端三个维度都登记了该会话
        assertIndexContains(ALL_INDEX_KEY, sessionId);
        assertIndexContains(USER_INDEX_PREFIX + "9201", sessionId);
        assertIndexContains(CLIENT_INDEX_PREFIX + "it-web", sessionId);

        // 服务侧能从真实 Redis 读回会话
        RefreshSession loaded = service.getSession(sessionId);
        assertNotNull(loaded);
        assertEquals(9201L, loaded.getUserId());
    }

    @Test
    @DisplayName("轮换后旧代指纹转入 previous、快照密文落库，宽限期内重放幂等")
    void shouldRotateWithRealRotationSnapshotAndIdempotentReplay() {
        RefreshTokenProperties properties = testProperties(30);
        RefreshTokenServiceImpl service = newService(properties, List.of());
        RefreshTokenCodec codec = new RefreshTokenCodec(properties);
        String sessionId = service.newSessionId();
        String oldToken = service.issue(sessionId, new ItPrincipal(9202L, "it-user"),
            bodyPolicy("it-web"), new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());

        RefreshIssueResult first = service.rotate(oldToken, new MockHttpServletResponse(),
            ignored -> new IssuedAccessToken("it-access-1", 900, null));
        assertEquals("it-access-1", first.getAccessToken());
        String newToken = first.getRefreshToken();
        RefreshTokenCodec.ParsedToken oldParsed = codec.parse(oldToken);
        RefreshTokenCodec.ParsedToken newParsed = codec.parse(newToken);

        // 会话记录完成代际切换：旧代指纹进入 previous，新代指纹成为 current
        JSONObject session = JSONUtil.parseObj(rawValue(DATA_PREFIX + sessionId));
        assertEquals(oldParsed.fingerprint(), session.getStr("previousTokenFingerprint"));
        assertEquals(newParsed.fingerprint(), session.getStr("currentTokenFingerprint"));

        // 旧代指纹的轮换快照真实落库，快照内密文可解出同一枚新令牌，TTL 服从宽限期配置
        String rotationJson = rawValue(ROTATION_PREFIX + oldParsed.fingerprint());
        assertNotNull(rotationJson);
        RefreshRotationResult snapshot = JSONUtil.toBean(rotationJson, RefreshRotationResult.class);
        assertEquals(newToken, codec.decrypt(snapshot.getEncryptedRefreshToken()));
        long snapshotTtl = client().getBucket(ROTATION_PREFIX + oldParsed.fingerprint())
            .remainTimeToLive();
        assertTrue(snapshotTtl > 0 && snapshotTtl <= Duration.ofSeconds(30).toMillis(),
            "快照 TTL 应服从宽限期：" + snapshotTtl);

        // 宽限期内重放旧令牌：幂等返回首次结果，不再前进代际、不重复执行签发回调
        RefreshIssueResult replay = service.rotate(oldToken, new MockHttpServletResponse(),
            ignored -> {
                throw new AssertionError("宽限期重放不应再次执行签发回调");
            });
        assertEquals("it-access-1", replay.getAccessToken());
        assertEquals(newToken, replay.getRefreshToken());
        JSONObject sessionAfterReplay = JSONUtil.parseObj(rawValue(DATA_PREFIX + sessionId));
        assertEquals(newParsed.fingerprint(), sessionAfterReplay.getStr("currentTokenFingerprint"));
    }
}
