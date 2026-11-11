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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

/**
 * {@link ExcelListConverter} 单元测试：仅覆盖 {@code List<基本类型> <=> xxx,xxx} 场景。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelListConverterTest {

    private final ExcelListConverter converter = new ExcelListConverter();
    private final ExcelContentProperty contentProperty = new ExcelContentProperty();
    private final GlobalConfiguration globalConfiguration = new GlobalConfiguration();

    @Test
    @DisplayName("支持的 Java 类型为 List，Excel 类型为 STRING")
    void shouldSupportListAndStringCell() {
        assertThat(converter.supportJavaTypeKey()).isEqualTo(List.class);
        assertThat(converter.supportExcelTypeKey()).isEqualTo(CellDataTypeEnum.STRING);
    }

    @ParameterizedTest(name = "[{index}] 单元格文本 {0} 转换为 {1} 个元素")
    @ValueSource(strings = {"a", "a,b,c", "1,2,3"})
    @DisplayName("按逗号拆分单元格文本")
    void shouldSplitCellTextToList(String cellText) {
        ReadCellData<String> cellData = new ReadCellData<>(cellText);
        List converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).containsExactly(cellText.split(","));
    }

    @Test
    @DisplayName("锁定当前行为：空串单元格文本转换为含一个空串的列表")
    void shouldSplitEmptyCellTextToSingleEmptyString() {
        // hutool CharSequenceUtil.split 对空串返回含一个空串元素的列表，而非空列表
        ReadCellData<String> cellData = new ReadCellData<>("");
        List converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).containsExactly("");
    }

    @Test
    @DisplayName("多元素集合按逗号拼接为单元格文本")
    void shouldJoinMultiElementListToCellText() {
        WriteCellData<Object> cellData =
            converter.convertToExcelData(List.of("张三", "李四", "王五"), contentProperty,
                globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.STRING);
        assertThat(cellData.getStringValue()).isEqualTo("张三,李四,王五");
    }

    @Test
    @DisplayName("单元素集合拼接后不带分隔符")
    void shouldJoinSingleElementListToCellText() {
        WriteCellData<Object> cellData =
            converter.convertToExcelData(List.of("single"), contentProperty, globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.STRING);
        assertThat(cellData.getStringValue()).isEqualTo("single");
    }

    @Test
    @DisplayName("空集合转换为空文本")
    void shouldJoinEmptyListToEmptyText() {
        WriteCellData<Object> cellData =
            converter.convertToExcelData(List.of(), contentProperty, globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.STRING);
        assertThat(cellData.getStringValue()).isEmpty();
    }
}
