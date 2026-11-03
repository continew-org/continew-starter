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

package top.continew.starter.auth.satoken.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.interceptor.SaInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * SaToken 自动配置装配契约测试。
 *
 * <p>固化两条与其他 Starter 相反的契约：{@code sa-token.extension.enabled} 无
 * {@code matchIfMissing}，缺省视为关闭，使用方必须显式开启；{@code dao.type=redis}
 * 且缺少 {@code RedissonClient} 时三种持久层策略均不满足，容器静默缺失
 * {@code SaTokenDao}（无兜底也不报错），使用方须保证 redis 类型与 Redis 前置同时成立。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SaTokenAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SaTokenAutoConfiguration.class));

    /**
     * imports 资源必须登记本模块包内全部自动配置类，防止新增配置类漏注册后静默失效。
     */
    @Test
    void importsShouldBeComplete() {
        assertThat(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.auth.satoken")).isEmpty();
    }

    /**
     * 开关缺省视为关闭，不得装配任何 Bean 且不影响应用启动。
     */
    @Test
    void shouldSkipByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SaInterceptor.class);
        });
    }

    /**
     * 显式开启后装配默认登录拦截器。
     */
    @Test
    void shouldRegisterInterceptorWhenEnabled() {
        this.contextRunner.withPropertyValues("sa-token.extension.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(SaInterceptor.class));
    }

    /**
     * dao.type=redis 但缺少 RedissonClient 时持久层整体缺失且不报错。
     */
    @Test
    void shouldLeaveDaoMissingWhenRedisTypeWithoutRedissonClient() {
        this.contextRunner.withPropertyValues("sa-token.extension.enabled=true",
            "sa-token.extension.dao.type=redis")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(SaTokenDao.class);
            });
    }

    /**
     * 使用方注册 SaInterceptor 后默认拦截器退避，容器保留用户实例。
     */
    @Test
    void shouldUseCustomInterceptorWhenPresent() {
        SaInterceptor customInterceptor = new SaInterceptor(handle -> {
        });
        this.contextRunner.withPropertyValues("sa-token.extension.enabled=true")
            .withBean(SaInterceptor.class, () -> customInterceptor)
            .run(context -> assertThat(context.getBean(SaInterceptor.class))
                .isSameAs(customInterceptor));
    }

    /**
     * jwt-enabled 默认关闭，显式开启后属性绑定生效。
     */
    @Test
    void shouldBindJwtEnabledProperty() {
        this.contextRunner.withPropertyValues("sa-token.extension.enabled=true")
            .run(context -> assertThat(context.getBean(SaTokenExtensionProperties.class)
                .isJwtEnabled()).isFalse());
        this.contextRunner.withPropertyValues("sa-token.extension.enabled=true",
            "sa-token.extension.jwt-enabled=true")
            .run(context -> assertThat(context.getBean(SaTokenExtensionProperties.class)
                .isJwtEnabled()).isTrue());
    }
}
