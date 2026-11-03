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

package top.continew.starter.captcha.behavior.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.CaptchaService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 行为验证码自动配置契约测试
 *
 * <p>固化 BehaviorCaptchaAutoConfiguration 的装配契约：imports 登记完整性、开关默认
 * 启用、CaptchaService 用户回退、缓存分支选择与配置属性绑定。验证码服务依赖缓存的
 * {@code captchaCacheService} Bean（{@code @DependsOn}），各用例按缓存分支默认装配
 * 路径供给。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class BehaviorCaptchaAutoConfigurationContractTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(BehaviorCaptchaAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.captcha.behavior").isEmpty());
    }

    /**
     * 开关默认启用（matchIfMissing），验证码服务与默认内存缓存服务均应注册。
     */
    @Test
    void shouldRegisterCaptchaServiceByDefault() {
        this.runner.run(context -> {
            assertThat(context).hasSingleBean(CaptchaService.class);
            assertThat(context).hasSingleBean(CaptchaCacheService.class);
        });
    }

    /**
     * 开关关闭时应整体跳过（含 @Import 的缓存配置），不产生任何副作用 Bean。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.runner.withPropertyValues("continew-starter.captcha.behavior.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(CaptchaService.class);
                assertThat(context).doesNotHaveBean(CaptchaCacheService.class);
            });
    }

    /**
     * 用户注册 CaptchaService 时应回退到用户实现，默认服务不再创建。
     */
    @Test
    void shouldUseUserCaptchaServiceWhenProvided() {
        CaptchaService userService = mock(CaptchaService.class);
        this.runner.withBean(CaptchaService.class, () -> userService).run(context -> {
            assertThat(context).hasSingleBean(CaptchaService.class);
            assertSame(userService, context.getBean(CaptchaService.class));
        });
    }

    /**
     * 缓存类型为 redis 且存在 RedissonClient 时，缓存服务应切换到 Redis 分支。
     *
     * <p>Redis 分支以 @ConditionalOnBean(RedissonClient) 为前提，用于固化可选依赖的
     * 分支切换契约：无 Redis 依赖时仍走默认内存分支，引入 Redisson 后整体切换。</p>
     */
    @Test
    void shouldRegisterRedisCacheServiceWhenRedissonClientPresent() {
        this.runner.withPropertyValues("continew-starter.captcha.behavior.cache-type=redis")
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .run(context -> assertThat(context).hasSingleBean(CaptchaCacheService.class));
    }

    /**
     * 滑动偏移量属性应支持默认值与覆盖值绑定。
     */
    @Test
    void shouldBindSlipOffsetProperty() {
        this.runner.run(context -> assertThat(context.getBean(BehaviorCaptchaProperties.class)
            .getSlipOffset()).isEqualTo("5"));
        this.runner.withPropertyValues("continew-starter.captcha.behavior.slip-offset=8")
            .run(context -> assertThat(context.getBean(BehaviorCaptchaProperties.class)
                .getSlipOffset()).isEqualTo("8"));
    }
}
