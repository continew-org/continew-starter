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

package top.continew.starter.web.autoconfigure.mvc.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.enums.BaseEnum;

/**
 * BaseEnum 参数转换器单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("BaseEnumConverter 单元测试")
class BaseEnumConverterTest {

    private final BaseEnumConverter<StatusEnum> converter =
        new BaseEnumConverter<>(StatusEnum.class);

    @Test
    @DisplayName("按枚举值字符串转换，未命中返回 null")
    void shouldConvertByEnumValue() {
        assertThat(converter.convert("1")).isEqualTo(StatusEnum.ENABLED);
        assertThat(converter.convert("0")).isEqualTo(StatusEnum.DISABLED);
        assertThat(converter.convert("unknown")).isNull();
    }

    private enum StatusEnum implements BaseEnum<Integer> {

        ENABLED(1, "启用"),
        DISABLED(0, "禁用");

        private final Integer value;
        private final String description;

        StatusEnum(Integer value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public Integer getValue() {
            return value;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }
}
