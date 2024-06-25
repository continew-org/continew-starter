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

package top.continew.starter.security.limiter.annotation;

import org.redisson.api.RateIntervalUnit;
import top.continew.starter.security.limiter.enums.LimitType;

import java.lang.annotation.*;

/**
 * 限流注解
 * @author KAI
 * @since 2.2.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * LimitType 限流模式
     * DEFAULT 全局限流
     * IP IP限流
     * CLUSTER 实例限流
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 缓存实例名称
     */
    String name() default "";

    /**
     * 限流key 支持 Spring EL 表达式
     */
    String key() default "";

    /**
     * 单位时间产生的令牌数
     */
    int rate() default Integer.MAX_VALUE;

    /**
     * 限流时间
     */
    int rateInterval() default 0;

    /**
     * 时间单位，默认毫秒
     */
    RateIntervalUnit timeUnit() default RateIntervalUnit.MILLISECONDS;

    /**
     * 拒绝请求时的提示信息
     */
    String message() default "您操作过于频繁，请稍后再试！";
}