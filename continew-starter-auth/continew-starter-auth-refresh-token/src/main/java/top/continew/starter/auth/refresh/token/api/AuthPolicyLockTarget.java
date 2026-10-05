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

package top.continew.starter.auth.refresh.token.api;

/**
 * 认证会话安全策略锁的目标。
 *
 * @param type 锁目标类型
 * @param key  锁目标标识
 * @author luoqiz
 * @since 2.17.0
 */
public record AuthPolicyLockTarget(Type type, String key) {

    public static AuthPolicyLockTarget client(String clientId) {
        return new AuthPolicyLockTarget(Type.CLIENT, clientId);
    }

    public static AuthPolicyLockTarget tenant(Long tenantId) {
        return new AuthPolicyLockTarget(Type.TENANT, String.valueOf(tenantId));
    }

    public static AuthPolicyLockTarget user(Long userId) {
        return new AuthPolicyLockTarget(Type.USER, String.valueOf(userId));
    }

    /**
     * 锁目标类型
     */
    public enum Type {
        USER,
        TENANT,
        CLIENT
    }
}
