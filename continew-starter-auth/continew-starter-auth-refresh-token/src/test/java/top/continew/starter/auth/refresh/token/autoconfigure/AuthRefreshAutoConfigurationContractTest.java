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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.auth.refresh.token.service.RefreshTokenService;
import top.continew.starter.auth.refresh.token.service.SessionInvalidationService;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * Refresh Token 自动配置装配契约测试。
 *
 * <p>固化三条装配契约：{@code RedissonClient} 是装配前提，缺失时必须静默跳过而非启动
 * 失败；{@code enabled} 开关缺省视为开启；扩展点按 {@code @ConditionalOnMissingBean}
 * 约定允许使用方整体覆盖。装配态必须提供不少于 32 字符的 secret，否则属性校验会在
 * 启动期拦截。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AuthRefreshAutoConfigurationContractTest {

    private static final String SECRET = "contract-test-refresh-token-secret-32-bytes";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AuthRefreshAutoConfiguration.class));

    /**
     * 会话缓存默认开启，装配态的 AccessSessionCache 初始化会订阅失效广播 Topic，
     * RedissonClient 桩需返回可用的 RTopic。
     *
     * @return 已完成 getTopic 打桩的 RedissonClient 测试桩
     */
    private RedissonClient mockedRedissonClient() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        // mock 创建提取到局部变量，避免其抛出的异常被错误归因（Sonar S9016）
        RTopic invalidationTopic = mock(RTopic.class);
        when(redissonClient.getTopic(anyString())).thenReturn(invalidationTopic);
        return redissonClient;
    }

    /**
     * imports 资源必须登记本模块包内全部自动配置类，防止新增配置类漏注册后静默失效。
     */
    @Test
    void importsShouldBeComplete() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.auth.refresh.token")).isEmpty();
    }

    /**
     * 缺少 RedissonClient 前置时应静默跳过，不影响应用启动。
     */
    @Test
    void shouldSkipSilentlyWhenRedissonClientAbsent() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RefreshTokenService.class);
        });
    }

    /**
     * enabled 开关缺省视为开启，RedissonClient 就位时装配会话服务与失效入口。
     */
    @Test
    void shouldRegisterBeansWhenEnabledByDefault() {
        this.contextRunner.withBean(RedissonClient.class, this::mockedRedissonClient)
            .withPropertyValues("continew-starter.refresh-token.secret=" + SECRET)
            .run(context -> {
                assertThat(context).hasSingleBean(RefreshTokenService.class);
                assertThat(context).hasSingleBean(SessionInvalidationService.class);
            });
    }

    /**
     * 显式关闭开关后即使 RedissonClient 就位也不得装配任何会话相关 Bean。
     */
    @Test
    void shouldSkipWhenDisabled() {
        // mock 创建提取到局部变量，避免其抛出的异常被归因到 withBean 调用（Sonar S9016）
        RedissonClient disabledRedissonClient = mock(RedissonClient.class);
        this.contextRunner.withBean(RedissonClient.class, () -> disabledRedissonClient)
            .withPropertyValues("continew-starter.refresh-token.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(RefreshTokenService.class);
            });
    }

    /**
     * 使用方注册 RefreshTokenCodec 后默认实现退避，容器保留用户实例。
     */
    @Test
    void shouldUseCustomCodecWhenPresent() {
        RefreshTokenCodec customCodec = mock(RefreshTokenCodec.class);
        this.contextRunner.withBean(RedissonClient.class, this::mockedRedissonClient)
            .withBean(RefreshTokenCodec.class, () -> customCodec)
            .withPropertyValues("continew-starter.refresh-token.secret=" + SECRET)
            .run(context -> assertThat(context.getBean(RefreshTokenCodec.class))
                .isSameAs(customCodec));
    }

    /**
     * Cookie 名称默认 refresh_token，配置覆盖后生效。
     */
    @Test
    void shouldBindCookieNameProperty() {
        this.contextRunner.withBean(RedissonClient.class, this::mockedRedissonClient)
            .withPropertyValues("continew-starter.refresh-token.secret=" + SECRET)
            .run(context -> assertThat(context.getBean(RefreshTokenProperties.class)
                .getCookieName()).isEqualTo("refresh_token"));
        this.contextRunner.withBean(RedissonClient.class, this::mockedRedissonClient)
            .withPropertyValues("continew-starter.refresh-token.secret=" + SECRET,
                "continew-starter.refresh-token.cookie-name=custom_rt")
            .run(context -> assertThat(context.getBean(RefreshTokenProperties.class)
                .getCookieName()).isEqualTo("custom_rt"));
    }
}
