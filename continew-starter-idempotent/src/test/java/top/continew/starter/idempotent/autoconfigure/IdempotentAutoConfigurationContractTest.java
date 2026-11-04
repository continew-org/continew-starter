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

package top.continew.starter.idempotent.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.idempotent.aop.IdempotentAspect;
import top.continew.starter.idempotent.generator.IdempotentNameGenerator;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 幂等自动配置装配契约测试。
 *
 * <p>切面构造仅依赖配置属性与名称生成器，Redis 读写发生在注解拦截期经 RedisUtils
 * 静态工具进行，因此装配态不依赖 RedissonClient Bean（区别于限流模块的构造器注入）。
 * 名称生成器按 {@code @ConditionalOnMissingBean} 约定允许使用方整体覆盖。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class IdempotentAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration.class));

    /**
     * imports 资源必须登记本模块包内全部自动配置类，防止新增配置类漏注册后静默失效。
     */
    @Test
    void importsShouldBeComplete() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.idempotent")).isEmpty();
    }

    /**
     * 开关缺省视为开启，无需 Redis 前置即可装配幂等切面。
     */
    @Test
    void shouldRegisterAspectByDefault() {
        this.contextRunner.run(context -> assertThat(context)
            .hasSingleBean(IdempotentAspect.class));
    }

    /**
     * 显式关闭开关后整体跳过，不影响应用启动。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.idempotent.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(IdempotentAspect.class);
            });
    }

    /**
     * 使用方注册名称生成器后默认实现退避，容器保留用户实例。
     */
    @Test
    void shouldUseCustomNameGeneratorWhenPresent() {
        IdempotentNameGenerator customGenerator = mock(IdempotentNameGenerator.class);
        this.contextRunner.withBean(IdempotentNameGenerator.class, () -> customGenerator)
            .run(context -> assertThat(context.getBean(IdempotentNameGenerator.class))
                .isSameAs(customGenerator));
    }

    /**
     * Key 前缀默认 Idempotent，配置覆盖后生效。
     */
    @Test
    void shouldBindKeyPrefixProperty() {
        this.contextRunner.run(context -> assertThat(context.getBean(IdempotentProperties.class)
            .getKeyPrefix()).isEqualTo("Idempotent"));
        this.contextRunner.withPropertyValues("continew-starter.idempotent.key-prefix=Idem")
            .run(context -> assertThat(context.getBean(IdempotentProperties.class)
                .getKeyPrefix()).isEqualTo("Idem"));
    }
}
