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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.ratelimiter.annotation.RateLimiter;
import top.continew.starter.ratelimiter.annotation.RateLimiters;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterExpireProperties;
import top.continew.starter.ratelimiter.autoconfigure.RateLimiterProperties;
import top.continew.starter.ratelimiter.enums.LimitType;
import top.continew.starter.ratelimiter.exception.RateLimiterException;
import top.continew.starter.ratelimiter.generator.DefaultRateLimiterNameGenerator;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 限流器切面 Key 解析与限流判定测试
 *
 * <p>覆盖缓存 Key 的四段拼装分支（注解名/生成器名、SpEL Key、IP/集群后缀）、SpEL
 * 解析为空时的报错、令牌获取成败两路、组注解短路语义，以及限流器配置单项漂移时的
 * setRate 更新与底层异常包装。切面持有跨调用的限流器实例静态缓存，因此各用例的承载
 * 方法均使用互不相同的缓存键。Redisson 行为以 mock 替身模拟，真实存储行为属集成测试
 * 范围。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringJUnitConfig(RateLimiterKeyResolutionTest.ContextConfig.class)
class RateLimiterKeyResolutionTest {

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

    private RateLimiterProperties properties;
    private RateLimiterExpireProperties expireProperties;
    private RedissonClient redissonClient;
    private RateLimiterAspect aspect;

    @BeforeEach
    void setUp() {
        this.properties = new RateLimiterProperties();
        this.expireProperties = new RateLimiterExpireProperties();
        this.redissonClient = mock(RedissonClient.class);
        this.aspect = new RateLimiterAspect(this.properties, this.expireProperties,
            new DefaultRateLimiterNameGenerator(), this.redissonClient);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 注解未指定名称时应由默认生成器按「压缩包名.方法名()」生成键段。
     */
    @Test
    void shouldGenerateKeyViaDefaultNameGenerator() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedWithGeneratedName");
        this.rateLimiterOf(annotation);
        this.aspect.aroundRateLimiter(this.joinPointOf("annotatedWithGeneratedName"), annotation);
        assertThat(this.capturedCacheKey())
            .isEqualTo(
                "RateLimiter:t.c.s.r.a.RateLimiterKeyResolutionTest.annotatedWithGeneratedName()");
    }

    /**
     * 注解未指定名称且注入自定义生成器时应采用自定义生成结果。
     */
    @Test
    void shouldUseCustomNameGeneratorWhenNameBlank() throws Throwable {
        RateLimiterAspect customAspect =
            new RateLimiterAspect(this.properties, this.expireProperties,
                (target, method, args) -> "custom-name", this.redissonClient);
        RateLimiter annotation = this.annotationOf("annotatedWithCustomGeneratorName");
        this.rateLimiterOf(annotation);
        customAspect.aroundRateLimiter(this.joinPointOf("annotatedWithCustomGeneratorName"),
            annotation);
        assertThat(this.capturedCacheKey()).isEqualTo("RateLimiter:custom-name");
    }

