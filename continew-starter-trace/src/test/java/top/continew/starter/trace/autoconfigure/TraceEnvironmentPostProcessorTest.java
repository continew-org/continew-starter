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

package top.continew.starter.trace.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

/**
 * 链路追踪环境后置处理器测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class TraceEnvironmentPostProcessorTest {

    private final TraceEnvironmentPostProcessor postProcessor = new TraceEnvironmentPostProcessor();

    /**
     * 未启用链路追踪时不补采样率
     */
    @Test
    @DisplayName("未启用链路追踪时不补采样率")
    void shouldNotConfigureSamplingWhenDisabled() {
        MockEnvironment environment = new MockEnvironment();

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty(TraceEnvironmentPostProcessor.SAMPLING_PROBABILITY))
            .isNull();
    }

    /**
     * 启用链路追踪时采样率默认补 1.0
     */
    @Test
    @DisplayName("启用链路追踪时采样率默认补 1.0")
    void shouldConfigureDefaultSampling() {
        MockEnvironment environment =
            new MockEnvironment().withProperty("continew-starter.trace.enabled", "true");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty(TraceEnvironmentPostProcessor.SAMPLING_PROBABILITY,
            Float.class))
            .isEqualTo(1.0F);
    }

    /**
     * 显式配置的采样率不被覆盖
     */
    @Test
    @DisplayName("显式配置的采样率不被覆盖")
    void shouldNotOverrideExplicitSampling() {
        MockEnvironment environment =
            new MockEnvironment().withProperty("continew-starter.trace.enabled", "true")
                .withProperty(TraceEnvironmentPostProcessor.SAMPLING_PROBABILITY, "0.5");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty(TraceEnvironmentPostProcessor.SAMPLING_PROBABILITY,
            Float.class))
            .isEqualTo(0.5F);
    }

    /**
     * 通过 spring.factories 注册并被 SpringApplication 加载
     */
    @Test
    @DisplayName("通过 spring.factories 注册并被 SpringApplication 加载")
    void shouldBeRegisteredAsEnvironmentPostProcessor() {
        SpringApplication application = new SpringApplication(TestConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);

        try (ConfigurableApplicationContext context = application
            .run("--continew-starter.trace.enabled=true")) {
            assertThat(context.getEnvironment()
                .getProperty(TraceEnvironmentPostProcessor.SAMPLING_PROBABILITY, Float.class))
                .isEqualTo(1.0F);
        }
    }

    /**
     * 测试用配置
     */
    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

    }
}
