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

package top.continew.starter.ratelimiter.annotation;

import top.continew.starter.ratelimiter.enums.LimitType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 *
 * @author KAI
 * @since 2.2.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * 类型
     *
     * @return 类型
     */
    LimitType type() default LimitType.DEFAULT;

    /**
     * 名称
     *
     * @return 名称
     */
    String name() default "";

    /**
     * 键（支持 Spring EL 表达式）
     *
     * @return 键
     */
    String key() default "";

    /**
     * 速率（指定时间间隔产生的令牌数）
     *
     * @return 速率
     */
    int rate() default Integer.MAX_VALUE;

    /**
     * 速率间隔（时间间隔）
     *
     * @return 速率间隔
     */
    int interval() default 0;

    /**
     * 速率间隔时间单位（默认：毫秒）
     *
     * @return 速率间隔时间单位
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * Key 过期时长（延迟过期）
     * <p>
     * 限流器每次被访问时刷新 Key 的 TTL，活跃限流器不失效，空闲超过该时长后 Key 到期删除。取值语义：
     * <ul>
     * <li>负值（默认 {@code -1}）：跟随全局配置 {@code continew-starter.rate-limiter.expire.enabled}；</li>
     * <li>{@code 0}：显式关闭过期，Key 永不过期（可覆盖全局开启）；</li>
     * <li>正值：注解级过期时长，覆盖全局配置。</li>
     * </ul>
     * 过期时长小于速率间隔会破坏限流语义，实际生效值不会低于速率间隔。
     *
     * @return Key 过期时长
     * @since 3.0.0
     */
    long expire() default -1;

    /**
     * Key 过期时长单位（默认：小时），仅在 expire 取正值时生效
     *
     * @return Key 过期时长单位
     * @since 3.0.0
     */
    TimeUnit expireUnit() default TimeUnit.HOURS;

    /**
     * 提示信息
     *
     * @return 提示信息
     */
    String message() default "操作过于频繁，请稍后再试";
}
