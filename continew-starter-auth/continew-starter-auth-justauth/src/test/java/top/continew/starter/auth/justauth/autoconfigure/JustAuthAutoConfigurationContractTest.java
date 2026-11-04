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

package top.continew.starter.auth.justauth.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import me.zhyd.oauth.cache.AuthDefaultStateCache;
import me.zhyd.oauth.cache.AuthStateCache;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.auth.justauth.AuthRequestFactory;
import top.continew.starter.auth.justauth.cache.RedisAuthStateCache;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * JustAuth 自动配置装配契约测试。
 *
 * <p>固化 State 缓存的三种策略契约：缺省使用进程内存（{@code AuthDefaultStateCache}）；
 * 使用方注册 {@code AuthStateCache} 后全部内存/Redis 策略退避；{@code cache.type=custom}
 * 却不提供用户实现属于配置错误，必须快速失败并指向缺失类型，而不是静默回退内存
 * （OAuth State 一次性语义被静默替换为跨实例共享失效会放大 CSRF 风险面）。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class JustAuthAutoConfigurationContractTest {

    private static final String PREFIX = "continew-starter.justauth";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JustAuthAutoConfiguration.class));

    /**
     * imports 资源必须登记本模块包内全部自动配置类，防止新增配置类漏注册后静默失效。
     */
    @Test
    void importsShouldBeComplete() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.auth.justauth")).isEmpty();
    }

    /**
     * 开关缺省视为开启，State 缓存默认走进程内存实现。
     */
    @Test
    void shouldRegisterFactoryWithDefaultStateCache() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuthRequestFactory.class);
            assertThat(context.getBean(AuthStateCache.class))
                .isSameAs(AuthDefaultStateCache.INSTANCE);
        });
    }

    /**
     * 显式关闭开关后不装配工厂且不影响应用启动。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.contextRunner.withPropertyValues(PREFIX + ".enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(AuthRequestFactory.class);
            });
    }

    /**
     * 使用方注册 AuthStateCache 后内存与 Redis 策略均退避，容器保留用户实例。
     */
    @Test
    void shouldUseCustomStateCacheWhenPresent() {
        AuthStateCache customStateCache = mock(AuthStateCache.class);
        this.contextRunner.withBean(AuthStateCache.class, () -> customStateCache)
            .run(context -> assertThat(context.getBean(AuthStateCache.class))
                .isSameAs(customStateCache));
    }

    /**
     * cache.type=redis 且 RedissonClient 就位时切换为 Redis 实现。
     */
    @Test
    void shouldUseRedisStateCacheWhenRedisTypeWithRedissonClient() {
        this.contextRunner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .withPropertyValues(PREFIX + ".cache.type=redis")
            .run(context -> assertThat(context.getBean(AuthStateCache.class))
                .isInstanceOf(RedisAuthStateCache.class));
    }

    /**
     * cache.type=custom 却不提供用户实现时启动快速失败，指向缺失的 AuthStateCache。
     */
    @Test
    void shouldFailFastWhenCustomStateCacheMissing() {
        this.contextRunner.withPropertyValues(PREFIX + ".cache.type=custom")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
            });
    }

    /**
     * State 缓存 Key 前缀默认值与覆盖值均按配置绑定生效。
     *
     * <p>嵌套属性对象在未绑定任何 {@code cache.*} 配置项时保持 null，先用 timeout
     * 触发实例化，再断言 prefix 的类内默认值。</p>
     */
    @Test
    void shouldBindCachePrefixProperty() {
        this.contextRunner.withPropertyValues(PREFIX + ".cache.timeout=5s")
            .run(context -> assertThat(context.getBean(JustAuthProperties.class).getCache()
                .getPrefix()).isEqualTo("CONTINEW-STARTER::JUSTAUTH::STATE::"));
        this.contextRunner.withPropertyValues(PREFIX + ".cache.prefix=test::")
            .run(context -> assertThat(context.getBean(JustAuthProperties.class).getCache()
                .getPrefix()).isEqualTo("test::"));
    }
}
