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

import de.schlichtherle.license.LicenseManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.license.bean.LicenseInstallerBean;
import top.continew.starter.license.exception.LicenseException;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * License 校验自动配置装配契约测试
 *
 * <p>开关为 {@code continew-starter.license.verifier.enabled}（缺省视为启用）。启用即强制校验：
 * 初始化 Bean 在容器启动阶段真实安装证书，缺证书时必须启动失败而不允许静默放行，该语义通过
 * 异常根因链在此固化。属性绑定不设独立用例：{@code enabled=false} 时整个配置类连同属性 Bean
 * 一并跳过，而启用场景启动先于属性可观测，契约层无法独立断言。模块各 Bean 均无
 * {@code @ConditionalOnMissingBean}，不设 fallback 用例。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseVerifyAutoConfigurationContractTest {

    // 自动配置类位于跨生成/校验共享的 autoconfigure 包，扫描基础包取模块根包
    private static final String BASE_PACKAGE = "top.continew.starter.license";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LicenseVerifyAutoConfiguration.class));

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 显式关闭后应整体跳过装配，不产生任何 License 侧 Bean。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.license.verifier.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LicenseInstallerBean.class);
                assertThat(context).doesNotHaveBean(LicenseManager.class);
            });
    }

    /**
     * 默认启用且无证书时应启动失败，根因链上必须存在 LicenseException。
     */
    @Test
    void shouldFailStartupWithoutLicenseWhenEnabled() {
        this.contextRunner.run(context -> {
            Throwable failure = context.getStartupFailure();
            assertThat(failure).as("启用校验器后缺证书应导致启动失败").isNotNull();
            assertThat(hasCauseOfType(failure, LicenseException.class))
                .as("启动失败根因链应包含 LicenseException")
                .isTrue();
        });
    }

    /**
     * 遍历异常根因链判断是否包含目标类型。
     *
     * @param failure 启动失败异常
     * @param type    目标异常类型
     * @return 根因链上存在目标类型实例时返回 true
     */
    private static boolean hasCauseOfType(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
