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
import top.continew.starter.auth.refresh.token.api.AuthPolicyLockTarget;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 用户策略锁参数解析测试。
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
class UserArgumentPolicyLockTargetResolverTest {

    private final UserArgumentPolicyLockTargetResolver resolver =
        new UserArgumentPolicyLockTargetResolver();

    @Test
    void shouldResolveUserIdFromUpdatePasswordSignature() {
        List<AuthPolicyLockTarget> targets = List.copyOf(resolver.resolve(
            new Object[] {"old-password", "new-password", 1L}));

        assertEquals(List.of(AuthPolicyLockTarget.user(1L)), targets);
    }

    @Test
    void shouldResolveUserIdsFromCollectionArgument() {
        List<AuthPolicyLockTarget> targets = List.copyOf(resolver.resolve(
            new Object[] {"ignored", List.of(1L, 2L)}));

        assertEquals(List.of(AuthPolicyLockTarget.user(1L), AuthPolicyLockTarget.user(2L)),
            targets);
    }

    @Test
    void shouldRejectArgumentsWithoutUserTarget() {
        assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(new Object[] {"old-password", "new-password"}));
    }
}
