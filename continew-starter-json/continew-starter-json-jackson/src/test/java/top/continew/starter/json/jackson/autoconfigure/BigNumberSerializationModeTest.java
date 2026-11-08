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

package top.continew.starter.json.jackson.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;
import top.continew.starter.json.jackson.enums.BigNumberSerializeMode;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 大数值序列化模式注册测试
 *
 * <p>固化三种 {@link BigNumberSerializeMode} 下的注册矩阵：FLEXIBLE 与 TO_STRING
 * 覆盖 Long/long/BigInteger/BigDecimal 且行为一致，NO_OPERATE 不注册任何序列化器。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class BigNumberSerializationModeTest {

    /**
     * Boot 4.1 的 Jackson 自动配置与本模块自动配置类同名，以全限定类字面量消歧组合。
     */
    private static final Class<?> BOOT_JACKSON_AUTO_CONFIGURATION =
        org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class;

    private static final String MODE_PROPERTY = "spring.jackson.big-number-serialize-mode";

    private final ApplicationContextRunner fullRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(BOOT_JACKSON_AUTO_CONFIGURATION,
            JacksonAutoConfiguration.class));

    @Test
    @DisplayName("FLEXIBLE 模式：BigInteger 超 long 范围输出字符串，安全范围内输出数字")
    void flexibleShouldHandleBigInteger() {
        this.fullRunner.run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);
            assertThat(mapper.writeValueAsString(new BigInteger("18446744073709551617")))
                .isEqualTo("\"18446744073709551617\"");
            assertThat(mapper.writeValueAsString(BigInteger.valueOf(123))).isEqualTo("123");
        });
    }

    @Test
    @DisplayName("FLEXIBLE 模式：BigDecimal 超安全范围转字符串，常规金额保持数字")
    void flexibleShouldHandleBigDecimal() {
        this.fullRunner.run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);
            assertThat(mapper.writeValueAsString(new BigDecimal("100000000000000000000")))
                .isEqualTo("\"100000000000000000000\"");
            assertThat(mapper.writeValueAsString(new BigDecimal("123.45"))).isEqualTo("123.45");
        });
    }

    @Test
    @DisplayName("TO_STRING 模式：BigDecimal 与 Long/BigInteger 一致，安全范围内也输出字符串")
    void toStringModeShouldCoverBigDecimalConsistently() {
        this.fullRunner.withPropertyValues(MODE_PROPERTY + "=to_string").run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);
            assertThat(mapper.writeValueAsString(new BigDecimal("123.45"))).isEqualTo("\"123.45\"");
            assertThat(mapper.writeValueAsString(Long.valueOf(123L))).isEqualTo("\"123\"");
            assertThat(mapper.writeValueAsString(BigInteger.valueOf(123))).isEqualTo("\"123\"");
        });
    }

    @Test
    @DisplayName("NO_OPERATE 模式：不注册序列化器，数值按 Jackson 默认行为输出")
    void noOperateModeShouldNotRegisterSerializers() {
        this.fullRunner.withPropertyValues(MODE_PROPERTY + "=no_operate").run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);
            assertThat(mapper.writeValueAsString(Long.valueOf(9223372036854775807L)))
                .isEqualTo("9223372036854775807");
            assertThat(mapper.writeValueAsString(new BigDecimal("123.45"))).isEqualTo("123.45");
            assertThat(mapper.writeValueAsString(BigInteger.valueOf(123))).isEqualTo("123");
        });
    }
}
