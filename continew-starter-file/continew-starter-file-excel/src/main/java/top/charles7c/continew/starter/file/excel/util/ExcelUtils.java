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

package top.charles7c.continew.starter.file.excel.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.charles7c.continew.starter.core.exception.BaseException;
import top.charles7c.continew.starter.file.excel.converter.ExcelBigNumberConverter;

import java.util.Date;
import java.util.List;

/**
 * Excel 工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class ExcelUtils {

    private static final Logger log = LoggerFactory.getLogger(ExcelUtils.class);

    private ExcelUtils() {
    }

    /**
     * 导出
     *
     * @param list     导出数据集合
     * @param fileName 文件名
     * @param clazz    导出数据类型
     * @param response 响应对象
     */
    public static <T> void export(List<T> list, String fileName, Class<T> clazz, HttpServletResponse response) {
        export(list, fileName, "Sheet1", clazz, response);
    }

    /**
     * 导出
     *
     * @param list      导出数据集合
     * @param fileName  文件名
     * @param sheetName 工作表名称
     * @param clazz     导出数据类型
     * @param response  响应对象
     */
    public static <T> void export(List<T> list,
                                  String fileName,
                                  String sheetName,
                                  Class<T> clazz,
                                  HttpServletResponse response) {
        try {
            String exportFileName = URLUtil.encode("%s_%s.xlsx".formatted(fileName, DateUtil
                .format(new Date(), DatePattern.PURE_DATETIME_PATTERN)));
            response.setHeader("Content-disposition", "attachment;filename=" + exportFileName);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            EasyExcelFactory.write(response.getOutputStream(), clazz)
                .autoCloseStream(false)
                // 自动适配宽度
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                // 自动转换大数值
                .registerConverter(new ExcelBigNumberConverter())
                .sheet(sheetName)
                .doWrite(list);
        } catch (Exception e) {
            log.error("Export excel occurred an error: {}. fileName: {}.", e.getMessage(), fileName, e);
            throw new BaseException("导出 Excel 出现错误");
        }
    }
}
