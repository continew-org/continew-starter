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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.enums.BaseEnum;

import java.lang.reflect.Field;

/**
 * {@link ExcelBaseEnumConverter} 单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelBaseEnumConverterTest {

    private final ExcelBaseEnumConverter converter = new ExcelBaseEnumConverter();
    private final ExcelContentProperty contentProperty = new ExcelContentProperty();
    private final GlobalConfiguration globalConfiguration = new GlobalConfiguration();

    @BeforeEach
    void setUpField() throws NoSuchFieldException {
        Field genderField = EnumFixture.class.getDeclaredField("gender");
        contentProperty.setField(genderField);
    }

    @Test
    @DisplayName("支持的 Java 类型为 BaseEnum，Excel 类型为 STRING")
    void shouldSupportBaseEnumAndStringCell() {
        assertThat(converter.supportJavaTypeKey()).isEqualTo(BaseEnum.class);
        assertThat(converter.supportExcelTypeKey()).isEqualTo(CellDataTypeEnum.STRING);
    }

    @ParameterizedTest(name = "[{index}] 描述 {0} 转换为对应枚举")
    @EnumSource(value = Gender.class, names = {"MALE", "FEMALE"})
    @DisplayName("按描述转换为枚举")
    void shouldConvertDescriptionToEnum(Gender gender) {
        ReadCellData<?> cellData = new ReadCellData<>(gender.getDescription());
        BaseEnum<?> converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).isSameAs(gender);
    }

    @Test
    @DisplayName("未匹配的描述转换为 null")
    void shouldConvertUnknownDescriptionToNull() {
        ReadCellData<?> cellData = new ReadCellData<>("不存在");
        BaseEnum<?> converted =
            converter.convertToJavaData(cellData, contentProperty, globalConfiguration);
        assertThat(converted).isNull();
    }

    @ParameterizedTest(name = "[{index}] 枚举 {0} 转换为其描述")
    @EnumSource(value = Gender.class, names = {"MALE", "FEMALE"})
    @DisplayName("按枚举转换为描述")
    void shouldConvertEnumToDescription(Gender gender) {
        WriteCellData<String> cellData =
            converter.convertToExcelData(gender, contentProperty, globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.STRING);
        assertThat(cellData.getStringValue()).isEqualTo(gender.getDescription());
    }

    @Test
    @DisplayName("null 枚举转换为空串")
    void shouldConvertNullEnumToEmptyString() {
        WriteCellData<String> cellData =
            converter.convertToExcelData(null, contentProperty, globalConfiguration);
        assertThat(cellData.getType()).isEqualTo(CellDataTypeEnum.STRING);
        assertThat(cellData.getStringValue()).isEqualTo(StringConstants.EMPTY);
    }

    enum Gender implements BaseEnum<Integer> {

        UNKNOWN(0, "未知"), MALE(1, "男"), FEMALE(2, "女");

        private final Integer value;
        private final String description;

        Gender(Integer value, String description) {
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

    static class EnumFixture {

        private Gender gender;
    }
}
