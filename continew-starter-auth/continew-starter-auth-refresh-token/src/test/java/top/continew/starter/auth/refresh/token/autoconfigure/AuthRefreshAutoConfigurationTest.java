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

package top.continew.starter.auth.refresh.token.autoconfigure;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.auth.refresh.token.service.RefreshTokenService;

/**
 * Refresh Token 自动配置装配测试。
 *
 * <p>本模块以 {@code @ConditionalOnBean(RedissonClient.class)} 为装配前提：Redis 是
 * Refresh Session 存储与轮换快照的硬性依赖，缺失时必须整体跳过（而非启动失败）。
 * 该前提依赖 {@code after = RedissonAutoConfiguration} 的排序保证，本测试用于固化
 * 这一装配契约，避免调整自动配置顺序后出现「无异常但全部 Bean 未注册」的静默失效。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
class AuthRefreshAutoConfigurationTest {

    private static final String SECRET = "test-only-refresh-token-secret-with-32-bytes";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AuthRefreshAutoConfiguration.class))
        .withPropertyValues("continew-starter.refresh-token.secret=" + SECRET);

    /**
     * 存在 RedissonClient 时应完整装配全部 Bean。
     */
    @Test
    void shouldRegisterBeansWhenRedissonClientPresent() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        when(redissonClient.getTopic(anyString())).thenReturn(topic);
        this.contextRunner.withBean(RedissonClient.class, () -> redissonClient)
            .run(context -> context.assertThat().hasSingleBean(RefreshTokenService.class));
    }

    /**
     * 缺少 RedissonClient 时应静默跳过，不得影响应用启动。
     */
    @Test
    void shouldSkipSilentlyWhenRedissonClientAbsent() {
        this.contextRunner.run(context -> {
            context.assertThat().hasNotFailed();
            context.assertThat().doesNotHaveBean(RefreshTokenService.class);
        });
    }
}
