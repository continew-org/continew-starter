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

package top.continew.starter.auth.justauth.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.continew.starter.auth.justauth.autoconfigure.cache.JustAuthStateCacheProperties;
import top.continew.starter.cache.redisson.util.RedisUtils;

import java.time.Duration;

/**
 * {@link RedisAuthStateCache} JustAuth State 缓存单元测试：锁定对 RedisUtils 的委托语义。
 *
 * <p>RedisUtils 静态初始化经 SpringUtil 定位 RedissonClient，测试侧用最小上下文满足该约定。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RedisAuthStateCacheTest {

    private RedisAuthStateCache cache;
    private JustAuthStateCacheProperties properties;

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

    @BeforeEach
    void setUp() {
        properties = new JustAuthStateCacheProperties();
        cache = new RedisAuthStateCache(properties);
    }

    /**
     * 构建已注册 formatKey 真实行为的静态 mock（formatKey 也属 RedisUtils 静态方法，
     * 不 stub 会导致被测代码拿到 null 键）。
     *
     * @return 静态 mock
     */
    private MockedStatic<RedisUtils> mockRedisUtils() {
        MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class);
        redisUtils
            .when(() -> RedisUtils.formatKey(org.mockito.ArgumentMatchers.any(String[].class)))
            .thenAnswer(invocation -> {
                Object[] args = invocation.getArguments();
                return String.join(":", java.util.Arrays.stream(args)
                    .map(String::valueOf)
                    .toArray(String[]::new));
            });
        return redisUtils;
    }

    /**
     * 双参 cache 应按默认超时写入（键带全局前缀）。
     */
    @Test
    @DisplayName("cache 双参按默认超时写入")
    void shouldCacheWithDefaultTimeout() {
        try (MockedStatic<RedisUtils> redisUtils = mockRedisUtils()) {
            cache.cache("state-key", "state-value");

            String expectedKey = String.join(":", properties.getPrefix(), "state-key");
            redisUtils.verify(() -> RedisUtils.set(eq(expectedKey), eq("state-value"), eq(properties
                .getTimeout())));
        }
    }

    /**
     * 三参 cache 应按指定超时写入。
     */
    @Test
    @DisplayName("cache 三参按指定超时写入")
    void shouldCacheWithCustomTimeout() {
        try (MockedStatic<RedisUtils> redisUtils = mockRedisUtils()) {
            cache.cache("state-key", "state-value", 60000);

            String expectedKey = String.join(":", properties.getPrefix(), "state-key");
            redisUtils.verify(() -> RedisUtils.set(eq(expectedKey), eq("state-value"), eq(Duration
                .ofMillis(60000))));
        }
    }

    /**
     * get 应透传带前缀的键读取。
     */
    @Test
    @DisplayName("get 透传读取")
    void shouldGetWithPrefix() {
        try (MockedStatic<RedisUtils> redisUtils = mockRedisUtils()) {
            String expectedKey = String.join(":", properties.getPrefix(), "state-key");
            redisUtils.when(() -> RedisUtils.get(expectedKey)).thenReturn("state-value");

            assertThat(cache.get("state-key")).isEqualTo("state-value");
        }
    }

    /**
     * containsKey 应透传带前缀的键存在性判断。
     */
    @Test
    @DisplayName("containsKey 透传判断")
    void shouldDelegateContainsKey() {
        try (MockedStatic<RedisUtils> redisUtils = mockRedisUtils()) {
            String expectedKey = String.join(":", properties.getPrefix(), "state-key");
            redisUtils.when(() -> RedisUtils.exists(expectedKey)).thenReturn(true);

            assertThat(cache.containsKey("state-key")).isTrue();

            String missingKey = String.join(":", properties.getPrefix(), "missing");
            redisUtils.when(() -> RedisUtils.exists(missingKey)).thenReturn(false);

            assertThat(cache.containsKey("missing")).isFalse();
        }
    }
}
