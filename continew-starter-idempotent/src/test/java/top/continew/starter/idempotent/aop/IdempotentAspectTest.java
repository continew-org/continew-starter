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

package top.continew.starter.idempotent.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.api.RedissonClient;
import org.springframework.context.support.StaticApplicationContext;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.idempotent.annotation.Idempotent;
import top.continew.starter.idempotent.autoconfigure.IdempotentProperties;
import top.continew.starter.idempotent.exception.IdempotentException;
import top.continew.starter.idempotent.generator.IdempotentNameGenerator;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link IdempotentAspect} 幂等切面测试。
 *
 * <p>以 Mockito 构造切点上下文驱动 around/getCacheKey 全部分支；RedisUtils 静态读写
 * 用 mockStatic 替身，formatKey 保留真实实现以锁定 Key 拼装格式，全程不依赖 Redis。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class IdempotentAspectTest {

    private final IdempotentProperties properties = new IdempotentProperties();
    private final IdempotentNameGenerator generator = mock(IdempotentNameGenerator.class);
    private final IdempotentAspect aspect = new IdempotentAspect(properties, generator);

    @BeforeAll
    static void initSpringContextForRedisUtils() {
        // RedisUtils 类加载即向容器索取 RedissonClient，mockStatic 触发类初始化需先备好空容器
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("redissonClient", mock(RedissonClient.class));
        new SpringUtil().setApplicationContext(context);
    }

    /**
     * 样例服务：各方法携带不同注解配置，方法体不真正执行（proceed 被替身接管）。
     */
    static class OrderService {

        @Idempotent
        public String create(String orderNo) {
            return "created:" + orderNo;
        }

        @Idempotent(name = "order:create", timeout = 2, unit = TimeUnit.SECONDS, message = "重复提交")
        public String namedCreate(String orderNo) {
            return "created:" + orderNo;
        }

        @Idempotent(key = "#orderNo")
        public String keyedCreate(String orderNo) {
            return "created:" + orderNo;
        }

        @Idempotent(key = "#orderNo", name = "order:keyed")
        public String keyedNamedCreate(String orderNo) {
            return "created:" + orderNo;
        }
    }

    private ProceedingJoinPoint joinPoint(String methodName, String... args)
        throws NoSuchMethodException {
        Method method = OrderService.class.getMethod(methodName, String.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new OrderService());
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private Idempotent annotation(String methodName) throws NoSuchMethodException {
        return OrderService.class.getMethod(methodName, String.class)
            .getAnnotation(Idempotent.class);
    }

    @Test
    void shouldProceedWhenKeyAbsent() throws Throwable {
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenReturn(true);
            ProceedingJoinPoint joinPoint = joinPoint("create", "A123");
            when(joinPoint.proceed()).thenReturn("created:A123");
            Object result = aspect.around(joinPoint, annotation("create"));
            assertThat(result).isEqualTo("created:A123");
            // 命中首次调用时不允许回删 Key，否则幂等窗口立即失效
            redisUtils.verify(() -> RedisUtils.delete(anyString()), never());
        }
    }

    @Test
    void shouldUseDefaultTimeoutOfOneSecond() throws Throwable {
        AtomicReference<Duration> captured = new AtomicReference<>();
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenAnswer(inv -> {
                    captured.set(inv.getArgument(2));
                    return true;
                });
            aspect.around(joinPoint("create", "A123"), annotation("create"));
            assertThat(captured.get()).isEqualTo(Duration.ofMillis(1000));
        }
    }

    @Test
    void shouldConvertTimeoutWithConfiguredUnit() throws Throwable {
        AtomicReference<Duration> captured = new AtomicReference<>();
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenAnswer(inv -> {
                    captured.set(inv.getArgument(2));
                    return true;
                });
            aspect.around(joinPoint("namedCreate", "A123"), annotation("namedCreate"));
            assertThat(captured.get()).isEqualTo(Duration.ofSeconds(2));
        }
    }

    @Test
    void shouldThrowIdempotentExceptionWhenKeyPresent() throws Throwable {
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenReturn(false);
            ProceedingJoinPoint joinPoint = joinPoint("namedCreate", "A123");
            assertThatThrownBy(() -> aspect.around(joinPoint, annotation("namedCreate")))
                .isInstanceOf(IdempotentException.class)
                .hasMessage("重复提交");
            verify(joinPoint, never()).proceed();
        }
    }

    @Test
    void shouldDeleteKeyWhenTargetThrows() throws Throwable {
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenReturn(true);
            // CALLS_REAL_METHODS 下未 stub 的 delete 会打到 mock RedissonClient，需显式接管
            redisUtils.when(() -> RedisUtils.delete(anyString())).thenReturn(true);
            ProceedingJoinPoint joinPoint = joinPoint("create", "A123");
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("业务异常"));
            assertThatThrownBy(() -> aspect.around(joinPoint, annotation("create")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("业务异常");
            // 目标方法失败必须回删 Key，否则失败结果会被幂等窗口挡住无法重试
            redisUtils.verify(() -> RedisUtils.delete(anyString()));
        }
    }

    @Test
    void shouldUseNameGeneratorWhenNameBlank() throws Throwable {
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenReturn(true);
            when(generator.generate(any(), any(), any())).thenReturn("gen:name");
            aspect.around(joinPoint("create", "A123"), annotation("create"));
            redisUtils
                .verify(() -> RedisUtils.setIfAbsent(eqKey("Idempotent:gen:name"), any(), any()));
        }
    }

    @Test
    void shouldSkipNameGeneratorWhenNamePresent() throws Throwable {
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenReturn(true);
            aspect.around(joinPoint("namedCreate", "A123"), annotation("namedCreate"));
            redisUtils.verify(() -> RedisUtils
                .setIfAbsent(eqKey("Idempotent:order:create"), any(), any()));
            verify(generator, never()).generate(any(), any(), any());
        }
    }

    @Test
    void shouldResolveSpelKey() throws Throwable {
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            redisUtils.when(() -> RedisUtils.setIfAbsent(anyString(), any(), any()))
                .thenReturn(true);
            aspect.around(joinPoint("keyedNamedCreate", "A123"), annotation("keyedNamedCreate"));
            redisUtils.verify(() -> RedisUtils
                .setIfAbsent(eqKey("Idempotent:order:keyed:A123"), any(), any()));
        }
    }

    @Test
    void shouldThrowWhenSpelKeyResolvesNull() throws Throwable {
        try (MockedStatic<RedisUtils> redisUtils =
            mockStatic(RedisUtils.class, CALLS_REAL_METHODS)) {
            ProceedingJoinPoint joinPoint = joinPoint("keyedCreate", (String) null);
            assertThatThrownBy(() -> aspect.around(joinPoint, annotation("keyedCreate")))
                .isInstanceOf(IdempotentException.class)
                .hasMessage("幂等 Key 解析错误");
            redisUtils.verify(() -> RedisUtils.setIfAbsent(anyString(), any(), any()), never());
        }
    }

    /**
     * 构造按字面量比对缓存 Key 的参数匹配器，增强 verify 可读性。
     *
     * @param key 期望的缓存 Key
     * @return 匹配器描述的参数值
     */
    private static String eqKey(String key) {
        return argThat(actual -> key.equals(actual));
    }
}
