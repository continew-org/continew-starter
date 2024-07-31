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

package top.continew.starter.security.limiter.core;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.*;
import org.springframework.stereotype.Component;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.expression.ExpressionUtils;
import top.continew.starter.security.limiter.annotation.RateLimiter;
import top.continew.starter.security.limiter.annotation.RateLimiters;
import top.continew.starter.security.limiter.autoconfigure.RateLimiterProperties;
import top.continew.starter.security.limiter.enums.LimitType;
import top.continew.starter.security.limiter.exception.RateLimiterException;
import top.continew.starter.web.util.SpringWebUtils;

import java.lang.reflect.Method;
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
@Component
public class RateLimiterAspect {

    private static final ConcurrentHashMap<String, RRateLimiter> RATE_LIMITER_CACHE = new ConcurrentHashMap<>();
    private final RateLimiterProperties properties;
    private final RateLimiterNameGenerator nameGenerator;
    private final RedissonClient redissonClient;

    public RateLimiterAspect(RateLimiterProperties properties,
                             RateLimiterNameGenerator nameGenerator,
                             RedissonClient redissonClient) {
        this.properties = properties;
        this.nameGenerator = nameGenerator;
        this.redissonClient = redissonClient;
    }

    /**
     * 单个限流注解切点
     */
    @Pointcut("@annotation(top.continew.starter.security.limiter.annotation.RateLimiter)")
    public void rateLimiterPointCut() {
    }

    /**
     * 多个限流注解切点
     */
    @Pointcut("@annotation(top.continew.starter.security.limiter.annotation.RateLimiters)")
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
    public Object aroundRateLimiter(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
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
    public Object aroundRateLimiters(ProceedingJoinPoint joinPoint, RateLimiters rateLimiters) throws Throwable {
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
            RRateLimiter rRateLimiter = RATE_LIMITER_CACHE.computeIfAbsent(cacheKey, key -> redissonClient
                .getRateLimiter(cacheKey));
            // 限流器配置
            RateType rateType = rateLimiter.type() == LimitType.CLUSTER ? RateType.PER_CLIENT : RateType.OVERALL;
            int rate = rateLimiter.rate();
            int rateInterval = rateLimiter.interval();
            RateIntervalUnit rateIntervalUnit = rateLimiter.unit();
            // 判断是否需要更新限流器
            if (this.isConfigurationUpdateNeeded(rRateLimiter, rateType, rate, rateInterval, rateIntervalUnit)) {
                // 更新限流器
                rRateLimiter.setRate(rateType, rate, rateInterval, rateIntervalUnit);
            }
            // 尝试获取令牌
            return !rRateLimiter.tryAcquire();
        } catch (Exception e) {
            throw new RateLimiterException("服务器限流异常，请稍候再试", e);
        }
    }

    /**
     * 获取限流缓存 Key
     *
     * @param joinPoint   切点
     * @param rateLimiter 限流注解
     * @return 限流缓存 Key
     */
    private String getCacheKey(JoinPoint joinPoint, RateLimiter rateLimiter) {
        Object target = joinPoint.getTarget();
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        // 获取限流名称
        String name = rateLimiter.name();
        if (CharSequenceUtil.isBlank(name)) {
            name = nameGenerator.generate(target, method, args);
        }
        // 解析限流 Key
        String key = rateLimiter.key();
        if (CharSequenceUtil.isNotBlank(key)) {
            Object eval = ExpressionUtils.eval(key, target, method, args);
            if (ObjectUtil.isNull(eval)) {
                throw new RateLimiterException("限流 Key 解析错误");
            }
            key = Convert.toStr(eval);
        }
        // 获取后缀
        String suffix = switch (rateLimiter.type()) {
            case IP -> JakartaServletUtil.getClientIP(SpringWebUtils.getRequest());
            case CLUSTER -> redissonClient.getId();
            default -> StringConstants.EMPTY;
        };
        return RedisUtils.formatKey(properties.getKeyPrefix(), name, key, suffix);
    }

    /**
     * 判断是否需要更新限流器配置
     *
     * @param rRateLimiter     限流器
     * @param rateType         限流类型（OVERALL：全局限流；PER_CLIENT：单机限流）
     * @param rate             速率（指定时间间隔产生的令牌数）
     * @param rateInterval     速率间隔
     * @param rateIntervalUnit 时间单位
     * @return 是否需要更新配置
     */
    private boolean isConfigurationUpdateNeeded(RRateLimiter rRateLimiter,
                                                RateType rateType,
                                                long rate,
                                                long rateInterval,
                                                RateIntervalUnit rateIntervalUnit) {
        RateLimiterConfig config = rRateLimiter.getConfig();
        return !Objects.equals(config.getRateType(), rateType) || !Objects.equals(config.getRate(), rate) || !Objects
            .equals(config.getRateInterval(), rateIntervalUnit.toMillis(rateInterval));
    }
}
