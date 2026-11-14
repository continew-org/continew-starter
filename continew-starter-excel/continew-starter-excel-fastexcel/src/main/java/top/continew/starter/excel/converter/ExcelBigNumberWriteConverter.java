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

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;

/**
 * Excel 大数值写端转换器
 * <p>
 * 写端按 {@code (Java 类型, 目标单元格类型)} 查找转换器，普通写路径的目标单元格类型为 null，
 * {@link ExcelBigNumberConverter} 按 {@code (Long, STRING)} 注册无法命中该查找，
 * 内置 LongNumberConverter 会让超过 15 位的大数值落入 NUMBER 单元格而被 xlsx double 精度截断。
 * 本转换器以 {@code null} 作为支持的单元格类型占用 {@code (Long, null)} 槽位，
 * 将长度分流逻辑接入真实写路径：超过 15 位走 STRING 单元格，其余走 NUMBER 单元格
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class ExcelBigNumberWriteConverter implements Converter<Long> {

    private final ExcelBigNumberConverter delegate = new ExcelBigNumberConverter();

    @Override
    public Class<Long> supportJavaTypeKey() {
        return Long.class;
    }

    /**
     * 返回 null 以占用写端 {@code (Long, null)} 转换器槽位
     */
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return null;
    }

    /**
     * 转换为 Excel 数据（写入 Excel），分流逻辑委托给 {@link ExcelBigNumberConverter}
     */
    @Override
    public WriteCellData<Object> convertToExcelData(Long value,
        ExcelContentProperty contentProperty,
        GlobalConfiguration globalConfiguration) {
        return delegate.convertToExcelData(value, contentProperty, globalConfiguration);
    }
}
