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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.model.AuthSecurityVersion;
import top.continew.starter.auth.refresh.token.service.impl.RefreshTokenServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Refresh Token 统一撤销真实 Redis 行为集成测试。
 *
 * <p>按用户与按会话两种撤销入口，验证真实键位清理、KICKOUT 原因标记、安全版本
 * 原子递增、管理索引收敛与撤销通知的完整闭环。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("Refresh Token 统一撤销真实 Redis 行为集成测试")
class RefreshTokenRevocationIT extends RefreshTokenRedisSupport {

    /**
     * 以下键名前缀锁定 RefreshSessionStore 的新部署命名空间，防止键位契约被静默破坏
     */
    private static final String DATA_PREFIX = "AUTH:REFRESH:V1:DATA:";
    private static final String REASON_PREFIX = "AUTH:REFRESH:V1:REASON:";
    private static final String ALL_INDEX_KEY = "AUTH:REFRESH:V1:ALL";
    private static final String USER_INDEX_PREFIX = "AUTH:REFRESH:V1:USER:";
    private static final String CLIENT_INDEX_PREFIX = "AUTH:REFRESH:V1:CLIENT:";
    private static final String VERSION_USER_PREFIX = "AUTH:REFRESH:V1:VERSION:USER:";

    @Test
    @DisplayName("revokeByUser 撤销该用户全部会话并递增安全版本，其他用户会话不受影响")
    void shouldRevokeAllUserSessionsAndIncrementSecurityVersion() {
        RefreshTokenProperties properties = testProperties(30);
        List<String> revokedSessionIds = new ArrayList<>();
        RefreshTokenServiceImpl service = newService(properties, List.of(revokedSessionIds::add));
        String sessionA = service.newSessionId();
        String sessionB = service.newSessionId();
        String sessionC = service.newSessionId();
        service.issue(sessionA, new ItPrincipal(9401L, "it-user-a"), bodyPolicy("it-web"),
            new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());
        service.issue(sessionB, new ItPrincipal(9401L, "it-user-a"), bodyPolicy("it-app"),
            new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());
        service.issue(sessionC, new ItPrincipal(9402L, "it-user-b"), bodyPolicy("it-web"),
            new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());

        service.revokeByUser(9401L);

        // 目标用户两个会话全部清理并留下 KICKOUT 标记；其他用户会话不受影响
        assertNull(rawValue(DATA_PREFIX + sessionA));
        assertEquals("KICKOUT", rawValue(REASON_PREFIX + sessionA));
        assertNull(rawValue(DATA_PREFIX + sessionB));
        assertEquals("KICKOUT", rawValue(REASON_PREFIX + sessionB));
        assertNotNull(rawValue(DATA_PREFIX + sessionC));

        // 用户安全版本经原子计数真实写入；其他用户的版本键未被创建
        assertEquals("1", rawValue(VERSION_USER_PREFIX + "9401"));
        assertNull(rawValue(VERSION_USER_PREFIX + "9402"));

        // 索引只收敛目标用户的会话
        assertIndexNotContains(ALL_INDEX_KEY, sessionA);
        assertIndexNotContains(ALL_INDEX_KEY, sessionB);
        assertIndexContains(ALL_INDEX_KEY, sessionC);
        assertIndexNotContains(USER_INDEX_PREFIX + "9401", sessionA);
        assertIndexNotContains(USER_INDEX_PREFIX + "9401", sessionB);
        assertIndexNotContains(CLIENT_INDEX_PREFIX + "it-web", sessionA);
        assertIndexContains(CLIENT_INDEX_PREFIX + "it-web", sessionC);

        // 服务侧视图与会话撤销通知；通知顺序取决于索引遍历顺序，按集合语义比较
        assertNull(service.getSession(sessionA));
        assertNotNull(service.getSession(sessionC));
        assertEquals(Set.of(sessionA, sessionB), Set.copyOf(revokedSessionIds));
    }

    @Test
    @DisplayName("revokeBySessionId 只撤销指定会话，同用户其余会话保持有效")
    void shouldRevokeOnlyTargetedSessionById() {
        RefreshTokenProperties properties = testProperties(30);
        RefreshTokenServiceImpl service = newService(properties, List.of());
        String sessionD = service.newSessionId();
        String sessionE = service.newSessionId();
        service.issue(sessionD, new ItPrincipal(9403L, "it-user"), bodyPolicy("it-web"),
            new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());
        service.issue(sessionE, new ItPrincipal(9403L, "it-user"), bodyPolicy("it-app"),
            new AuthSecurityVersion(0, 0, 0), new MockHttpServletResponse());

        service.revokeBySessionId(sessionD);

        assertNull(rawValue(DATA_PREFIX + sessionD));
        assertEquals("KICKOUT", rawValue(REASON_PREFIX + sessionD));
        assertNotNull(rawValue(DATA_PREFIX + sessionE));
        assertIndexNotContains(ALL_INDEX_KEY, sessionD);
        assertIndexContains(ALL_INDEX_KEY, sessionE);
        assertIndexContains(USER_INDEX_PREFIX + "9403", sessionE);
        assertNull(service.getSession(sessionD));
        assertNotNull(service.getSession(sessionE));
    }
}
