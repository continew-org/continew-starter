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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import top.continew.starter.ratelimiter.annotation.RateLimiter;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterExpireProperties;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterProperties;
import top.continew.starter.ratelimiter.generator.DefaultRateLimiterNameGenerator;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 限流器切面 Key 过期取值来源与冲突兜底测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringJUnitConfig(RateLimiterExpireBehaviorTest.ContextConfig.class)
class RateLimiterExpireBehaviorTest {

    /**
     * 为 {@code RedisUtils} 的静态初始化提供 Hutool Spring 上下文
     */
    @Configuration(proxyBeanMethods = false)
    @Import(SpringUtil.class)
    static class ContextConfig {

        /**
         * Redisson 客户端占位实现
         *
         * @return Redisson 客户端占位实现
         */
        @Bean
        RedissonClient redissonClient() {
            return mock(RedissonClient.class);
        }
    }

    private RateLimiterExpireProperties expireProperties;
    private RedissonClient redissonClient;
    private RateLimiterAspect aspect;

    @BeforeEach
    void setUp() {
        expireProperties = new RateLimiterExpireProperties();
        redissonClient = mock(RedissonClient.class);
        aspect = new RateLimiterAspect(new RateLimiterProperties(), expireProperties,
            new DefaultRateLimiterNameGenerator(), redissonClient);
    }

    /**
     * 全局开启时取全局配置的时长与单位，而非内置默认的 1 小时
     */
    @Test
    void shouldUseConfiguredGlobalTimeAndUnit() throws Throwable {
        expireProperties.setEnabled(true);
        expireProperties.setTime(30);
        expireProperties.setUnit(TimeUnit.MINUTES);
        RateLimiter annotation = this.annotationOf("annotatedGlobalCustom");
        RRateLimiter rRateLimiter = this.mockRateLimiter("behavior-global-custom", annotation);
        this.invoke(annotation, "annotatedGlobalCustom");
        verify(rRateLimiter).expire(Duration.ofMinutes(30));
        verify(rRateLimiter, never()).expire(Duration.ofHours(1));
    }

    /**
     * 注解取其他负值时同样跟随全局配置
     */
    @Test
    void shouldFollowGlobalWhenAnnotationExpireIsOtherNegative() throws Throwable {
        expireProperties.setEnabled(true);
        expireProperties.setTime(30);
        expireProperties.setUnit(TimeUnit.MINUTES);
        RateLimiter annotation = this.annotationOf("annotatedOtherNegative");
        RRateLimiter rRateLimiter = this.mockRateLimiter("behavior-negative", annotation);
        this.invoke(annotation, "annotatedOtherNegative");
        verify(rRateLimiter).expire(Duration.ofMinutes(30));
        verify(rRateLimiter, never()).expire(Duration.ofMinutes(5));
    }

    /**
     * 全局配置的过期时长同样受速率间隔兜底
     */
    @Test
    void shouldClampGlobalExpireToRateInterval() throws Throwable {
        expireProperties.setEnabled(true);
        expireProperties.setTime(30);
        expireProperties.setUnit(TimeUnit.MINUTES);
        RateLimiter annotation = this.annotationOf("annotatedGlobalShort");
        RRateLimiter rRateLimiter = this.mockRateLimiter("behavior-global-short", annotation);
        this.invoke(annotation, "annotatedGlobalShort");
        verify(rRateLimiter).expire(Duration.ofHours(2));
        verify(rRateLimiter, never()).expire(Duration.ofMinutes(30));
    }

