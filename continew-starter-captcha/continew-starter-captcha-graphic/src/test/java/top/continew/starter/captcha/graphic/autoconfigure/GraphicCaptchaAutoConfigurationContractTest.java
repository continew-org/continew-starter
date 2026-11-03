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

package top.continew.starter.captcha.graphic.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.captcha.graphic.core.GraphicCaptchaService;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * 图形验证码自动配置契约测试
 *
 * <p>固化 GraphicCaptchaAutoConfiguration 的装配契约：imports 登记完整性、开关默认
 * 启用、验证码服务用户回退与配置属性绑定。模块无前置 Bean 依赖，装配只受开关控制。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class GraphicCaptchaAutoConfigurationContractTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(GraphicCaptchaAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.captcha.graphic").isEmpty());
    }

    /**
     * 开关默认启用（matchIfMissing），验证码服务应注册。
     */
    @Test
    void shouldRegisterCaptchaServiceByDefault() {
        this.runner.run(context -> assertThat(context).hasSingleBean(GraphicCaptchaService.class));
    }

    /**
     * 开关关闭时应整体跳过，不产生任何副作用 Bean。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.runner.withPropertyValues("continew-starter.captcha.graphic.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(GraphicCaptchaService.class);
            });
    }

    /**
     * 用户注册 GraphicCaptchaService 时应回退到用户实现，默认服务不再创建。
     */
    @Test
    void shouldUseUserCaptchaServiceWhenProvided() {
        GraphicCaptchaService userService =
            new GraphicCaptchaService(new GraphicCaptchaProperties());
        this.runner.withBean(GraphicCaptchaService.class, () -> userService).run(context -> {
            assertThat(context).hasSingleBean(GraphicCaptchaService.class);
            assertSame(userService, context.getBean(GraphicCaptchaService.class));
        });
    }

    /**
     * 验证码内容长度属性应支持默认值与覆盖值绑定。
     */
    @Test
    void shouldBindLengthProperty() {
        this.runner.run(context -> assertThat(context.getBean(GraphicCaptchaProperties.class)
            .getLength()).isEqualTo(4));
        this.runner.withPropertyValues("continew-starter.captcha.graphic.length=6")
            .run(context -> assertThat(context.getBean(GraphicCaptchaProperties.class)
                .getLength()).isEqualTo(6));
    }
}
