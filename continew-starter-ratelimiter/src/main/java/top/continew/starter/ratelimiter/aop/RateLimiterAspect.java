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

package top.continew.starter.ratelimiter.aop;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.ServletUtils;
import top.continew.starter.core.util.expression.ExpressionUtils;
import top.continew.starter.ratelimiter.annotation.RateLimiter;
import top.continew.starter.ratelimiter.annotation.RateLimiters;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterExpireProperties;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterProperties;
import top.continew.starter.ratelimiter.enums.LimitType;
import top.continew.starter.ratelimiter.exception.RateLimiterException;
import top.continew.starter.ratelimiter.generator.RateLimiterNameGenerator;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器切面
 *
 * @author KAI
 * @author Charles7c
 * @since 2.2.0
 */
@Aspect
public class RateLimiterAspect {

    private static final ConcurrentHashMap<String, RRateLimiter> RATE_LIMITER_CACHE =
        new ConcurrentHashMap<>();
    private final RateLimiterProperties properties;
    private final RateLimiterExpireProperties expireProperties;
    private final RateLimiterNameGenerator nameGenerator;
    private final RedissonClient redissonClient;

    public RateLimiterAspect(RateLimiterProperties properties,
        RateLimiterExpireProperties expireProperties,
        RateLimiterNameGenerator nameGenerator,
        RedissonClient redissonClient) {
        this.properties = properties;
        this.expireProperties = expireProperties;
        this.nameGenerator = nameGenerator;
        this.redissonClient = redissonClient;
    }

    /**
     * 单个限流注解切点
     */
    @Pointcut("@annotation(top.continew.starter.ratelimiter.annotation.RateLimiter)")
    public void rateLimiterPointCut() {
    }

    /**
     * 多个限流注解切点
     */
    @Pointcut("@annotation(top.continew.starter.ratelimiter.annotation.RateLimiters)")
    public void rateLimitersPointCut() {
    }

    /**
     * 单限流场景
     *
     * @param joinPoint   切点
     * @param rateLimiter 限流注解
     * @return 目标方法的执行结果
     * @throws Throwable /
     */
    @Around("@annotation(rateLimiter)")
    public Object aroundRateLimiter(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter)
        throws Throwable {
        if (isRateLimited(joinPoint, rateLimiter)) {
            throw new RateLimiterException(rateLimiter.message());
        }
        return joinPoint.proceed();
    }

    /**
     * 多限流场景
     *
     * @param joinPoint    切点
     * @param rateLimiters 限流组注解
     * @return 目标方法的执行结果
     * @throws Throwable /
     */
    @Around("@annotation(rateLimiters)")
    public Object aroundRateLimiters(ProceedingJoinPoint joinPoint, RateLimiters rateLimiters)
        throws Throwable {
        for (RateLimiter rateLimiter : rateLimiters.value()) {
            if (isRateLimited(joinPoint, rateLimiter)) {
                throw new RateLimiterException(rateLimiter.message());
            }
        }
        return joinPoint.proceed();
    }

    /**
     * 是否需要限流
     *
     * @param joinPoint   切点
     * @param rateLimiter 限流注解
     * @return true: 需要限流；false：不需要限流
     */
    private boolean isRateLimited(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) {
        try {
            String cacheKey = this.getCacheKey(joinPoint, rateLimiter);
            RRateLimiter rRateLimiter =
                RATE_LIMITER_CACHE.computeIfAbsent(cacheKey, key -> redissonClient
                    .getRateLimiter(cacheKey));
            // 限流器配置
            RateType rateType =
                rateLimiter.type() == LimitType.CLUSTER ? RateType.PER_CLIENT : RateType.OVERALL;
            int rate = rateLimiter.rate();
            Duration rateInterval =
                Duration.ofMillis(rateLimiter.unit().toMillis(rateLimiter.interval()));
            // 判断是否需要更新限流器
            if (this.isConfigurationUpdateNeeded(rRateLimiter, rateType, rate, rateInterval)) {
                // 更新限流器
                rRateLimiter.setRate(rateType, rate, rateInterval);
            }
            // 刷新 Key 过期时间（延迟过期：活跃限流器不失效，空闲超过过期时长后 Key 到期删除）
            Duration expireDuration = this.resolveExpireDuration(rateLimiter, rateInterval);
            if (expireDuration != null) {
                rRateLimiter.expire(expireDuration);
            }
            // 尝试获取令牌
            return !rRateLimiter.tryAcquire();
        } catch (Exception e) {
            throw new RateLimiterException("服务器限流异常，请稍候再试", e);
        }
    }