    /**
     * 全局关闭时注解正值独立生效，不依赖全局开关
     */
    @Test
    void shouldApplyAnnotationExpireWhenGlobalDisabled() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedIndependent");
        RRateLimiter rRateLimiter = this.mockRateLimiter("behavior-independent", annotation);
        this.invoke(annotation, "annotatedIndependent");
        verify(rRateLimiter).expire(Duration.ofMinutes(5));
    }

    /**
     * 限流器配置与注解一致时不触发 setRate 更新，保证只有过期行为被测量
     */
    @Test
    void shouldNotUpdateRateWhenConfigMatchesAnnotation() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedMatched");
        RRateLimiter rRateLimiter = this.mockRateLimiter("behavior-matched", annotation);
        this.invoke(annotation, "annotatedMatched");
        verify(rRateLimiter, never()).setRate(any(RateType.class), anyLong(), any(Duration.class));
    }

    /**
     * 限流器配置与注解不一致时先更新速率，随后仍刷新 TTL
     */
    @Test
    void shouldRefreshExpireAfterRateUpdate() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedStale");
        RRateLimiter rRateLimiter = mock(RRateLimiter.class);
        // 故意返回与注解不一致的旧配置：速率 1、间隔 1 毫秒
        RateLimiterConfig stale = new RateLimiterConfig(RateType.OVERALL, 1L, 1L);
        when(rRateLimiter.getConfig()).thenReturn(stale);
        when(rRateLimiter.tryAcquire()).thenReturn(true);
        when(redissonClient.getRateLimiter(contains("behavior-stale"))).thenReturn(rRateLimiter);
        this.invoke(annotation, "annotatedStale");
        InOrder inOrder = inOrder(rRateLimiter);
        inOrder.verify(rRateLimiter).setRate(RateType.OVERALL, 20, Duration.ofMillis(500));
        inOrder.verify(rRateLimiter).expire(Duration.ofMinutes(5));
        inOrder.verify(rRateLimiter).tryAcquire();
    }

    private void invoke(RateLimiter annotation, String methodName) throws Throwable {
        ProceedingJoinPoint joinPoint = this.mockJoinPoint(this.methodOf(methodName));
        aspect.aroundRateLimiter(joinPoint, annotation);
    }

    private Method methodOf(String name) throws NoSuchMethodException {
        return RateLimiterExpireBehaviorTest.class.getDeclaredMethod(name);
    }

    private RateLimiter annotationOf(String name) throws NoSuchMethodException {
        return this.methodOf(name).getAnnotation(RateLimiter.class);
    }

    private ProceedingJoinPoint mockJoinPoint(Method method) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn(new Object());
        return joinPoint;
    }

    private RRateLimiter mockRateLimiter(String namePart, RateLimiter annotation) {
        RRateLimiter rRateLimiter = mock(RRateLimiter.class);
        // 构造参数顺序：限流类型、速率间隔、速率
        RateLimiterConfig config = new RateLimiterConfig(RateType.OVERALL,
            annotation.unit().toMillis(annotation.interval()),
            (long) annotation.rate());
        when(rRateLimiter.getConfig()).thenReturn(config);
        when(rRateLimiter.tryAcquire()).thenReturn(true);
        when(redissonClient.getRateLimiter(contains(namePart))).thenReturn(rRateLimiter);
        return rRateLimiter;
    }

    @RateLimiter(name = "behavior-global-custom")
    private void annotatedGlobalCustom() {
    }

    /**
     * 速率间隔 2 小时，大于全局过期时长 30 分钟
     */
    @RateLimiter(name = "behavior-global-short", rate = 10, interval = 2, unit = TimeUnit.HOURS)
    private void annotatedGlobalShort() {
    }

    @RateLimiter(name = "behavior-negative", expire = -2)
    private void annotatedOtherNegative() {
    }

    @RateLimiter(name = "behavior-independent", expire = 5, expireUnit = TimeUnit.MINUTES)
    private void annotatedIndependent() {
    }

    @RateLimiter(name = "behavior-matched", rate = 20, interval = 500, unit = TimeUnit.MILLISECONDS,
        expire = 5, expireUnit = TimeUnit.MINUTES)
    private void annotatedMatched() {
    }

    @RateLimiter(name = "behavior-stale", rate = 20, interval = 500, unit = TimeUnit.MILLISECONDS,
        expire = 5, expireUnit = TimeUnit.MINUTES)
    private void annotatedStale() {
    }
}
