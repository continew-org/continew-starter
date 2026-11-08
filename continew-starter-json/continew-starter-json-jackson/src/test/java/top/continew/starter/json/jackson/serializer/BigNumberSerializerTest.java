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

package top.continew.starter.json.jackson.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 大数值序列化器测试
 *
 * <p>以 JS 安全整数闭区间 [-9007199254740991, 9007199254740991] 为基准，固化
 * BigInteger 不经 longValue() 回绕、BigDecimal 精度判定与边界值闭区间语义。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class BigNumberSerializerTest {

    private static final String BIG_INTEGER_BEYOND_LONG = "18446744073709551617";
    private static JsonMapper mapper;

    @BeforeAll
    static void initMapper() {
        // 与 FLEXIBLE 模式的注册矩阵一致
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, BigNumberSerializer.SERIALIZER_INSTANCE);
        module.addSerializer(Long.TYPE, BigNumberSerializer.SERIALIZER_INSTANCE);
        module.addSerializer(BigInteger.class, BigNumberSerializer.SERIALIZER_INSTANCE);
        module.addSerializer(BigDecimal.class, BigNumberSerializer.SERIALIZER_INSTANCE);
        mapper = JsonMapper.builder().addModule(module).build();
    }

    /**
     * 断言按 FLEXIBLE 注册矩阵序列化后的 JSON 输出。
     *
     * @param value 待序列化数值
     * @param expected 期望的 JSON 输出
     */
    private void assertJson(Number value, String expected) {
        assertEquals(expected, mapper.writeValueAsString(value));
    }

    @Test
    @DisplayName("BigInteger 超出 long 范围不再经 longValue() 回绕，输出字符串原值")
    void bigIntegerBeyondLongShouldSerializeAsString() {
        // 2^64 + 1 的 longValue() 回绕为 1，必须先以 bitLength 判定，超出安全范围的数字经前端解析必丢精度
        assertJson(new BigInteger(BIG_INTEGER_BEYOND_LONG), "\"" + BIG_INTEGER_BEYOND_LONG + "\"");
        assertJson(new BigInteger(BIG_INTEGER_BEYOND_LONG).negate(), "\"-" + BIG_INTEGER_BEYOND_LONG
            + "\"");
        assertJson(new BigInteger("18446744073709551616"), "\"18446744073709551616\"");
    }

    @Test
    @DisplayName("BigInteger 安全范围内保持数字输出且逐值正确")
    void bigIntegerWithinSafeRangeShouldSerializeAsNumber() {
        assertJson(BigInteger.valueOf(123), "123");
        assertJson(BigInteger.valueOf(0), "0");
        assertJson(BigInteger.valueOf(-123), "-123");
        assertJson(new BigInteger("9007199254740990"), "9007199254740990");
    }

    @Test
    @DisplayName("边界值 ±9007199254740991 为闭区间，保持数字输出")
    void boundaryValuesShouldStayAsNumber() {
        assertJson(Long.valueOf(9007199254740991L), "9007199254740991");
        assertJson(Long.valueOf(-9007199254740991L), "-9007199254740991");
        assertJson(BigInteger.valueOf(9007199254740991L), "9007199254740991");
        assertJson(BigInteger.valueOf(-9007199254740991L), "-9007199254740991");
        assertJson(new BigDecimal("9007199254740991"), "9007199254740991");
    }

    @Test
    @DisplayName("边界外紧邻值 ±9007199254740992 转为字符串")
    void valuesJustBeyondBoundaryShouldSerializeAsString() {
        assertJson(Long.valueOf(9007199254740992L), "\"9007199254740992\"");
        assertJson(Long.valueOf(-9007199254740992L), "\"-9007199254740992\"");
        assertJson(BigInteger.valueOf(9007199254740992L), "\"9007199254740992\"");
    }

    @Test
    @DisplayName("BigDecimal 常规金额保持数字，超安全范围转字符串")
    void bigDecimalShouldSwitchBySafeRange() {
        assertJson(new BigDecimal("123.45"), "123.45");
        assertJson(new BigDecimal("-123.45"), "-123.45");
        assertJson(new BigDecimal("100000000000000000000"), "\"100000000000000000000\"");
        assertJson(new BigDecimal("-100000000000000000000"), "\"-100000000000000000000\"");
        assertJson(new BigDecimal("9007199254740992"), "\"9007199254740992\"");
    }

    @Test
    @DisplayName("BigDecimal 小数可能丢失的量级逐值判定：不可精确表示转字符串，可精确表示保持数字")
    void bigDecimalFractionShouldBeJudgedPerValue() {
        // double 在 [2^51, 2^53) 区间最小间距已达 0.5，带此小数的值经 JSON.parse 必丢精度
        assertJson(new BigDecimal("9007199254740990.999"), "\"9007199254740990.999\"");
        assertJson(new BigDecimal("2251799813685248.75"), "\"2251799813685248.75\"");
        // 2^51 + 0.5 恰为该区间 double 的可精确表示值
        assertJson(new BigDecimal("2251799813685248.5"), "2251799813685248.5");
        // 2^52 为可精确表示的整数值
        assertJson(new BigDecimal("4503599627370496"), "4503599627370496");
    }

    @Test
    @DisplayName("Long/long 相邻路径不受影响：常规值数字，超范围转字符串")
    void longPathShouldKeepOriginalBehavior() {
        assertJson(Long.valueOf(123L), "123");
        assertJson(0L, "0");
        assertJson(Long.valueOf(9223372036854775807L), "\"9223372036854775807\"");
        assertJson(Long.valueOf(-9223372036854775808L), "\"-9223372036854775808\"");
    }
}
