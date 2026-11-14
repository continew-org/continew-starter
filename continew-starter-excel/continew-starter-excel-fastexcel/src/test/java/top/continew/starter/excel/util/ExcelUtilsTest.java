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

package top.continew.starter.excel.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.read.listener.PageReadListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.core.exception.BaseException;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link ExcelUtils} 导出测试：基于内存响应写出后再读回，验证响应头与数据往返一致。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelUtilsTest {

    private static final String CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8";

    @Test
    @DisplayName("export 写出附件响应头与全部数据行")
    void shouldExportRowsWithAttachmentHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<ExportRow> rows = List.of(new ExportRow("张三", 18), new ExportRow("李四", 25));

        ExcelUtils.export(rows, "用户", ExportRow.class, response);

        assertThat(response.getHeader("Content-disposition"))
            .matches("attachment;filename=.*_\\d{14}\\.xlsx");
        assertThat(response.getContentType()).isEqualTo(CONTENT_TYPE);
        List<ExportRow> readRows = readRows(response.getContentAsByteArray());
        assertThat(readRows).hasSize(2);
        assertThat(readRows.get(0).getNickname()).isEqualTo("张三");
        assertThat(readRows.get(0).getAge()).isEqualTo(18);
        assertThat(readRows.get(1).getNickname()).isEqualTo("李四");
        assertThat(readRows.get(1).getAge()).isEqualTo(25);
    }

    @Test
    @DisplayName("export 支持自定义工作表与排除字段")
    void shouldExportWithExcludeColumns() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<ExportRow> rows = List.of(new ExportRow("张三", 18));

        ExcelUtils.export(rows, "用户", "自定义Sheet", Set.of("age"), ExportRow.class, response);

        List<ExportRow> readRows = readRows(response.getContentAsByteArray());
        assertThat(readRows).hasSize(1);
        assertThat(readRows.get(0).getNickname()).isEqualTo("张三");
        assertThat(readRows.get(0).getAge()).as("排除列不导出，读回为 null").isNull();
    }

    @Test
    @DisplayName("export 走真实入口时超长 Long 经大数值写转换器转为文本，读回不截断")
    void shouldPreserveLargeLongThroughExportEntry() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        long largeId = 6101234567890123789L;
        List<ExportRow> rows = List.of(new ExportRow("张三", 18, largeId));

        ExcelUtils.export(rows, "用户", ExportRow.class, response);

        List<ExportRow> readRows = readRows(response.getContentAsByteArray());
        assertThat(readRows).hasSize(1);
        assertThat(readRows.get(0).getId()).as("超过 15 位的 Long 经真实导出入口不丢精度")
            .isEqualTo(largeId);
    }

    @Test
    @DisplayName("锁定当前行为：导出异常时抛出 BaseException 并重置响应")
    void shouldThrowBaseExceptionWhenExportFails() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("模拟输出流故障"));

        List<ExportRow> rows = List.of(new ExportRow("张三", 18));

        assertThatThrownBy(() -> ExcelUtils.export(rows, "用户", ExportRow.class, response))
            .isInstanceOf(BaseException.class)
            .hasMessage("导出 Excel 出现错误");
    }

    /**
     * 将导出字节读回为行对象，读端不注册任何自定义转换器，保持被测库默认行为。
     *
     * @param content 导出得到的 xlsx 字节
     * @return 读回的行对象集合
     */
    private List<ExportRow> readRows(byte[] content) {
        List<ExportRow> result = new ArrayList<>();
        PageReadListener<ExportRow> listener = new PageReadListener<>(result::addAll);
        FastExcelFactory.read(new ByteArrayInputStream(content), ExportRow.class, listener).sheet()
            .doRead();
        return result;
    }

    public static class ExportRow {

        @ExcelProperty("昵称")
        private String nickname;

        @ExcelProperty("年龄")
        private Integer age;

        @ExcelProperty("ID")
        private Long id;

        public ExportRow() {
        }

        public ExportRow(String nickname, Integer age) {
            this.nickname = nickname;
            this.age = age;
        }

        public ExportRow(String nickname, Integer age, Long id) {
            this.nickname = nickname;
            this.age = age;
            this.id = id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
