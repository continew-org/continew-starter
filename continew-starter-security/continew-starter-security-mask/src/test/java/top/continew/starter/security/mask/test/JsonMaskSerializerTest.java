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

package top.continew.starter.security.mask.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import top.continew.starter.security.mask.annotation.JsonMask;
import top.continew.starter.security.mask.enums.MaskType;

/**
 * JSON 脱敏序列化测试（基于 Jackson 3）
 *
 * @author Charles7c
 * @since 3.0.0
 */
class JsonMaskSerializerTest {

    /**
     * 脱敏测试模型
     */
    static class MaskedModel {

        @JsonMask(MaskType.MOBILE_PHONE)
        private String phone;

        @JsonMask(value = MaskType.CUSTOM, left = 2, right = 2)
        private String name;

        @JsonMask(MaskType.MOBILE_PHONE)
        private String empty;

        private Integer age;

        MaskedModel(String phone, String name, String empty, Integer age) {
            this.phone = phone;
            this.name = name;
            this.empty = empty;
            this.age = age;
        }

        public String getPhone() {
            return phone;
        }

        public String getName() {
            return name;
        }

        public String getEmpty() {
            return empty;
        }

        public Integer getAge() {
            return age;
        }
    }

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("手机号类型脱敏（保留前 3 后 4）")
    void shouldMaskMobilePhone() {
        MaskedModel model = new MaskedModel("13812345678", "张三", "13900000000", 18);
        String json = jsonMapper.writeValueAsString(model);
        assertTrue(json.contains("\"phone\":\"138****5678\""), json);
        assertTrue(json.contains("\"empty\":\"139****0000\""), json);
    }

    @Test
    @DisplayName("自定义脱敏（保留前 2 后 2）")
    void shouldMaskWithCustomRange() {
        MaskedModel model = new MaskedModel("13812345678", "张三四五六", null, 18);
        String json = jsonMapper.writeValueAsString(model);
        assertTrue(json.contains("\"name\":\"张三*五六\""), json);
    }

    @Test
    @DisplayName("null 值与非 String 类型字段不参与脱敏且不报错")
    void shouldHandleNullAndNonStringFields() {
        MaskedModel model = new MaskedModel(null, null, null, 18);
        String json = jsonMapper.writeValueAsString(model);
        assertFalse(json.contains("138"), json);
        assertTrue(json.contains("\"age\":18"), json);
    }

    @Test
    @DisplayName("空白字符串脱敏为空字符串")
    void shouldMaskBlankStringAsEmpty() {
        MaskedModel model = new MaskedModel("13812345678", "张三", " ", 18);
        String json = jsonMapper.writeValueAsString(model);
        assertTrue(json.contains("\"empty\":\"\""), json);
    }
}
