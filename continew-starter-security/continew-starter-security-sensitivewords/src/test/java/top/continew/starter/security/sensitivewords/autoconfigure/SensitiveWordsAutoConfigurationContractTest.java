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

package top.continew.starter.security.sensitivewords.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.security.sensitivewords.service.NoOpSensitiveWordsService;
import top.continew.starter.security.sensitivewords.service.SensitiveWordsService;
import top.continew.starter.security.sensitivewords.support.RuntimeWordDeny;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 敏感词自动配置装配契约测试
 *
 * <p>模块顶层无条件装配，检测开关作用在 {@code SensitiveWordBs} 工厂方法上
 * （{@code continew-starter.security.sensitive-words.enabled}，缺省视为启用）；
 * {@code enabled=false} 时不仅引擎跳过，服务还必须降级为空实现，保证「关闭即不检测」
 * 无条件成立。运行时词库为内部状态载体，不开放替换，其存在性也一并固化。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SensitiveWordsAutoConfigurationContractTest {

    private static final String BASE_PACKAGE = "top.continew.starter.security.sensitivewords";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SensitiveWordsAutoConfiguration.class));

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 默认应装配引擎、服务与运行时词库。
     */
    @Test
    void shouldRegisterEngineAndServiceByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SensitiveWordBs.class);
            assertThat(context).hasSingleBean(SensitiveWordsService.class);
            assertThat(context).hasSingleBean(RuntimeWordDeny.class);
        });
    }

    /**
     * enabled=false 时引擎应跳过且服务降级为空实现。
     */
    @Test
    void shouldFallbackToNoOpServiceWhenDisabled() {
        this.contextRunner
            .withPropertyValues("continew-starter.security.sensitive-words.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(SensitiveWordBs.class);
                assertThat(context).hasSingleBean(SensitiveWordsService.class);
                assertThat(context.getBean(SensitiveWordsService.class))
                    .isInstanceOf(NoOpSensitiveWordsService.class);
            });
    }

    /**
     * 使用方注册自定义引擎时应替换默认工厂（@ConditionalOnMissingBean 退让）。
     */
    @Test
    void shouldUseCustomWordBsInsteadOfDefault() {
        SensitiveWordBs customWordBs = mock(SensitiveWordBs.class);
        this.contextRunner.withBean(SensitiveWordBs.class, () -> customWordBs)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(SensitiveWordBs.class);
                assertThat(context.getBean(SensitiveWordBs.class)).isSameAs(customWordBs);
            });
    }

    /**
     * denyWords 覆盖值应经属性绑定进入检测引擎并生效。
     */
    @Test
    void shouldBindDenyWordsPropertyToEngine() {
        this.contextRunner
            .withPropertyValues(
                "continew-starter.security.sensitive-words.deny-words[0]=continew-test-deny-word")
            .run(context -> {
                assertThat(context).hasNotFailed();
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                assertThat(service.contains("包含 continew-test-deny-word 的文本")).isTrue();
                assertThat(service.contains("不含敏感词的普通文本")).isFalse();
            });
    }
}
