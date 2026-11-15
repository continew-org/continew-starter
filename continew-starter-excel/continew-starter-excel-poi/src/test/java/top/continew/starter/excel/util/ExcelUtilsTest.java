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

import cn.hutool.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import top.continew.excel.annotation.ExcelExport;
import top.continew.excel.annotation.ExcelImport;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ExcelUtils} 导入导出单元测试：真实 workbook 写出后读回逐单元格断言。
 *
 * <p>导出数据行不得包含空串单元格：poi 版 {@code isNumeric("")} 恒真，空串会进入
 * {@code Double.parseDouble} 抛 NumberFormatException（既有缺陷，由行为锁定用例固化），
 * 因此导入 fixture 用空格单元格表达「空值」。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelUtilsTest {

    /**
     * 导出模板测试实体
     */
    public static class UserEntity {

        @ExcelExport(value = "姓名", sort = 1, example = "张三")
        private String nickname;

        @ExcelExport(value = "年龄", sort = 2, example = "18")
        private Integer age;

        @ExcelExport(value = "性别", sort = 3, example = "男", kv = "1-男;2-女")
        private Integer gender;

        public UserEntity() {
        }

        public UserEntity(String nickname, Integer age, Integer gender) {
            this.nickname = nickname;
            this.age = age;
            this.gender = gender;
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

        public Integer getGender() {
            return gender;
        }

        public void setGender(Integer gender) {
            this.gender = gender;
        }
    }

    /**
     * 导入测试实体
     */
    public static class ImportEntity {

        @ExcelImport(value = "姓名", required = true, maxLength = 10)
        private String nickname;

        @ExcelImport(value = "年龄", unique = true)
        private Integer age;

        @ExcelImport(value = "性别", kv = "1-男;2-女")
        private Integer gender;

        @ExcelImport(value = "薪资")
        private BigDecimal salary;

        public String getNickname() {
            return nickname;
        }

        public Integer getAge() {
            return age;
        }

        public Integer getGender() {
            return gender;
        }

        public BigDecimal getSalary() {
            return salary;
        }
    }

    /**
     * 无注解导出测试实体：表头按字段名输出
     */
    public static class PlainEntity {

        private String nickname;

        private Integer age;

        public PlainEntity(String nickname, Integer age) {
            this.nickname = nickname;
            this.age = age;
        }

        public String getNickname() {
            return nickname;
        }

        public Integer getAge() {
            return age;
        }
    }

    /**
     * 行号/行数据/行提示承载实体
     */
    public static class RowAwareEntity {

        @ExcelImport(value = "姓名", required = true)
        private String nickname;

        @ExcelImport(value = "年龄", unique = true)
        private Integer age;

        private Integer rowNum;

        private String rowData;

        private String rowTips;

        public Integer getRowNum() {
            return rowNum;
        }

        public String getRowData() {
            return rowData;
        }

        public String getRowTips() {
            return rowTips;
        }
    }

    private static final String CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 构建导入用 xlsx 字节：导出与读回共用本工具形成真实往返。
     *
     * @param rows 数据行
     * @return xlsx 字节
     */
    private static byte[] exportToBytes(List<List<Object>> rows) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.export(response, "导入模板", rows);
        return response.getContentAsByteArray();
    }

    /**
     * 将导出字节落盘为本地 xlsx 文件（导出→读回真实往返）。
     *
     * @param content  导出得到的 xlsx 字节
     * @param fileName 文件名
     * @return 落盘文件
     */
    private File toFile(byte[] content, String fileName) throws IOException {
        File file = new File(tempExportDir(), fileName);
        java.nio.file.Files.write(file.toPath(), content);
        return file;
    }

    /**
     * 实体列表导出应写响应头并逐行写出数据，读回字段与表头一致。
     */
    @Test
    void shouldExportEntityListAndRoundTrip() throws IOException {
        File dir = tempExportDir();
        File file = ExcelUtils.exportFile(dir.getPath() + "/", "用户表", List
            .of(new UserEntity("张三", 18, 1), new UserEntity("李四", 25, 2)));

        assertThat(file).isNotNull().hasName("用户表.xlsx");

        JSONArray array = ExcelUtils.readFile(file);
        assertThat(array).hasSize(2);
        assertThat(array.getJSONObject(0).getStr("姓名")).isEqualTo("张三");
        assertThat(array.getJSONObject(0).getStr("年龄")).isEqualTo("18");
        assertThat(array.getJSONObject(1).getStr("姓名")).isEqualTo("李四");
        assertThat(array.getJSONObject(1).getStr("年龄")).isEqualTo("25");
    }

    /**
     * {@code @ExcelExport} kv 映射导出应把字段值翻译为映射显示值写单元格（1→男）。
     */
    @Test
    void shouldExportWithKvMapping() throws IOException {
        File dir = tempExportDir();
        File file = ExcelUtils.exportFile(dir.getPath() + "/", "性别表", List
            .of(new UserEntity("张三", 18, 1), new UserEntity("李四", 25, 2)));

        JSONArray array = ExcelUtils.readFile(file);
        assertThat(array.getJSONObject(0).getStr("性别")).isEqualTo("男");
        assertThat(array.getJSONObject(1).getStr("性别")).isEqualTo("女");
    }

    /**
     * 无注解实体导出应按字段名输出表头（无表头名映射）。
     */
    @Test
    void shouldExportPlainEntityWithFieldNameHeader() throws IOException {
        File dir = tempExportDir();
        File file = ExcelUtils.exportFile(dir.getPath() + "/", "无注解", Arrays
            .asList(new PlainEntity("张三", 18), new PlainEntity("李四", 20)));

        JSONArray array = ExcelUtils.readFile(file);
        assertThat(array).hasSize(2);
        assertThat(array.getJSONObject(0).getStr("nickname")).isEqualTo("张三");
        assertThat(array.getJSONObject(0).getStr("age")).isEqualTo("18");
        assertThat(array.getJSONObject(1).getStr("nickname")).isEqualTo("李四");
    }

    /**
     * 数字串按长度分流写单元格：短于 8 位的数字串写数值单元格，8 位及以上的数字串
     * 写字符串单元格保留全精度（与 fastexcel 版「Long 超 15 位转文本」的分流策略不同）。
     */
    @Test
    void shouldSplitNumericStringByLength() throws IOException {
        List<List<Object>> rows = List.of(Arrays.asList("短数字", "超长ID"), Arrays.asList("18",
            "6101234567890123789"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.export(response, "分流", rows);

        assertThat(response.getHeader("Content-Disposition")).contains(".xlsx");
        assertThat(response.getContentType()).startsWith(CONTENT_TYPE);

        JSONArray array = ExcelUtils.readFile(toFile(response.getContentAsByteArray(),
            "roundtrip.xlsx"));
        assertThat(array.getJSONObject(0).getStr("短数字")).isEqualTo("18");
        assertThat(array.getJSONObject(0).getStr("超长ID")).isEqualTo("6101234567890123789");
    }

    /**
     * Long 对象一律写数值单元格：超过 15 位的大数经 double 精度截断且读回为科学计数法
     * 字符串（fastexcel 版走 ExcelBigNumberWriteConverter 转文本保精度，两版行为不一致，
     * 已记录 PR 报告，本用例锁定 poi 版现状）。
     */
    @Test
    void shouldWriteLargeLongObjectWithDoublePrecisionLoss() throws IOException {
        long largeId = 6101234567890123789L;
        List<List<Object>> rows = List.of(List.of("ID"), List.of(largeId));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.export(response, "大数", rows);

        JSONArray array = ExcelUtils.readFile(toFile(response.getContentAsByteArray(),
            "large-long.xlsx"));
        assertThat(array.getJSONObject(0).getStr("ID"))
            .isEqualTo(Double.toString((double) largeId));
    }

    /**
     * 空串单元格导出必抛 NumberFormatException：{@code isNumeric("")} 恒真导致空串进入
     * {@code Double.parseDouble}（既有缺陷，锁定现状；含空单元格的模板/实体导出均不可用）。
     */
    @Test
    void shouldThrowNumberFormatWhenExportEmptyStringCell() {
        List<List<Object>> rows = List.of(List.of("列A"), List.of(""));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> ExcelUtils.export(response, "空串", rows))
            .isInstanceOf(NumberFormatException.class);
    }

    /**
     * Long/Double/Boolean/BigDecimal 类型单元格写入后读回语义（BigDecimal 默认 3 位小数）。
     */
    @Test
    void shouldWriteTypedCellValues() throws IOException {
        List<List<Object>> rows = List.of(Arrays.asList("Long", "Double", "Boolean", "BigDecimal",
            "String"), Arrays.asList(99L, 1.5D, Boolean.TRUE, new BigDecimal("3.14159"), "文本"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.export(response, "类型", rows);

        JSONArray array = ExcelUtils.readFile(toFile(response.getContentAsByteArray(),
            "typed.xlsx"));
        assertThat(array.getJSONObject(0).getStr("Long")).isEqualTo("99");
        assertThat(array.getJSONObject(0).getStr("Double")).isEqualTo("1.5");
        assertThat(array.getJSONObject(0).getStr("Boolean")).isEqualTo("true");
        assertThat(array.getJSONObject(0).getStr("BigDecimal")).isEqualTo("3.142");
        assertThat(array.getJSONObject(0).getStr("String")).isEqualTo("文本");
    }

    /**
     * 本地文件导出（exportFile(File, List)）应落盘可读回。
     */
    @Test
    void shouldExportToFileAndReadBack() throws IOException {
        File dir = tempExportDir();
        List<List<Object>> sheetData = List.of(Arrays.asList("列A", "列B"), Arrays.asList("a1",
            "b1"), Arrays.asList("a2", "b2"));
        File file = new File(dir, "plain.xlsx");
        ExcelUtils.exportFile(file, sheetData);

        assertThat(file).exists().isNotEmpty();

        Map<String, JSONArray> many = ExcelUtils.readFileManySheet(file);
        assertThat(many.get("plain.xlsx").getJSONObject(0).getStr("列A")).isEqualTo("a1");
    }

    /**
     * 导出模板应含示例行（各字段 example 值）；不含示例时仅表头行（读回为单行空值）。
     */
    @Test
    void shouldExportTemplateWithAndWithoutExample() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.exportTemplate(response, "用户模板", UserEntity.class, true);
        assertThat(response.getContentAsByteArray()).isNotEmpty();

        File withExample = toFile(response.getContentAsByteArray(), "with-example.xlsx");
        JSONArray array = ExcelUtils.readFile(withExample);
        assertThat(array).hasSize(1);
        assertThat(array.getJSONObject(0).getStr("姓名")).isEqualTo("张三");
        assertThat(array.getJSONObject(0).getStr("年龄")).isEqualTo("18");
        assertThat(array.getJSONObject(0).getStr("性别")).isEqualTo("男");

        MockHttpServletResponse plainResponse = new MockHttpServletResponse();
        ExcelUtils.exportTemplate(plainResponse, "用户模板", UserEntity.class, false);
        File withoutExample = toFile(plainResponse.getContentAsByteArray(), "without-example.xlsx");
        JSONArray plainArray = ExcelUtils.readFile(withoutExample);
        assertThat(plainArray).hasSize(1);
        assertThat(plainArray.getJSONObject(0).getStr("姓名")).isEmpty();
        assertThat(plainArray.getJSONObject(0).getStr("年龄")).isEmpty();
    }

    /**
     * 导出模板按 sort 升序排列表头（1 姓名 / 2 年龄 / 3 性别）。
     */
    @Test
    void shouldSortTemplateHeadersBySortValue() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.exportTemplate(response, "排序模板", UserEntity.class, false);

        JSONArray array = ExcelUtils.readFile(toFile(response.getContentAsByteArray(),
            "sorted.xlsx"));
        assertThat((java.util.Map<String, Object>) (Object) array.getJSONObject(0))
            .containsKey("姓名")
            .containsKey("年龄")
            .containsKey("性别");
    }

    /**
     * 空导出应写出单列表头「导出无数据」。
     */
    @Test
    void shouldExportEmptyWithPlaceholderHeader() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.exportEmpty(response, "空数据");

        JSONArray array = ExcelUtils.readFile(toFile(response.getContentAsByteArray(),
            "empty.xlsx"));
        assertThat(array).hasSize(1);
        assertThat(array.getJSONObject(0).getStr("导出无数据")).isEmpty();
    }

    /**
     * 多 sheet 导出应按 sheet 名分组写出，读回按名取数。
     */
    @Test
    void shouldExportManySheet() throws IOException {
        Map<String, List<List<Object>>> sheetMap = new LinkedHashMap<>();
        sheetMap.put("第一页", List.of(Arrays.asList("A"), Arrays.asList("a1")));
        sheetMap.put("第二页", List.of(Arrays.asList("B"), Arrays.asList("b1")));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.exportManySheet(response, "多页", sheetMap);

        File file = toFile(response.getContentAsByteArray(), "multi.xlsx");
        Map<String, JSONArray> many = ExcelUtils.readFileManySheet(file);
        assertThat(many).containsKeys("第一页", "第二页");
        assertThat(many.get("第一页").getJSONObject(0).getStr("A")).isEqualTo("a1");
        assertThat(many.get("第二页").getJSONObject(0).getStr("B")).isEqualTo("b1");
    }

    /**
     * 导入应按 @ExcelImport 列名映射赋值：kv 列填显示值（男）映射回键（1），数值列
     * 经数值单元格往返后转 Integer，BigDecimal 列正常转换。
     */
    @Test
    void shouldImportEntityWithKvAndTypeConversion()
        throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别", "薪资"), Arrays
            .asList("张三", "18", "男", "3.5"));
        List<ImportEntity> list = ExcelUtils.readFile(new File(writeImportFile(rows)),
            ImportEntity.class);

        assertThat(list).hasSize(1);
        ImportEntity entity = list.get(0);
        assertThat(entity.getNickname()).isEqualTo("张三");
        assertThat(entity.getAge()).isEqualTo(18);
        assertThat(entity.getGender()).isEqualTo(1);
        assertThat(entity.getSalary()).isEqualByComparingTo("3.5");
    }

    /**
     * 必填列为空、kv 不匹配应记录错误信息且字段不赋值。
     */
    @Test
    void shouldCollectImportErrorsIntoRowTips() throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别"), Arrays.asList(" ",
            "18", "3"));
        List<ImportEntity> list = ExcelUtils.readFile(new File(writeImportFile(rows)),
            ImportEntity.class);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getNickname()).isNull();
        assertThat(list.get(0).getAge()).isEqualTo(18);
        assertThat(list.get(0).getGender()).isNull();
    }

    /**
     * rowTips 字段应承载该行全部错误信息（必填缺失 + kv 不匹配）。
     */
    @Test
    void shouldFillRowTipsWithErrors() throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别"), Arrays.asList(" ",
            "18", "3"));
        List<RowAwareEntity> list = ExcelUtils.readFile(new File(writeImportFile(rows)),
            RowAwareEntity.class);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getRowTips()).contains("不能为空");
    }

    /**
     * unique 字段重复不应影响行数据本身（错误信息仅在带 rowTips 字段的实体上可见）。
     */
    @Test
    void shouldDetectDuplicateUniqueValue() throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别"), Arrays.asList("张三",
            "18", "男"), Arrays.asList("李四", "18", "女"));
        List<ImportEntity> list = ExcelUtils.readFile(new File(writeImportFile(rows)),
            ImportEntity.class);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getNickname()).isEqualTo("张三");
        assertThat(list.get(1).getNickname()).isEqualTo("李四");
    }

    /**
     * unique 字段重复应在带 rowTips 字段的实体上记录唯一性校验失败信息（checkUniqueness）。
     */
    @Test
    void shouldRecordDuplicateUniqueErrorInRowTips()
        throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别"), Arrays.asList("张三",
            "18", "男"), Arrays.asList("李四", "18", "女"));
        List<RowAwareEntity> list = ExcelUtils.readFile(new File(writeImportFile(rows)),
            RowAwareEntity.class);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getRowTips()).isEmpty();
        assertThat(list.get(1).getRowTips()).contains("数据唯一性校验失败");
    }

    /**
     * 整数格式错误该字段不赋值，原始行 JSON 仍承载（rowData）。
     */
    @Test
    void shouldRecordFormatErrorForInvalidInteger()
        throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别"), Arrays.asList("张三",
            "abc", "男"));
        List<RowAwareEntity> list = ExcelUtils.readFile(new File(writeImportFile(rows)),
            RowAwareEntity.class);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getRowData()).isNotNull();
        assertThat(list.get(0).getRowData()).contains("abc");
    }

    /**
     * multipart 上传读取应与文件读取等价。
     */
    @Test
    void shouldReadMultipartFileSameAsFile() throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别"), Arrays.asList("张三",
            "18", "男"));
        MockMultipartFile mFile = new MockMultipartFile("file", "导入.xlsx", null, java.nio.file.Files
            .readAllBytes(new File(writeImportFile(rows)).toPath()));

        JSONArray array = ExcelUtils.readMultipartFile(mFile);
        assertThat(array).hasSize(1);
        assertThat(array.getJSONObject(0).getStr("姓名")).isEqualTo("张三");

        List<ImportEntity> list = ExcelUtils.readMultipartFile(mFile, ImportEntity.class);
        assertThat(list.get(0).getNickname()).isEqualTo("张三");
        assertThat(list.get(0).getAge()).isEqualTo(18);
    }

    /**
     * 只有表头一行时应返回单行空值数据（buildSingleHeaderRow 行为）。
     */
    @Test
    void shouldReturnSingleHeaderRowWhenOnlyHeader() throws IOException {
        List<List<Object>> rows = List.of(List.of("姓名", "年龄"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.export(response, "仅表头", rows);

        JSONArray array = ExcelUtils.readFile(toFile(response.getContentAsByteArray(),
            "header-only.xlsx"));
        assertThat(array).hasSize(1);
        assertThat(array.getJSONObject(0).getStr("姓名")).isEmpty();
        assertThat(array.getJSONObject(0).getStr("年龄")).isEmpty();
    }

    /**
     * 空白单元格行读回为空值整行跳过（buildRowData 空行返回 null）。
     */
    @Test
    void shouldSkipBlankRowsWhenReading() throws IOException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄"), Arrays.asList("张三", "18"),
            Arrays.asList(" ", " "), Arrays.asList("李四", "20"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.export(response, "空行", rows);

        JSONArray array = ExcelUtils.readFile(toFile(response.getContentAsByteArray(),
            "blank-rows.xlsx"));
        assertThat(array).hasSize(2);
        assertThat(array.getJSONObject(0).getStr("姓名")).isEqualTo("张三");
        assertThat(array.getJSONObject(1).getStr("姓名")).isEqualTo("李四");
    }

    /**
     * 不存在的文件与非法后缀读取应返回空数组（getWorkbook 返回 null）。
     */
    @Test
    void shouldReturnEmptyArrayWhenFileMissingOrUnsupported() throws IOException {
        File missing = new File(tempExportDir(), "missing.xlsx");
        assertThat(ExcelUtils.readFile(missing)).isEmpty();

        File unsupported = new File(tempExportDir(), "unsupported.txt");
        java.nio.file.Files.write(unsupported.toPath(), "not excel".getBytes());
        assertThat(ExcelUtils.readFile(unsupported)).isEmpty();
    }

    /**
     * rowNum/rowData 特殊字段应分别承载行号（物理行号，含表头偏移）与原始行 JSON。
     */
    @Test
    void shouldFillRowNumAndRowDataForImportRow() throws IOException, ReflectiveOperationException {
        List<List<Object>> rows = List.of(Arrays.asList("姓名", "年龄", "性别"), Arrays.asList("张三",
            "18", "男"));
        List<RowAwareEntity> list = ExcelUtils.readFile(new File(writeImportFile(rows)),
            RowAwareEntity.class);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getRowNum()).isEqualTo(2);
        assertThat(list.get(0).getRowData()).contains("张三");
    }

    @TempDir
    Path tempDir;

    private File tempExportDir() {
        File dir = tempDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 将导入数据写出为本地 xlsx 文件并返回路径（导出→读回真实往返）。
     *
     * @param rows 数据行
     * @return 文件路径
     */
    private String writeImportFile(List<List<Object>> rows) throws IOException {
        File file = new File(tempExportDir(), "import-" + System.nanoTime() + ".xlsx");
        java.nio.file.Files.write(file.toPath(), exportToBytes(rows));
        return file.getPath();
    }
}
