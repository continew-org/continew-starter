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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Refresh Token 配置属性启动期校验边界测试。
 *
 * @author Charles7c
 * @since 2.17.0
 */
class RefreshTokenPropertiesTest {

    private final RefreshTokenProperties properties = new RefreshTokenProperties();

    @Test
    void shouldDefaultCookieInsecureAllowedToFalse() {
        assertFalse(properties.isCookieInsecureAllowed());
    }

    @Test
    void shouldDefaultPendingRotationPeriodLongerThanIdempotentGrace() {
        // 待恢复快照必须覆盖崩溃节点 watchdog 锁残留（Redisson 默认 30 秒）与客户端
        // 重试窗口，默认值需显著大于幂等宽限期。
        assertEquals(60, properties.getRotationPendingPeriod());
        assertTrue(properties.getRotationPendingPeriod() > properties.getRotationGracePeriod());
    }

    @Test
    void shouldRejectSameSiteNoneWithoutSecure() {
        properties.setCookieSameSite("None");
        properties.setCookieSecure(false);
        assertFalse(properties.isCookieSecurityValid());

        properties.setCookieSecure(true);
        assertTrue(properties.isCookieSecurityValid());
    }

    @Test
    void shouldRejectInvalidSameSiteValue() {
        properties.setCookieSameSite("Invalid");
        assertFalse(properties.isCookieSameSiteValid());

        properties.setCookieSameSite("lax");
        assertTrue(properties.isCookieSameSiteValid());
    }

    @Test
    void shouldEnforceHostPrefixConstraints() {
        properties.setCookieName("__Host-refresh_token");
        properties.setCookieSecure(false);
        properties.setCookiePath("/");
        assertFalse(properties.isHostCookieValid());

        properties.setCookieSecure(true);
        properties.setCookiePath("/auth");
        assertFalse(properties.isHostCookieValid());

        properties.setCookiePath("/");
        assertTrue(properties.isHostCookieValid());
    }

    @Test
    void shouldAcceptWildcardSubdomainOrigin() {
        properties.setCookieAllowedOrigins(List.of("https://*.example.com"));
        assertTrue(properties.isCookieAllowedOriginsValid());

        properties.setCookieAllowedOrigins(List.of("*"));
        assertFalse(properties.isCookieAllowedOriginsValid());
    }
}
