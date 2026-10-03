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

import java.util.Collection;

/**
 * 将业务方法参数解析为认证会话策略锁目标。
 *
 * <p>实现由业务模块按需注册，认证会话模块不引用任何业务实体或 Mapper。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public interface AuthPolicyLockTargetResolver {

    /**
     * 从业务方法参数中解析策略锁目标。
     *
     * @param args 拦截的业务方法参数
     * @return 解析到的锁目标集合
     */
    Collection<AuthPolicyLockTarget> resolve(Object[] args);
}
