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

package top.continew.starter.license.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.license.service.LicenseCreateService;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * License 生成自动配置装配契约测试
 *
 * <p>开关为 {@code continew-starter.license.generator.enabled}（缺省视为启用），装配产物为
 * {@code LicenseCreateService} 单例，并允许使用方通过 {@code @ConditionalOnMissingBean}
 * 整体替换。配置属性仅有开关本身（其语义由开关用例覆盖），无其他绑定项，故不设独立属性
 * 绑定用例。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseGenerateAutoConfigurationContractTest {

    // 自动配置类位于跨生成/校验共享的 autoconfigure 包，扫描基础包取模块根包
    private static final String BASE_PACKAGE = "top.continew.starter.license";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LicenseGenerateAutoConfiguration.class));

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 默认（未声明 enabled）应装配生成服务。
     */
    @Test
    void shouldRegisterServiceByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LicenseCreateService.class);
        });
    }

    /**
     * 显式关闭后应整体跳过装配。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.license.generator.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LicenseCreateService.class);
            });
    }

    /**
     * 使用方注册自定义生成服务时应替换默认单例（@ConditionalOnMissingBean 退让）。
     */
    @Test
    void shouldUseCustomServiceInsteadOfDefault() {
        LicenseCreateService customService = mock(LicenseCreateService.class);
        this.contextRunner.withBean(LicenseCreateService.class, () -> customService)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(LicenseCreateService.class);
                assertThat(context.getBean(LicenseCreateService.class)).isSameAs(customService);
            });
    }
}
