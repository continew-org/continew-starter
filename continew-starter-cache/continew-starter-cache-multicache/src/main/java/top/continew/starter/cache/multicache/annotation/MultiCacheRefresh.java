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

package top.continew.starter.cache.multicache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存定时刷新注解
 *
 * <p>
 * 标注在方法上，需配合 {@code @Cacheable} 使用（同一方法，且 @Cacheable 不能使用 SpEL key，
 * 使用默认键生成器或显式 keyGenerator 均可）。方法首次被调用并缓存后，后台调度线程将按固定周期
 * 先失效该缓存键，再经代理重新执行方法并写回新值（等效于 JetCache 的 {@code @CacheRefresh}）。
 * </p>
 * <p>
 * 注意：刷新任务按“方法 + 实参”注册且数量有上限，参数取值发散的方法不适合使用本注解；
 * 集群部署时每个节点独立刷新（节点级单飞），如需集群单点刷新，请改用 schedule 模块
 * 的分布式定时任务主动调用 {@code CacheUtils.put}。
 * </p>
 *
 * @author KAI
 * @since 3.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiCacheRefresh {

    /**
     * 刷新周期（默认 7200，即 2 小时）
     *
     * @return 刷新周期
     */
    long refresh() default 7200;

    /**
     * 刷新周期时间单位（默认秒）
     *
     * @return 刷新周期时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 方法执行失败后重试间隔（单位同 {@link #timeUnit()}，默认 60）
     *
     * @return 方法执行失败后重试间隔
     */
    long retryAfterFailure() default 60;
}
