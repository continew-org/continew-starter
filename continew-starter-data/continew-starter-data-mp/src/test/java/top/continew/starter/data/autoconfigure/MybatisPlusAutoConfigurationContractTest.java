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

package top.continew.starter.data.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * MyBatis Plus 自动配置契约测试。
 *
 * <p>固化装配契约：开关（mybatis-plus.extension.enabled）默认关闭须显式开启、分页插件按
 * 配置装配、拦截器链的默认组成、用户拦截器 fallback、代表性配置属性绑定。hutool SpringUtil
 * 以 Bean 注册（拦截器装配期从容器收集 InnerInterceptor），本模块类路径无业务插件，不额外
 * 提供 mock。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MybatisPlusAutoConfigurationContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class))
        .withBean(SpringUtil.class, SpringUtil::new);

    /**
     * imports 注册完整性：包内全部自动配置类均已登记进 AutoConfiguration.imports。
     */
    @Test
    void shouldRegisterAllAutoConfigurationClasses() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered("top.continew.starter.data"))
            .isEmpty();
    }

    /**
     * 开关默认关闭（havingValue 无 matchIfMissing）：未显式开启时静默跳过，不注册任何 Bean。
     */
    @Test
    void shouldSkipSilentlyByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(MybatisPlusPropertiesCustomizer.class);
            assertThat(context).doesNotHaveBean(MybatisPlusInterceptor.class);
        });
    }

    /**
     * 显式开启后完整装配：配置定制器与拦截器链（默认仅防全表更新与删除插件）。
     */
    @Test
    void shouldRegisterBeansWhenEnabled() {
        this.contextRunner.withPropertyValues("mybatis-plus.extension.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(MybatisPlusPropertiesCustomizer.class);
                assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(BlockAttackInnerInterceptor.class);
            });
    }

    /**
     * 分页插件按配置装配：开启分页并指定数据库类型后，插件链应包含分页拦截器。
     */
    @Test
    void shouldRegisterPaginationInterceptorWhenConfigured() {
        this.contextRunner.withPropertyValues("mybatis-plus.extension.enabled=true",
            "mybatis-plus.extension.pagination.enabled=true",
            "mybatis-plus.extension.pagination.db-type=mysql")
            .run(context -> {
                assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors())
                    .hasSize(2)
                    .first()
                    .isInstanceOf(PaginationInnerInterceptor.class);
            });
    }

    /**
     * 用户已提供拦截器时不再装配内置插件链，容器保留用户实例（同一性）。
     */
    @Test
    void shouldKeepUserInterceptorWhenPresent() {
        MybatisPlusInterceptor userInterceptor = new MybatisPlusInterceptor();
        this.contextRunner.withPropertyValues("mybatis-plus.extension.enabled=true")
            .withBean(MybatisPlusInterceptor.class, () -> userInterceptor)
            .run(context -> assertThat(context.getBean(MybatisPlusInterceptor.class))
                .isSameAs(userInterceptor));
    }

    /**
     * 属性绑定：防全表更新与删除默认开启、乐观锁默认关闭，覆盖值生效。
     */
    @Test
    void shouldBindExtensionProperties() {
        this.contextRunner.withPropertyValues("mybatis-plus.extension.enabled=true")
            .run(context -> {
                MyBatisPlusExtensionProperties properties = context
                    .getBean(MyBatisPlusExtensionProperties.class);
                assertThat(properties.isBlockAttackPluginEnabled()).isTrue();
                assertThat(properties.isOptimisticLockerEnabled()).isFalse();
            });
        this.contextRunner
            .withPropertyValues("mybatis-plus.extension.enabled=true",
                "mybatis-plus.extension.optimistic-locker-enabled=true")
            .run(context -> assertThat(context.getBean(MyBatisPlusExtensionProperties.class)
                .isOptimisticLockerEnabled()).isTrue());
    }
}
