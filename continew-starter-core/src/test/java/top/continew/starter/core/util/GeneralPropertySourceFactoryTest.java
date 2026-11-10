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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;

/**
 * 通用配置文件读取工厂测试
 *
 * <p>
 * 锁定 yml/yaml 扩展名走 YAML 加载、其余扩展名走父类 properties 加载的路由契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class GeneralPropertySourceFactoryTest {

    private final GeneralPropertySourceFactory factory = new GeneralPropertySourceFactory();

    /**
     * 构造类路径编码资源
     *
     * @param location 类路径位置
     * @return 编码资源
     */
    private static EncodedResource classPathResource(String location) {
        Resource resource = new ClassPathResource(location);
        return new EncodedResource(resource);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource({
        "property-source-factory/test-config.yml, test-config.yml",
        "property-source-factory/test-config.yaml, test-config.yaml",
        "property-source-factory/test-config.properties, class path resource [property-source-factory/test-config.properties]"})
    @DisplayName("扩展名路由：yml/yaml 按 YAML 加载并以文件名命名，properties 回退父类以资源描述命名")
    void shouldRoutePropertySourceNameByExtension(String location, String expectedName)
        throws IOException {
        PropertySource<?> source = factory.createPropertySource(null, classPathResource(location));
        assertThat(source.getName()).isEqualTo(expectedName);
        assertThat(source.getProperty("provider.name")).isEqualTo("continew");
    }

    @Test
    @DisplayName("yml 扩展名按 YAML 加载时数值属性被解析为 Integer，properties 路由无此转换")
    void shouldParseNumericValueAsIntegerForYamlRoute() throws IOException {
        PropertySource<?> yamlSource = factory
            .createPropertySource(null,
                classPathResource("property-source-factory/test-config.yml"));
        assertThat(yamlSource.getProperty("provider.version")).isEqualTo(3);
        PropertySource<?> propertiesSource = factory
            .createPropertySource(null,
                classPathResource("property-source-factory/test-config.properties"));
        assertThat(propertiesSource.getProperty("provider.version")).isEqualTo("3");
    }

    @Test
    @DisplayName("显式传入的资源名在 properties 路由下作为 PropertySource 名称")
    void shouldKeepExplicitNameForPropertiesRoute() throws IOException {
        PropertySource<?> source = factory
            .createPropertySource("custom-name",
                classPathResource("property-source-factory/test-config.properties"));
        assertThat(source.getName()).isEqualTo("custom-name");
    }

    @Test
    @DisplayName("无扩展名文件回退父类加载路径")
    void shouldFallbackForExtensionlessFile() throws IOException {
        PropertySource<?> source = factory
            .createPropertySource(null, classPathResource("property-source-factory/test-config"));
        assertThat(source.getProperty("provider.name")).isEqualTo("continew");
    }

    @Test
    @DisplayName("大写扩展名不触发 YAML 路由（锁定当前行为：扩展名判断区分大小写）")
    void shouldNotRouteUpperCaseExtension() throws IOException {
        PropertySource<?> source = factory
            .createPropertySource(null,
                classPathResource("property-source-factory/test-config-upper.YML"));
        // 大写扩展名不满足 endWithAny 判断，回退父类后以资源描述命名
        assertThat(source.getName())
            .isEqualTo("class path resource [property-source-factory/test-config-upper.YML]");
        assertThat(source.getProperty("provider.name")).isEqualTo("continew");
    }
}
