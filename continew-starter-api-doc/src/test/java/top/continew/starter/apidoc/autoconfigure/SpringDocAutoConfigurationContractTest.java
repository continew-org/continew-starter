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

package top.continew.starter.apidoc.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.core.autoconfigure.application.ApplicationAutoConfiguration;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;
import top.nextdoc4j.enums.resolver.EnumMetadataResolver;

/**
 * API 文档模块自动配置契约测试
 *
 * <p>固化 SpringDocAutoConfiguration 的契约：imports 登记完整性、默认装配、
 * application 前缀属性到 OpenAPI 信息的绑定与 {@code @ConditionalOnMissingBean} 回退。</p>
 *
 * <p>本模块源码无 @ConditionalOnClass 条件（springdoc 为编译期硬依赖），无需
 * FilteredClassLoader 用例；亦无自有 @ConfigurationProperties，属性绑定以 core 的
 * application 前缀属性作用于 openApi Bean 的行为验证。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SpringDocAutoConfigurationContractTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ApplicationAutoConfiguration.class,
            SpringDocAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert.findUnregistered("top.continew.starter.apidoc")
            .isEmpty());
    }

    /**
     * 无装配开关，默认应注册 OpenAPI 配置与枚举元数据解析器。
     */
    @Test
    void beansShouldRegisterByDefault() {
        this.runner.run(context -> {
            assertThat(context).hasSingleBean(OpenAPI.class);
            assertThat(context).hasSingleBean(EnumMetadataResolver.class);
        });
    }

    /**
     * openApi 应消费 core 的 application 前缀属性：默认 version 为空，覆盖后标题与版本随之变化。
     */
    @Test
    void openApiShouldBindApplicationProperties() {
        this.runner.run(context -> assertNull(context.getBean(OpenAPI.class).getInfo()
            .getVersion()));
        this.runner.withPropertyValues("application.name=ContiNew", "application.version=3.1.0")
            .run(context -> {
                OpenAPI openApi = context.getBean(OpenAPI.class);
                assertEquals("ContiNew API 文档", openApi.getInfo().getTitle());
                assertEquals("3.1.0", openApi.getInfo().getVersion());
            });
    }

    /**
     * 用户提供 OpenAPI 时应整体回退，容器中保持用户实例（@ConditionalOnMissingBean 语义）。
     */
    @Test
    void openApiShouldFallbackToUserProvidedBean() {
        OpenAPI custom = new OpenAPI();
        this.runner.withBean(OpenAPI.class, () -> custom).run(context -> {
            assertThat(context).hasSingleBean(OpenAPI.class);
            assertSame(custom, context.getBean(OpenAPI.class));
        });
    }
}
