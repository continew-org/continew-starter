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

import cn.hutool.core.util.NumberUtil;
import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;

/**
 * Excel 大数值写端转换器
 * <p>
 * Excel 中超过 15 位的数值从第 16 位起一律归零，超出长度的 {@code Long} 写入 NUMBER 单元格会被 xlsx double 精度截断，
 * 因此写端按长度分流：超过 15 位走 STRING 单元格，其余走 NUMBER 单元格。
 * </p>
 * <p>
 * 写端按 {@code (Java 类型, 目标单元格类型)} 查找转换器，普通写路径的目标单元格类型为 null，内置 LongNumberConverter
 * 按 {@code (Long, NUMBER)} 注册无法命中该查找。本转换器以 {@code null} 作为支持的单元格类型占用 {@code (Long, null)}
 * 槽位，使分流逻辑接入真实写路径。该查找行为属 fastexcel 实现细节（未在文档承诺），上游升级后需通过 ExcelUtils
 * 导出往返用例回归验证。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class ExcelBigNumberWriteConverter implements Converter<Long> {

    /**
     * Excel 输入数值长度限制
     */
    private static final int MAX_LENGTH = 15;

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

    @Override
    public WriteCellData<Object> convertToExcelData(Long value,
        ExcelContentProperty contentProperty,
        GlobalConfiguration globalConfiguration) {
        if (value != null) {
            String str = Long.toString(value);
            if (str.length() > MAX_LENGTH) {
                return new WriteCellData<>(str);
            }
        }
        WriteCellData<Object> writeCellData = new WriteCellData<>(NumberUtil.toBigDecimal(value));
        writeCellData.setType(CellDataTypeEnum.NUMBER);
        return writeCellData;
    }
}
