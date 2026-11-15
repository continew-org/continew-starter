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

package top.continew.starter.apidoc.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.apidoc.util.ApiDocUtils;
import top.continew.starter.core.enums.BaseEnum;

/**
 * {@link BaseEnumProcessor} 与 {@link ApiDocUtils} 单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class BaseEnumProcessorTest {

    /**
     * 实现 BaseEnum 的枚举应被支持；普通枚举与普通类不支持。
     */
    @Test
    @DisplayName("supports 按 BaseEnum 判定")
    void shouldSupportOnlyBaseEnum() {
        BaseEnumProcessor processor = new BaseEnumProcessor();

        assertThat(processor.supports(SampleEnum.class)).isTrue();
        assertThat(processor.supports(PlainEnum.class)).isFalse();
        assertThat(processor.supports(String.class)).isFalse();
        assertThat(processor.supports(null)).isFalse();
    }

    /**
     * 枚举接口类型应返回 BaseEnum。
     */
    @Test
    @DisplayName("getEnumInterfaceType 返回 BaseEnum")
    void shouldReturnBaseEnumInterface() {
        assertThat(new BaseEnumProcessor().getEnumInterfaceType()).isEqualTo(BaseEnum.class);
    }

    /**
     * buildGroupedOpenApi 双参应按 group 构建分组并携带路径匹配。
     */
    @Test
    @DisplayName("buildGroupedOpenApi 构建分组")
    void shouldBuildGroupedOpenApi() {
        var grouped = ApiDocUtils.buildGroupedOpenApi("test-group", new String[] {"/api/**"});

        assertThat(grouped.getGroup()).isEqualTo("test-group");
        assertThat(grouped.getPathsToMatch()).containsExactly("/api/**");
    }

    /**
     * buildGroupedOpenApi 三参应携带显示名称。
     */
    @Test
    @DisplayName("buildGroupedOpenApi 携带显示名称")
    void shouldBuildGroupedOpenApiWithDisplayName() {
        var grouped =
            ApiDocUtils.buildGroupedOpenApi("test-group", "测试分组", new String[] {"/api/**"});

        assertThat(grouped.getGroup()).isEqualTo("test-group");
        assertThat(grouped.getDisplayName()).isEqualTo("测试分组");
        assertThat(grouped.getPathsToMatch()).containsExactly("/api/**");
    }

    /**
     * 实现 BaseEnum 的样例枚举
     */
    enum SampleEnum implements BaseEnum<String> {

        /**
         * 样例值
         */
        SAMPLE("S");

        private final String value;

        SampleEnum(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getDescription() {
            return "样例枚举";
        }
    }

    /**
     * 未实现 BaseEnum 的普通枚举
     */
    enum PlainEnum {

        /**
         * 普通值
         */
        PLAIN
    }
}
