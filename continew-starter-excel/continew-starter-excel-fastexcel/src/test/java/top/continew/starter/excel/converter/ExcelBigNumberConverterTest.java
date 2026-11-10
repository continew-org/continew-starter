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

package top.continew.starter.excel.converter;

import static org.assertj.core.api.Assertions.assertThat;

import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

/**
 * {@link ExcelBigNumberConverter} 单元测试。
 * <p>
 * 超过 15 位的数值在 Excel 中只能以文本录入，因此写入时按长度分流：15 位以内走 NUMBER 单元格，超出走 STRING 单元格。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelBigNumberConverterTest {

    private final ExcelBigNumberConverter converter = new ExcelBigNumberConverter();
    private final ExcelContentProperty contentProperty = new ExcelContentProperty();
    private final GlobalConfiguration globalConfiguration = new GlobalConfiguration();

    @Test
    @DisplayName("支持的 Java 类型为 Long，Excel 类型为 STRING")
    void shouldSupportLongAndStringCell() {
        assertThat(converter.supportJavaTypeKey()).isEqualTo(Long.class);
        assertThat(converter.supportExcelTypeKey()).isEqualTo(CellDataTypeEnum.STRING);
    }

    @ParameterizedTest(name = "[{index}] 值 {0}（{1} 位）写入为 NUMBER 单元格")
    @MethodSource("withinMaxLengthProvider")
    @DisplayName("长度不超过 15 位时写入为 NUMBER 单元格")
    void shouldWriteNumberCellWithinMaxLength(Long value, int length) {
        WriteCellData<Object> cellData =
            converter.convertToExcelData(value, contentProperty, globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.NUMBER);
        assertThat(cellData.getNumberValue()).isEqualByComparingTo(BigDecimal.valueOf(value));
    }

    @ParameterizedTest(name = "[{index}] 值 {0}（{1} 位）写入为 STRING 单元格，原样保留数字文本")
    @MethodSource("overMaxLengthProvider")
    @DisplayName("长度超过 15 位时写入为 STRING 单元格")
    void shouldWriteStringCellOverMaxLength(Long value, int length) {
        WriteCellData<Object> cellData =
            converter.convertToExcelData(value, contentProperty, globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.STRING);
        assertThat(cellData.getStringValue()).isEqualTo(Long.toString(value));
    }

    @Test
    @DisplayName("null 值写入为 NUMBER 单元格的 0")
    void shouldWriteNullAsZeroNumberCell() {
        WriteCellData<Object> cellData =
            converter.convertToExcelData(null, contentProperty, globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.NUMBER);
        assertThat(cellData.getNumberValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("读取 NUMBER 单元格的 data 为 Long")
    void shouldReadNumberCellDataToLong() {
        ReadCellData<Object> cellData = new ReadCellData<>();
        cellData.setType(CellDataTypeEnum.NUMBER);
        cellData.setData(new BigDecimal("610123456789012378"));
        Long converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).isEqualTo(610123456789012378L);
    }

    @Test
    @DisplayName("锁定当前行为：单元格 data 未填充时读取结果为 null")
    void shouldReadNullDataToNull() {
        // new ReadCellData<>(String) 仅填充 stringValue，data 为 null；转换器依赖 getData()，此时结果为 null
        ReadCellData<String> cellData = new ReadCellData<>("610123456789012378");
        Long converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).isNull();
    }

    private static Stream<Arguments> withinMaxLengthProvider() {
        return Stream.of(Arguments.arguments(123L, 3), Arguments.arguments(999999999999999L, 15));
    }

    private static Stream<Arguments> overMaxLengthProvider() {
        return Stream.of(Arguments.arguments(10000000000000000L, 17), Arguments
            .arguments(6101234567890123789L, 19));
    }
}
