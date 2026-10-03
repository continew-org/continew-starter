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

import top.continew.starter.auth.refresh.token.api.AuthPolicyLockTarget;

/**
 * 根据约定的业务方法参数解析租户策略锁目标。
 *
 * @author luoqiz
 * @since 2.17.0
 */
public class TenantArgumentPolicyLockTargetResolver
    extends AbstractLongArgumentPolicyLockTargetResolver {

    public TenantArgumentPolicyLockTargetResolver() {
        super(AuthPolicyLockTarget::tenant, "认证租户策略锁参数无效");
    }
}
