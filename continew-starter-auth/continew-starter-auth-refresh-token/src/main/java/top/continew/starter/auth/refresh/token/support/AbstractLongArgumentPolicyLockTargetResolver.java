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
import top.continew.starter.auth.refresh.token.api.AuthPolicyLockTargetResolver;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * 按约定的业务方法参数解析单一 Long 维度策略锁目标的基类。
 *
 * <p>用户与租户两个维度共用同一套参数扫描约定：取方法参数里第一个 {@code Long} 或
 * 第一个 {@code Long} 集合，差异仅在目标工厂上，由子类经 {@code targetFactory} 注入。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
abstract class AbstractLongArgumentPolicyLockTargetResolver
    implements AuthPolicyLockTargetResolver {

    private final Function<Long, AuthPolicyLockTarget> targetFactory;
    private final String errorMessage;

    AbstractLongArgumentPolicyLockTargetResolver(Function<Long, AuthPolicyLockTarget> targetFactory,
        String errorMessage) {
        this.targetFactory = targetFactory;
        this.errorMessage = errorMessage;
    }

    @Override
    public Collection<AuthPolicyLockTarget> resolve(Object[] args) {
        for (Object value : args) {
            if (value instanceof Long id) {
                return List.of(targetFactory.apply(id));
            }
            if (value instanceof Collection<?> values) {
                List<AuthPolicyLockTarget> targets = values.stream().filter(Long.class::isInstance)
                    .map(Long.class::cast).map(targetFactory).toList();
                if (!targets.isEmpty()) {
                    return targets;
                }
            }
        }
        throw new IllegalArgumentException(errorMessage);
    }
}
