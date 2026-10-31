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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import top.continew.starter.ratelimiter.annotation.RateLimiters;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterExpireProperties;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterProperties;
import top.continew.starter.ratelimiter.generator.DefaultRateLimiterNameGenerator;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 限流器切面 Key 过期（延迟过期）测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringJUnitConfig(RateLimiterAspectTest.ContextConfig.class)
class RateLimiterAspectTest {

    /**
     * 仅为单元测试提供 Hutool Spring 上下文：{@code RedisUtils} 的静态初始化经
     * {@code SpringUtil} 获取 {@code RedissonClient}，此处注册占位 Bean 满足依赖要求。
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

    private RateLimiterProperties properties;
    private RateLimiterExpireProperties expireProperties;
    private RedissonClient redissonClient;
    private RateLimiterAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new RateLimiterProperties();
        expireProperties = new RateLimiterExpireProperties();
        redissonClient = mock(RedissonClient.class);
        aspect = new RateLimiterAspect(properties, expireProperties,
            new DefaultRateLimiterNameGenerator(), redissonClient);
    }

    /**
     * 默认（注解未配置过期 + 全局未开启）不设置 TTL，行为与历史版本一致
     */
    @Test
    void shouldNotExpireWhenDisabledByDefault() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedDefault");
        RRateLimiter rRateLimiter = this.mockRateLimiter("test-default", annotation);
        this.invoke(annotation, "annotatedDefault");
        verify(rRateLimiter, never()).expire(any(Duration.class));
    }

    /**
     * 全局开启后按全局时长设置 TTL，且在尝试获取令牌之前刷新
     */
    @Test
    void shouldExpireWhenGloballyEnabled() throws Throwable {
        expireProperties.setEnabled(true);
        RateLimiter annotation = this.annotationOf("annotatedGlobal");
        RRateLimiter rRateLimiter = this.mockRateLimiter("test-global", annotation);
        this.invoke(annotation, "annotatedGlobal");
        InOrder inOrder = inOrder(rRateLimiter);
        inOrder.verify(rRateLimiter).expire(Duration.ofHours(1));
        inOrder.verify(rRateLimiter).tryAcquire();
    }

    /**
     * 延迟过期语义：限流器每次被访问时刷新 TTL
     */
    @Test
    void shouldRefreshExpireOnEachAccess() throws Throwable {
        expireProperties.setEnabled(true);
        RateLimiter annotation = this.annotationOf("annotatedRefresh");
        RRateLimiter rRateLimiter = this.mockRateLimiter("test-refresh", annotation);
        this.invoke(annotation, "annotatedRefresh");
        this.invoke(annotation, "annotatedRefresh");
        verify(rRateLimiter, times(2)).expire(Duration.ofHours(1));
    }

    /**
     * 注解级过期时长覆盖全局配置
     */
    @Test
    void shouldUseAnnotationExpireOverGlobal() throws Throwable {
        expireProperties.setEnabled(true);
        RateLimiter annotation = this.annotationOf("annotatedExpire");
        RRateLimiter rRateLimiter = this.mockRateLimiter("test-annotation", annotation);
        this.invoke(annotation, "annotatedExpire");
        verify(rRateLimiter).expire(Duration.ofMinutes(5));
        verify(rRateLimiter, never()).expire(Duration.ofHours(1));
    }

    /**
     * 全局开启时，注解取 0 可显式关闭过期
     */
    @Test
    void shouldDisableExpireByAnnotationWhenGloballyEnabled() throws Throwable {
        expireProperties.setEnabled(true);
        RateLimiter annotation = this.annotationOf("annotatedDisabled");
        RRateLimiter rRateLimiter = this.mockRateLimiter("test-disabled", annotation);
        this.invoke(annotation, "annotatedDisabled");
        verify(rRateLimiter, never()).expire(any(Duration.class));
    }

    /**
     * 过期时长小于速率间隔时以速率间隔兜底，避免令牌桶在补发周期内被删除导致限流重置
     */
    @Test
    void shouldNotExpireShorterThanRateInterval() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedShortExpire");
        RRateLimiter rRateLimiter = this.mockRateLimiter("test-short", annotation);
        this.invoke(annotation, "annotatedShortExpire");
        verify(rRateLimiter).expire(Duration.ofSeconds(1));
        verify(rRateLimiter, never()).expire(Duration.ofMillis(100));
    }

    /**
     * 组注解场景下每个子注解独立取值：显式配置的子注解用注解时长，未配置的子注解跟随全局
     */
    @Test
    void shouldExpireGroupAnnotationsIndependently() throws Throwable {
        expireProperties.setEnabled(true);
        Method method = this.methodOf("annotatedGroup");
        RateLimiters group = method.getAnnotation(RateLimiters.class);
        RRateLimiter explicit = this.mockRateLimiter("group-explicit", group.value()[0]);
        RRateLimiter follow = this.mockRateLimiter("group-follow", group.value()[1]);
        ProceedingJoinPoint joinPoint = this.mockJoinPoint(method);
        aspect.aroundRateLimiters(joinPoint, group);
        verify(explicit).expire(Duration.ofMinutes(5));
        verify(follow).expire(Duration.ofHours(1));
    }

    private void invoke(RateLimiter annotation, String methodName) throws Throwable {
        ProceedingJoinPoint joinPoint = this.mockJoinPoint(this.methodOf(methodName));
        aspect.aroundRateLimiter(joinPoint, annotation);
    }

    private Method methodOf(String name) throws NoSuchMethodException {
        return RateLimiterAspectTest.class.getDeclaredMethod(name);
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
        // 与注解配置一致的限流器配置，避免触发 setRate 更新（构造参数顺序：类型、速率间隔、速率）
        RateLimiterConfig config = new RateLimiterConfig(RateType.OVERALL,
            annotation.unit().toMillis(annotation.interval()),
            (long) annotation.rate());
        when(rRateLimiter.getConfig()).thenReturn(config);
        when(rRateLimiter.tryAcquire()).thenReturn(true);
        when(redissonClient.getRateLimiter(contains(namePart))).thenReturn(rRateLimiter);
        return rRateLimiter;
    }

    @RateLimiter(name = "test-default")
    private void annotatedDefault() {
    }

    @RateLimiter(name = "test-global")
    private void annotatedGlobal() {
    }

    @RateLimiter(name = "test-refresh")
    private void annotatedRefresh() {
    }

    @RateLimiter(name = "test-annotation", expire = 5, expireUnit = TimeUnit.MINUTES)
    private void annotatedExpire() {
    }

    @RateLimiter(name = "test-disabled", expire = 0)
    private void annotatedDisabled() {
    }

    @RateLimiter(name = "test-short", rate = 10, interval = 1, unit = TimeUnit.SECONDS,
        expire = 100, expireUnit = TimeUnit.MILLISECONDS)
    private void annotatedShortExpire() {
    }

    @RateLimiters({@RateLimiter(name = "group-explicit", expire = 5, expireUnit = TimeUnit.MINUTES),
        @RateLimiter(name = "group-follow")})
    private void annotatedGroup() {
    }
}
