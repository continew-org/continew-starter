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

package top.continew.starter.auth.refresh.token.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Origin 严格校验边界测试。
 *
 * @author Charles7c
 * @since 2.17.0
 */
class OriginUtilsTest {

    @Test
    void shouldAcceptStrictHttpAndHttpsOrigin() {
        assertTrue(OriginUtils.isValidOrigin("https://admin.example.com"));
        assertTrue(OriginUtils.isValidOrigin("http://localhost:5173"));
        assertTrue(OriginUtils.isValidOrigin("https://example.com:8443"));
    }

    @Test
    void shouldRejectOriginWithPathQueryOrFragment() {
        assertFalse(OriginUtils.isValidOrigin("https://example.com/path"));
        assertFalse(OriginUtils.isValidOrigin("https://example.com/?a=1"));
        assertFalse(OriginUtils.isValidOrigin("https://example.com/#frag"));
        assertFalse(OriginUtils.isValidOrigin("https://user:pass@example.com"));
    }

    @Test
    void shouldRejectIllegalPortAndScheme() {
        assertFalse(OriginUtils.isValidOrigin("ftp://example.com"));
        assertFalse(OriginUtils.isValidOrigin("https://example.com:0"));
        assertFalse(OriginUtils.isValidOrigin("https://example.com:65536"));
        assertFalse(OriginUtils.isValidOrigin("https://example.com:"));
        assertFalse(OriginUtils.isValidOrigin("not-an-origin"));
    }
}