    /**
     * 解析限流器 Key 过期时长
     * <p>
     * 优先级：注解参数 &gt; 全局配置 &gt; 默认关闭。注解 expire 取 0 时表示显式关闭（可覆盖全局开启），
     * 取负值时跟随全局配置。过期时长小于速率间隔会破坏令牌桶语义（Key 在补发周期内被删除导致限流被重置），
     * 此时以速率间隔兜底。
     *
     * @param rateLimiter  限流注解
     * @param rateInterval 速率间隔
     * @return 过期时长，不需要过期时返回 null
     */
    private Duration resolveExpireDuration(RateLimiter rateLimiter, Duration rateInterval) {
        long expire = rateLimiter.expire();
        // 注解显式关闭过期
        if (expire == 0) {
            return null;
        }
        Duration expireDuration;
        if (expire > 0) {
            expireDuration = Duration.ofMillis(rateLimiter.expireUnit().toMillis(expire));
        } else {
            // 跟随全局配置
            if (!this.expireProperties.isEnabled()) {
                return null;
            }
            expireDuration = Duration.ofMillis(this.expireProperties.getUnit()
                .toMillis(this.expireProperties.getTime()));
        }
        return expireDuration.compareTo(rateInterval) < 0 ? rateInterval : expireDuration;
    }

    /**
     * 获取缓存 Key
     *
     * @param joinPoint   切点
     * @param rateLimiter 限流注解
     * @return 缓存 Key
     */
    private String getCacheKey(JoinPoint joinPoint, RateLimiter rateLimiter) {
        Object target = joinPoint.getTarget();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        // 获取名称
        String name = rateLimiter.name();
        if (CharSequenceUtil.isBlank(name)) {
            name = nameGenerator.generate(target, method, args);
        }
        // 解析 Key
        String key = rateLimiter.key();
        if (CharSequenceUtil.isNotBlank(key)) {
            Object eval = ExpressionUtils.eval(key, target, method, args);
            if (eval == null) {
                throw new RateLimiterException("限流 Key 解析错误");
            }
            key = Convert.toStr(eval);
        }
        // 获取后缀
        String suffix = switch (rateLimiter.type()) {
            case IP -> ServletUtils.getClientIP(ServletUtils.getRequest());
            case CLUSTER -> redissonClient.getId();
            default -> StringConstants.EMPTY;
        };
        return RedisUtils.formatKey(properties.getKeyPrefix(), name, key, suffix);
    }

    /**
     * 判断是否需要更新限流器配置
     *
     * @param rRateLimiter 限流器
     * @param rateType     限流类型（OVERALL：全局限流；PER_CLIENT：单机限流）
     * @param rate         速率（指定时间间隔产生的令牌数）
     * @param rateInterval 速率间隔
     * @return 是否需要更新配置
     */
    private boolean isConfigurationUpdateNeeded(RRateLimiter rRateLimiter,
        RateType rateType,
        long rate,
        Duration rateInterval) {
        RateLimiterConfig config = rRateLimiter.getConfig();
        return !Objects.equals(config.getRateType(), rateType)
            || !Objects.equals(config.getRate(), rate) || !Objects
                .equals(config.getRateInterval(), rateInterval.toMillis());
    }
}
