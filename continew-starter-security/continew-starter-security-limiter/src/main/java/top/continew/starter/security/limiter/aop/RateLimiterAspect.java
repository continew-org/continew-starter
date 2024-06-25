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

package top.continew.starter.security.limiter.aop;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.continew.starter.security.limiter.annotation.RateLimiter;
import top.continew.starter.security.limiter.annotation.RateLimiters;
import top.continew.starter.security.limiter.autoconfigure.RateLimiterProperties;
import top.continew.starter.security.limiter.enums.LimitType;
import top.continew.starter.security.limiter.exception.RateLimiterException;
import top.continew.starter.web.util.ServletUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流 AOP 拦截器
 *
 * @author KAI
 * @since 2.2.0
 */
@Aspect
@Component
public class RateLimiterAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    private static final ConcurrentHashMap<String, RRateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    private static final RedissonClient CLIENT = SpringUtil.getBean(RedissonClient.class);

    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParserContext parserContext = new TemplateParserContext();

    @Autowired
    private RateLimiterProperties rateLimiterProperties;

    /**
     * 单个限流注解切点
     */
    @Pointcut("@annotation(top.continew.starter.security.limiter.annotation.RateLimiter)")
    public void rateLimiterSinglePointCut() {
    }

    /**
     * 多个限流注解切点
     */
    @Pointcut("@annotation(top.continew.starter.security.limiter.annotation.RateLimiters)")
    public void rateLimiterBatchPointCut() {
    }

    /**
     * 环绕通知，处理单个限流注解
     *
     * @param joinPoint   切点
     * @param rateLimiter 限流注解
     * @return 返回目标方法的执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(rateLimiter)")
    public Object aroundSingle(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        // 未开启限流功能，直接执行目标方法
        if (!rateLimiterProperties.isEnabled()) {
            return joinPoint.proceed();
        }
        if (isRateLimited(joinPoint, rateLimiter)) {
            throw new RateLimiterException(rateLimiter.message());
        }
        return joinPoint.proceed();
    }

    /**
     * 环绕通知，处理多个限流注解
     *
     * @param joinPoint    切点
     * @param rateLimiters 多个限流注解
     * @return 返回目标方法的执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(rateLimiters)")
    public Object aroundBatch(ProceedingJoinPoint joinPoint, RateLimiters rateLimiters) throws Throwable {
        // 未开启限流功能，直接执行目标方法
        if (!rateLimiterProperties.isEnabled()) {
            return joinPoint.proceed();
        }
        for (RateLimiter rateLimiter : rateLimiters.value()) {
            if (isRateLimited(joinPoint, rateLimiter)) {
                throw new RateLimiterException(rateLimiter.message());
            }
        }
        return joinPoint.proceed();
    }

    /**
     * 执行限流逻辑
     *
     * @param joinPoint   切点
     * @param rateLimiter 限流注解
     * @throws Throwable 异常
     */
    private boolean isRateLimited(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        try {
            // 生成限流 Key
            String redisKey = generateRedisKey(rateLimiter, joinPoint);
            String encipherKey = SecureUtil.md5(redisKey);
            // 确定限流类型
            RateType rateType = rateLimiter.limitType() == LimitType.CLUSTER ? RateType.PER_CLIENT : RateType.OVERALL;

            // 获取redisson限流实例
            RRateLimiter rRateLimiter = getRateLimiter(encipherKey);
            RateIntervalUnit rateIntervalUnit = rateLimiter.timeUnit();
            int rateInterval = rateLimiter.rateInterval();
            int rate = rateLimiter.rate();
            // 判断是否需要更新限流器
            if (shouldUpdateRateLimiter(rRateLimiter, rateType, rate, rateInterval, rateIntervalUnit)) {
                // 更新限流器
                rRateLimiter.setRate(rateType, rate, rateInterval, rateIntervalUnit);
            }
            // 尝试获取令牌
            return !rRateLimiter.tryAcquire();
        } catch (RateLimiterException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("服务器限流异常，请稍候再试", e);
        }
    }

    /**
     * 获取 Redisson RateLimiter 实例
     *
     * @param key 限流器的 Key
     * @return RateLimiter 实例
     */
    private RRateLimiter getRateLimiter(String key) {
        RRateLimiter rRateLimiter = rateLimiterCache.get(key);
        if (rRateLimiter == null) {
            // 直接创建 RateLimiter 实例
            rRateLimiter = CLIENT.getRateLimiter(key);
            rateLimiterCache.put(key, rRateLimiter);
        }
        return rRateLimiter;
    }

    /**
     * 判断是否需要更新限流器配置
     *
     * @param rRateLimiter     现有的限流器
     * @param rateType         限流类型（OVERALL：全局限流；PER_CLIENT：单机限流）
     * @param rate             速率（指定时间间隔产生的令牌数）
     * @param rateInterval     速率间隔
     * @param rateIntervalUnit 时间单位
     * @return 是否需要更新配置
     */
    private boolean shouldUpdateRateLimiter(RRateLimiter rRateLimiter,
                                            RateType rateType,
                                            long rate,
                                            long rateInterval,
                                            RateIntervalUnit rateIntervalUnit) {

        RateLimiterConfig config = rRateLimiter.getConfig();
        return !Objects.equals(config.getRateType(), rateType) || !Objects.equals(config.getRate(), rate) || !Objects
            .equals(config.getRateInterval(), rateIntervalUnit.toMillis(rateInterval));
    }

    /**
     * 获取限流Key
     *
     * @param rateLimiter RateLimiter实例
     * @param point       切点
     * @return 限流Key
     */
    private String generateRedisKey(RateLimiter rateLimiter, JoinPoint point) {
        // 获取限流器配置的 key
        String key = rateLimiter.key();
        // 如果 key 不为空，则解析表达式并获取最终的 key
        key = Optional.ofNullable(key).map(k -> {
            // 获取方法签名
            MethodSignature signature = (MethodSignature)point.getSignature();
            // 获取方法参数
            Object[] args = point.getArgs();
            // 创建表达式上下文
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, signature
                .getMethod(), args, discoverer);
            // 设置 Bean 解析器
            context.setBeanResolver(new BeanFactoryResolver(SpringUtil.getBeanFactory()));
            // 解析表达式
            Expression expression;
            if (StringUtils.startsWithIgnoreCase(k, parserContext.getExpressionPrefix()) && StringUtils
                .endsWithIgnoreCase(k, parserContext.getExpressionSuffix())) {
                expression = parser.parseExpression(k, parserContext);
            } else {
                expression = parser.parseExpression(k);
            }
            // 获取表达式结果
            return expression.getValue(context, String.class);
        }).orElse(key);

        // 拼接最终的 key
        StringBuilder redisKey = new StringBuilder(rateLimiterProperties.getLimiterKey()).append(ServletUtils
            .getRequest()
            .getRequestURI()).append(":");
        //如果缓存name 不为空 则拼接上去
        String name = rateLimiter.name();
        if (StringUtils.hasText(name)) {
            redisKey.append(name);
            if (!name.endsWith(":")) {
                redisKey.append(":");
            }
        }
        // 根据限流类型添加不同的信息
        switch (rateLimiter.limitType()) {
            case IP:
                // 获取请求 IP
                redisKey.append(JakartaServletUtil.getClientIP(ServletUtils.getRequest())).append(":");
                break;
            case CLUSTER:
                // 获取客户端实例 ID
                redisKey.append(CLIENT.getId()).append(":");
                break;
            default:
                break;
        }
        // 添加解析后的 key
        return redisKey.append(key).toString();
    }
}
