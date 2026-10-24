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

package top.continew.starter.json.jackson.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Jackson 自动配置集成测试（行为快照：与 2.x 版本行为保持一致）
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest
class JacksonAutoConfigurationTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("容器内 ObjectMapper 为 Jackson 3 JsonMapper")
    void shouldProvideJackson3Mapper() {
        assertNotNull(jsonMapper);
        assertEquals("tools.jackson.databind.json.JsonMapper", jsonMapper.getClass().getName());
    }

    @Test
    @DisplayName("LocalDateTime 序列化为 yyyy-MM-dd HH:mm:ss 格式")
    void shouldSerializeLocalDateTimeWithPattern() {
        TestModel model = new TestModel();
        model.setCreateTime(LocalDateTime.of(2026, 10, 23, 8, 30, 0));
        String json = jsonMapper.writeValueAsString(model);
        assertTrue(json.contains("\"createTime\":\"2026-10-23 08:30:00\""), json);
    }

    @Test
    @DisplayName("LocalDateTime 按 yyyy-MM-dd HH:mm:ss 格式反序列化")
    void shouldDeserializeLocalDateTimeWithPattern() {
        String json = "{\"createTime\":\"2026-10-23 08:30:00\"}";
        TestModel model = jsonMapper.readValue(json, TestModel.class);
        assertEquals(LocalDateTime.of(2026, 10, 23, 8, 30, 0), model.getCreateTime());
    }

    @Test
    @DisplayName("BaseEnum 枚举序列化为 value 值（接口回退）")
    void shouldSerializeBaseEnumByValue() {
        TestModel model = new TestModel();
        model.setStatus(TestStringValueEnum.ACTIVE);
        String json = jsonMapper.writeValueAsString(model);
        assertTrue(json.contains("\"status\":\"1\""), json);
    }

    @Test
    @DisplayName("BaseEnum 枚举按 value 反序列化")
    void shouldDeserializeBaseEnumByValue() {
        TestModel model = jsonMapper.readValue("{\"status\":\"0\"}", TestModel.class);
        assertEquals(TestStringValueEnum.DISABLED, model.getStatus());
    }

    @Test
    @DisplayName("BaseEnum 枚举未知 value 反序列化抛出 InvalidFormatException")
    void shouldThrowOnUnknownEnumValue() {
        assertThrows(InvalidFormatException.class, () -> jsonMapper
            .readValue("{\"status\":\"unknown\"}", TestModel.class));
    }

    @Test
    @DisplayName("超出 JS 安全整数范围的 Long 序列化为字符串（FLEXIBLE 模式）")
    void shouldSerializeOutOfRangeLongAsString() {
        String json = jsonMapper.writeValueAsString(Long.valueOf(9007199254740992L));
        assertEquals("\"9007199254740992\"", json);
    }

    @Test
    @DisplayName("安全范围内的 Long 序列化为数字（FLEXIBLE 模式）")
    void shouldSerializeSafeLongAsNumber() {
        String json = jsonMapper.writeValueAsString(Long.valueOf(123L));
        assertEquals("123", json);
    }

    @Test
    @DisplayName("超出 JS 安全整数范围的 BigInteger 序列化为字符串（FLEXIBLE 模式）")
    void shouldSerializeOutOfRangeBigIntegerAsString() {
        String json =
            jsonMapper.writeValueAsString(new BigInteger("123456789012345678901234567890"));
        assertEquals("\"123456789012345678901234567890\"", json);
    }

    @Test
    @DisplayName("未知属性不抛异常（fail-on-unknown-properties=false）")
    void shouldIgnoreUnknownProperties() {
        TestModel model = jsonMapper.readValue("{\"unknownField\":\"any\"}", TestModel.class);
        assertNotNull(model);
    }

    @Test
    @DisplayName("属性按声明顺序输出（非字母序）")
    void shouldKeepPropertyDeclarationOrder() {
        TestModel model = new TestModel();
        model.setId(1L);
        model.setName("test");
        String json = jsonMapper.writeValueAsString(model);
        int idIndex = json.indexOf("\"id\"");
        int nameIndex = json.indexOf("\"name\"");
        int createTimeIndex = json.indexOf("\"createTime\"");
        assertTrue(idIndex < nameIndex && nameIndex < createTimeIndex, json);
    }

    @Test
    @DisplayName("@JsonIgnore 注解生效（Jackson 3 复用 annotations 包）")
    void shouldRespectJsonIgnore() {
        TestModel model = new TestModel();
        model.setSecret("hidden");
        String json = jsonMapper.writeValueAsString(model);
        assertFalse(json.contains("secret"), json);
    }

    @Test
    @DisplayName("空对象序列化不抛异常（fail-on-empty-beans=false）")
    void shouldSerializeEmptyBean() {
        String json = jsonMapper.writeValueAsString(new Object());
        assertEquals("{}", json);
    }

    @Test
    @DisplayName("时区跟随 JVM 默认值（与运行环境时区无关）")
    void shouldApplyJvmDefaultTimeZone() {
        java.util.Date date = java.util.Date.from(LocalDateTime.of(2026, 10, 23, 8, 0, 0)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant());
        String json = jsonMapper.writeValueAsString(date);
        assertTrue(json.contains("\"2026-10-23 08:00:00\""), json);
    }
}
