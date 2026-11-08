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

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.ser.jdk.NumberSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 大数值序列化器
 * <p>
 * 将 JS 取值范围之外的数值转换为字符串
 * </p>
 *
 * @author Charles7c
 * @since 1.0.0
 */
@JacksonStdImpl
public class BigNumberSerializer extends NumberSerializer {

    /**
     * 静态实例
     */
    public static final BigNumberSerializer SERIALIZER_INSTANCE =
        new BigNumberSerializer(Number.class);
    /**
     * JS：Number.MAX_SAFE_INTEGER
     */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    /**
     * JS：Number.MIN_SAFE_INTEGER
     */
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;
    /**
     * 超出该位长的 BigInteger 必然超出 JS 安全整数区间
     */
    private static final int MAX_SAFE_INTEGER_BITS = 53;
    /**
     * JS 安全整数上界的 BigDecimal 形式：BigDecimal 数值可能远超 long 范围，必须用 compareTo 比较，longValue() 会静默回绕
     */
    private static final BigDecimal MAX_SAFE_INTEGER_DECIMAL = BigDecimal.valueOf(MAX_SAFE_INTEGER);
    /**
     * 2^51：绝对值超过该值时 double 最小间距 >= 0.5，小数部分可能无法精确表示，需要逐值判定
     */
    private static final BigDecimal FRACTION_LOSSY_LIMIT = BigDecimal.valueOf(1L << 51);

    public BigNumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    @Override
    public void serialize(Number value, JsonGenerator generator, SerializationContext context) {
        if (value instanceof BigInteger bigInteger) {
            // bitLength 小于等于安全位长时才可安全调用 longValue()，否则超 long 范围的值会静默回绕
            if (bigInteger.bitLength() <= MAX_SAFE_INTEGER_BITS
                && inJsSafeIntegerRange(bigInteger.longValue())) {
                super.serialize(value, generator, context);
            } else {
                generator.writeString(bigInteger.toString());
            }
            return;
        }
        if (value instanceof BigDecimal decimal) {
            BigDecimal abs = decimal.abs();
            if (abs.compareTo(MAX_SAFE_INTEGER_DECIMAL) > 0
                || (abs.compareTo(FRACTION_LOSSY_LIMIT) >= 0
                    && !isExactlyRepresentableAsDouble(decimal))) {
                generator.writeString(decimal.toString());
            } else {
                super.serialize(value, generator, context);
            }
            return;
        }
        if (inJsSafeIntegerRange(value.longValue())) {
            super.serialize(value, generator, context);
        } else {
            generator.writeString(value.toString());
        }
    }

    /**
     * 判断数值是否处于 JS 安全整数区间
     * <p>
     * JS 安全整数为闭区间 [Number.MIN_SAFE_INTEGER, Number.MAX_SAFE_INTEGER]，边界值本身可被 JS 精确表示
     * </p>
     *
     * @param longValue 待判断的 long 值
     * @return 是否处于 JS 安全整数区间
     */
    private static boolean inJsSafeIntegerRange(long longValue) {
        return longValue >= MIN_SAFE_INTEGER && longValue <= MAX_SAFE_INTEGER;
    }

    /**
     * 判断 BigDecimal 的数值能否被 double 精确表示（即前端 JSON.parse 后不产生精度损失）
     *
     * @param decimal 待判断的 BigDecimal 值
     * @return 能否被 double 精确表示
     */
    private static boolean isExactlyRepresentableAsDouble(BigDecimal decimal) {
        double asDouble = decimal.doubleValue();
        // 超出 double 范围时 round-trip 比较无意义（Infinity）
        return !Double.isInfinite(asDouble) && BigDecimal.valueOf(asDouble).compareTo(decimal) == 0;
    }
}
