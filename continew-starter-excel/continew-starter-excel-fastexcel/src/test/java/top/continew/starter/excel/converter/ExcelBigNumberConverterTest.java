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

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.idev.excel.read.listener.PageReadListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link ExcelBigNumberConverter} 单元测试。
 * <p>
 * 超过 15 位的数值在 Excel 中只能以文本录入，因此写入时按长度分流：15 位以内走 NUMBER 单元格，超出走 STRING 单元格；
 * 读取方向从单元格真实存放字段取值（NUMBER 取 numberValue，STRING 取 stringValue），并以真实 workbook 写出再读回的往返链路锁定读取行为。
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
    @DisplayName("读取 STRING 单元格中的数字文本为 Long")
    void shouldReadStringCellDataToLong() {
        // new ReadCellData<>(String) 仅填充 stringValue，与真实读取链路的字段填充行为一致
        ReadCellData<String> cellData = new ReadCellData<>("610123456789012378");
        Long converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).isEqualTo(610123456789012378L);
    }

    @Test
    @DisplayName("读取 NUMBER 单元格中的数值为 Long")
    void shouldReadNumberCellDataToLong() {
        ReadCellData<BigDecimal> cellData =
            new ReadCellData<>(new BigDecimal("610123456789012378"));
        Long converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).isEqualTo(610123456789012378L);
    }

    @Test
    @DisplayName("非数字文本读取为 null")
    void shouldReadNonNumericTextToNull() {
        ReadCellData<String> cellData = new ReadCellData<>("非数字");
        Long converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).isNull();
    }

    @Test
    @DisplayName("真实 workbook 往返：超过 15 位的大数值经 STRING 单元格写出后读回保持")
    void shouldRoundTripOverMaxLengthValueThroughWorkbook() {
        List<BigNumberRow> readRows = roundTrip(List.of(new BigNumberRow(6101234567890123789L)));

        assertThat(readRows).hasSize(1);
        assertThat(readRows.get(0).getId()).isEqualTo(6101234567890123789L);
    }

    @Test
    @DisplayName("真实 workbook 往返：超过 Integer.MAX_VALUE 的 15 位以内数值经 NUMBER 单元格写出后读回保持")
    void shouldRoundTripWithinMaxLengthValueThroughWorkbook() {
        List<BigNumberRow> readRows =
            roundTrip(List.of(new BigNumberRow(999999999999999L), new BigNumberRow(2147483648L)));

        assertThat(readRows).hasSize(2);
        assertThat(readRows.get(0).getId()).isEqualTo(999999999999999L);
        assertThat(readRows.get(1).getId()).isEqualTo(2147483648L);
    }

    /**
     * 将行数据经真实 workbook 写出后读回，读写两端均注册被测转换器，与 ExcelUtils 的注册方式一致：
     * 写端注册 {@link ExcelBigNumberWriteConverter} 占用 (Long, null) 槽位，读端注册 {@link ExcelBigNumberConverter} 处理 STRING 单元格
     *
     * @param rows 待写出的行数据
     * @return 读回的行数据集合
     */
    private List<BigNumberRow> roundTrip(List<BigNumberRow> rows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FastExcelFactory.write(outputStream, BigNumberRow.class)
            .registerConverter(new ExcelBigNumberWriteConverter())
            .sheet("Sheet1")
            .doWrite(rows);
        byte[] content = outputStream.toByteArray();
        List<BigNumberRow> result = new ArrayList<>();
        PageReadListener<BigNumberRow> listener = new PageReadListener<>(result::addAll);
        FastExcelFactory.read(new ByteArrayInputStream(content), BigNumberRow.class, listener)
            .registerConverter(new ExcelBigNumberConverter())
            .sheet()
            .doRead();
        return result;
    }

    private static Stream<Arguments> withinMaxLengthProvider() {
        return Stream.of(Arguments.arguments(123L, 3), Arguments.arguments(999999999999999L, 15));
    }

    private static Stream<Arguments> overMaxLengthProvider() {
        return Stream.of(Arguments.arguments(10000000000000000L, 17), Arguments
            .arguments(6101234567890123789L, 19));
    }

    /**
     * 往返验证用的行模型，字段类型与生产用法一致
     */
    public static class BigNumberRow {

        @ExcelProperty("编号")
        private Long id;

        public BigNumberRow() {
        }

        public BigNumberRow(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