    /**
     * SpEL Key 应求值后拼入缓存键（方法参数作为变量参与求值）。
     */
    @Test
    void shouldResolveSpelKeyParameter() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedWithSpelKey", String.class);
        this.rateLimiterOf(annotation);
        this.aspect.aroundRateLimiter(this.joinPointOf("annotatedWithSpelKey", "tom"), annotation);
        assertThat(this.capturedCacheKey()).isEqualTo("RateLimiter:key-spel:tom");
    }

    /**
     * SpEL 求值结果为 null 时应显式报错「限流 Key 解析错误」，该错误再被底层兜底包装，
     * 目标方法不得执行。
     */
    @Test
    void shouldThrowWhenSpelKeyResolvesToNull() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedWithNullKey");
        this.rateLimiterOf(annotation);
        ProceedingJoinPoint joinPoint = this.joinPointOf("annotatedWithNullKey");
        assertThatThrownBy(() -> this.aspect.aroundRateLimiter(joinPoint, annotation))
            .isInstanceOf(RateLimiterException.class)
            .hasMessage("服务器限流异常，请稍候再试")
            .hasCauseInstanceOf(RateLimiterException.class)
            .hasRootCauseMessage("限流 Key 解析错误");
        verify(joinPoint, never()).proceed();
    }

    /**
     * IP 限流类型应把客户端 IP 拼作键后缀。
     */
    @Test
    void shouldAppendClientIpForIpType() throws Throwable {
        RequestContextHolder
            .setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        RateLimiter annotation = this.annotationOf("annotatedWithIpType");
        this.rateLimiterOf(annotation);
        this.aspect.aroundRateLimiter(this.joinPointOf("annotatedWithIpType"), annotation);
        assertThat(this.capturedCacheKey()).isEqualTo("RateLimiter:key-ip:127.0.0.1");
    }

    /**
     * IP 类型但缺少请求上下文时底层取 IP 失败，应包装为限流异常。
     */
    @Test
    void shouldWrapMissingRequestContextAsRateLimiterException() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedWithIpType");
        this.rateLimiterOf(annotation);
        ProceedingJoinPoint joinPoint = this.joinPointOf("annotatedWithIpType");
        assertThatThrownBy(() -> this.aspect.aroundRateLimiter(joinPoint, annotation))
            .isInstanceOf(RateLimiterException.class)
            .hasMessage("服务器限流异常，请稍候再试")
            .hasCauseInstanceOf(NullPointerException.class);
    }

    /**
     * 集群类型应把节点 ID 拼作键后缀，并以单机限流类型（PER_CLIENT）更新配置。
     */
    @Test
    void shouldAppendClusterIdAndUpdateRateTypeForClusterType() throws Throwable {
        when(this.redissonClient.getId()).thenReturn("cluster-node-1");
        RateLimiter annotation = this.annotationOf("annotatedWithClusterType");
        RRateLimiter rRateLimiter =
            this.staleLimiterOf(annotation, new RateLimiterConfig(RateType.OVERALL,
                0L, (long) annotation.rate()));
        this.aspect.aroundRateLimiter(this.joinPointOf("annotatedWithClusterType"), annotation);
        assertThat(this.capturedCacheKey()).isEqualTo("RateLimiter:key-cluster:cluster-node-1");
        verify(rRateLimiter).setRate(RateType.PER_CLIENT, Integer.MAX_VALUE, Duration.ZERO);
    }

    /**
     * 默认类型不应追加任何键后缀。
     */
    @Test
    void shouldNotAppendSuffixForDefaultType() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedResolved");
        this.rateLimiterOf(annotation);
        this.aspect.aroundRateLimiter(this.joinPointOf("annotatedResolved"), annotation);
        assertThat(this.capturedCacheKey()).isEqualTo("RateLimiter:key-resolved");
    }

    /**
     * 获取令牌失败时应抛出注解声明的提示信息，且不执行目标方法。
     */
    @Test
    void shouldThrowWithAnnotationMessageWhenTryAcquireFails() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedWithCustomMessage");
        RRateLimiter rRateLimiter = this.rateLimiterOf(annotation);
        when(rRateLimiter.tryAcquire()).thenReturn(false);
        ProceedingJoinPoint joinPoint = this.joinPointOf("annotatedWithCustomMessage");
        assertThatThrownBy(() -> this.aspect.aroundRateLimiter(joinPoint, annotation))
            .isInstanceOf(RateLimiterException.class)
            .hasMessage("自定义限流提示");
        verify(joinPoint, never()).proceed();
    }

    /**
     * 获取令牌成功时应透传目标方法返回值。
     */
    @Test
    void shouldProceedAndReturnTargetResultWhenAcquireSucceeds() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedWithProceed");
        this.rateLimiterOf(annotation);
        Object result =
            this.aspect.aroundRateLimiter(this.joinPointOf("annotatedWithProceed"), annotation);
        assertThat(result).isEqualTo("ok");
    }

    /**
     * 组注解中首个子注解命中限流时应立即终止，不再检查后续子注解。
     */
    @Test
    void shouldStopAtFirstLimitedAnnotationInGroup() throws Throwable {
        RateLimiters group = this.groupOf("annotatedGroupFirst", "annotatedGroupSecond");
        RRateLimiter firstLimiter = this.limiterOf(group.value()[0]);
        when(firstLimiter.tryAcquire()).thenReturn(false);
        RRateLimiter secondLimiter = this.limiterOf(group.value()[1]);
        // 两个子注解先后查询限流器，按调用顺序依次返回对应替身
        when(this.redissonClient.getRateLimiter(anyString()))
            .thenReturn(firstLimiter, secondLimiter);
        ProceedingJoinPoint joinPoint = this.joinPointOf("annotatedGroupFirst");
        assertThatThrownBy(() -> this.aspect.aroundRateLimiters(joinPoint, group))
            .isInstanceOf(RateLimiterException.class)
            .hasMessage("group-first limited");
        verify(secondLimiter, never()).tryAcquire();
        verify(joinPoint, never()).proceed();
    }

    /**
     * 组注解全部通过时应执行目标方法。
     */
    @Test
    void shouldProceedWhenAllGroupAnnotationsPass() throws Throwable {
        RateLimiters group = this.groupOf("annotatedGroupPassFirst", "annotatedGroupPassSecond");
        this.rateLimiterOf(group.value()[0]);
        this.rateLimiterOf(group.value()[1]);
        ProceedingJoinPoint joinPoint = this.joinPointOf("annotatedGroupPassFirst");
        Object result = this.aspect.aroundRateLimiters(joinPoint, group);
        assertThat(result).isEqualTo("ok");
    }

    /**
     * 底层 Redisson 异常应包装为限流异常并保留原因。
     */
    @Test
    void shouldWrapUnexpectedFailureAsRateLimiterException() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedWithRedisDown");
        when(this.redissonClient.getRateLimiter(anyString()))
            .thenThrow(new RuntimeException("redis down"));
        ProceedingJoinPoint joinPoint = this.joinPointOf("annotatedWithRedisDown");
        assertThatThrownBy(() -> this.aspect.aroundRateLimiter(joinPoint, annotation))
            .isInstanceOf(RateLimiterException.class)
            .hasMessage("服务器限流异常，请稍候再试")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    /**
     * 仅速率间隔漂移时应按注解值更新限流器配置。
     */
    @Test
    void shouldUpdateRateWhenOnlyIntervalDiffers() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedIntervalDiff");
        RRateLimiter rRateLimiter =
            this.staleLimiterOf(annotation, new RateLimiterConfig(RateType.OVERALL,
                600L, 10L));
        this.aspect.aroundRateLimiter(this.joinPointOf("annotatedIntervalDiff"), annotation);
        verify(rRateLimiter).setRate(RateType.OVERALL, 10, Duration.ofMillis(500));
    }

    /**
     * 仅速率漂移时应按注解值更新限流器配置。
     */
    @Test
    void shouldUpdateRateWhenOnlyRateDiffers() throws Throwable {
        RateLimiter annotation = this.annotationOf("annotatedRateDiff");
        RRateLimiter rRateLimiter =
            this.staleLimiterOf(annotation, new RateLimiterConfig(RateType.OVERALL,
                500L, 5L));
        this.aspect.aroundRateLimiter(this.joinPointOf("annotatedRateDiff"), annotation);
        verify(rRateLimiter).setRate(RateType.OVERALL, 10, Duration.ofMillis(500));
    }

    /**
     * 捕获传递给 Redisson 的缓存键。
     *
     * @return 缓存键
     */
    private String capturedCacheKey() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(this.redissonClient).getRateLimiter(captor.capture());
        return captor.getValue();
    }

    /**
     * 构造与注解配置一致的限流器替身（避免触发 setRate 更新），并绑定响应任意键查询。
     *
     * @param annotation 限流注解
     * @return 限流器替身
     */
    private RRateLimiter rateLimiterOf(RateLimiter annotation) {
        RRateLimiter rRateLimiter = this.limiterOf(annotation);
        when(this.redissonClient.getRateLimiter(anyString())).thenReturn(rRateLimiter);
        return rRateLimiter;
    }

    /**
     * 构造限流器替身本体：配置与注解一致、令牌获取成功，不绑定 Redisson 桩。
     *
     * @param annotation 限流注解
     * @return 限流器替身
     */
    private RRateLimiter limiterOf(RateLimiter annotation) {
        RRateLimiter rRateLimiter = mock(RRateLimiter.class);
        // 构造参数顺序：限流类型、速率间隔、速率
        RateLimiterConfig config = new RateLimiterConfig(RateType.OVERALL, annotation.unit()
            .toMillis(annotation.interval()), (long) annotation.rate());
        when(rRateLimiter.getConfig()).thenReturn(config);
        when(rRateLimiter.tryAcquire()).thenReturn(true);
        when(this.redissonClient.getRateLimiter(anyString())).thenReturn(rRateLimiter);
        return rRateLimiter;
    }

    /**
     * 构造持有过期配置的限流器替身，令牌获取保持成功。
     *
     * @param annotation 限流注解
     * @param staleConfig 过期的限流器配置
     * @return 限流器替身
     */
    private RRateLimiter staleLimiterOf(RateLimiter annotation, RateLimiterConfig staleConfig) {
        RRateLimiter rRateLimiter = mock(RRateLimiter.class);
        when(rRateLimiter.getConfig()).thenReturn(staleConfig);
        when(rRateLimiter.tryAcquire()).thenReturn(true);
        when(this.redissonClient.getRateLimiter(anyString())).thenReturn(rRateLimiter);
        return rRateLimiter;
    }

    /**
     * 构造组注解替身，子注解从类上的承载方法反射读取。
     *
     * @param firstMethod  首个子注解承载方法名
     * @param secondMethod 次个子注解承载方法名
     * @return 组注解替身
     */
    private RateLimiters groupOf(String firstMethod, String secondMethod)
        throws NoSuchMethodException {
        RateLimiters group = mock(RateLimiters.class);
        RateLimiter[] children = {this.annotationOf(firstMethod), this.annotationOf(secondMethod)};
        when(group.value()).thenReturn(children);
        return group;
    }

    /**
     * 构造指向本类指定方法的切点替身（参数类型按实参推导，测试参数均为引用类型）。
     *
     * @param methodName 方法名
     * @param args       方法实参
     * @return 切点替身
     */
    private ProceedingJoinPoint joinPointOf(String methodName, Object... args) throws Throwable {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        Method method =
            RateLimiterKeyResolutionTest.class.getDeclaredMethod(methodName, parameterTypes);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("ok");
        return joinPoint;
    }

    /**
     * 读取本类指定方法上的 RateLimiter 注解。
     *
     * @param name           方法名
     * @param parameterTypes 方法参数类型（无参方法不传）
     * @return 限流注解实例
     */
    private RateLimiter annotationOf(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException {
        return RateLimiterKeyResolutionTest.class.getDeclaredMethod(name, parameterTypes)
            .getAnnotation(RateLimiter.class);
    }

    @RateLimiter(name = "key-resolved")
    private void annotatedResolved() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter
    private void annotatedWithGeneratedName() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter
    private void annotatedWithCustomGeneratorName() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-spel", key = "#param")
    private void annotatedWithSpelKey(String param) {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-null", key = "#missingVariable")
    private void annotatedWithNullKey() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-ip", type = LimitType.IP)
    private void annotatedWithIpType() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-cluster", type = LimitType.CLUSTER)
    private void annotatedWithClusterType() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-custom-message", message = "自定义限流提示")
    private void annotatedWithCustomMessage() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-proceed")
    private void annotatedWithProceed() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-redis-down")
    private void annotatedWithRedisDown() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-interval-diff", rate = 10, interval = 500, expire = 0)
    private void annotatedIntervalDiff() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "key-rate-diff", rate = 10, interval = 500, expire = 0)
    private void annotatedRateDiff() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "group-first", message = "group-first limited")
    private void annotatedGroupFirst() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "group-second")
    private void annotatedGroupSecond() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "group-pass-first")
    private void annotatedGroupPassFirst() {
        // 仅承载注解，供反射读取注解实例
    }

    @RateLimiter(name = "group-pass-second")
    private void annotatedGroupPassSecond() {
        // 仅承载注解，供反射读取注解实例
    }
}
