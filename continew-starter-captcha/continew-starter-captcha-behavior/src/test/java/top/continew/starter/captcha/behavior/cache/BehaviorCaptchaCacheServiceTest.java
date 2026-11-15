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

package top.continew.starter.captcha.behavior.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.continew.starter.cache.redisson.util.RedisUtils;

import java.time.Duration;

/**
 * {@link BehaviorCaptchaCacheService} 行为验证码缓存单元测试：锁定对 RedisUtils 的委托语义。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class BehaviorCaptchaCacheServiceTest {

    private final BehaviorCaptchaCacheService service = new BehaviorCaptchaCacheService();

    /**
     * RedisUtils 的静态字段在类加载时即定位 RedissonClient，先建最小上下文供其初始化。
     */
    @BeforeAll
    static void setUpSpringContext() {
        if (SpringUtil.getApplicationContext() != null) {
            return;
        }
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(SpringUtil.class);
        context.registerBean(RedissonClient.class, () -> mock(RedissonClient.class));
        context.refresh();
    }

    /**
     * type 应返回 redis（StorageType.REDIS 小写）。
     */
    @Test
    @DisplayName("type 返回 redis")
    void shouldReturnRedisType() {
        assertThat(service.type()).isEqualTo("redis");
    }

    /**
     * set 数值字符串应按 Integer 写入（aj-captcha 数值点位按 int 存取），带过期时间。
     */
    @Test
    @DisplayName("set 数值走 Integer 写入")
    void shouldSetNumericValueAsInteger() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            service.set("captcha:key", "123456", 300);

            redisUtils.verify(() -> RedisUtils.set(eq("captcha:key"), eq(123456), eq(Duration
                .ofSeconds(300))));
        }
    }

    /**
     * set 非数值字符串应按原字符串写入。
     */
    @Test
    @DisplayName("set 非数值走字符串写入")
    void shouldSetNonNumericValueAsString() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            service.set("captcha:key", "point:100,200", 60);

            redisUtils
                .verify(() -> RedisUtils.set(eq("captcha:key"), eq("point:100,200"), eq(Duration
                    .ofSeconds(60))));
        }
    }

    /**
     * get 应把 Redis 值转为字符串返回；null 值返回 null。
     */
    @Test
    @DisplayName("get 转字符串返回")
    void shouldGetStringValue() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.get("captcha:key")).thenReturn(123456);

            assertThat(service.get("captcha:key")).isEqualTo("123456");

            redisUtils.when(() -> RedisUtils.get("empty:key")).thenReturn(null);

            assertThat(service.get("empty:key")).isNull();
        }
    }

    /**
     * exists/delete 应透传 RedisUtils 同名操作。
     */
    @Test
    @DisplayName("exists/delete 透传")
    void shouldDelegateExistsAndDelete() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.exists("captcha:key")).thenReturn(true);

            assertThat(service.exists("captcha:key")).isTrue();

            service.delete("captcha:key");

            redisUtils.verify(() -> RedisUtils.delete("captcha:key"));
        }
    }

    /**
     * increment 应透传 RedisUtils.incr（忽略入参 val，锁定现状）。
     */
    @Test
    @DisplayName("increment 透传 incr")
    void shouldDelegateIncrement() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.incr("fail:count")).thenReturn(3L);

            assertThat(service.increment("fail:count", 5)).isEqualTo(3L);
        }
    }
}
