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

package top.continew.excel.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * {@link ExcelExport} 注解属性测试：通过反射读取测试字段上的注解实际值进行断言。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelExportTest {

    @ExcelExport(value = "昵称", sort = 2, kv = "0-未知;1-男;2-女", example = "张三")
    private String nickname;

    @ExcelExport("默认字段")
    private String defaultValue;

    @Test
    @DisplayName("注解作用于字段且运行时保留")
    void shouldTargetFieldAndRetainAtRuntime() {
        Target target = ExcelExport.class.getAnnotation(Target.class);
        Retention retention = ExcelExport.class.getAnnotation(Retention.class);
        assertThat(target.value()).containsExactly(ElementType.FIELD);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    @DisplayName("value 属性返回字段名称")
    void shouldReturnValue() throws NoSuchFieldException {
        ExcelExport annotation = getAnnotation("nickname");
        assertThat(annotation.value()).isEqualTo("昵称");
    }

    @Test
    @DisplayName("显式声明的 sort 与声明值一致")
    void shouldReturnExplicitSort() throws NoSuchFieldException {
        ExcelExport annotation = getAnnotation("nickname");
        assertThat(annotation.sort()).isEqualTo(2);
    }

    @ParameterizedTest(name = "[{index}] 属性 {0} 实际值 {1}")
    @CsvSource({"kv, 0-未知;1-男;2-女", "example, 张三"})
    @DisplayName("显式声明的字符串属性与声明值一致")
    void shouldReturnExplicitStringValues(String attribute, String expected) throws Exception {
        Object actual = ExcelExport.class.getMethod(attribute).invoke(getAnnotation("nickname"));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("未声明的 sort 取默认值 0")
    void shouldReturnDefaultSort() throws NoSuchFieldException {
        ExcelExport annotation = getAnnotation("defaultValue");
        assertThat(annotation.sort()).isZero();
    }

    @ParameterizedTest(name = "[{index}] 属性 {0} 默认值为空串")
    @CsvSource({"kv", "example"})
    @DisplayName("未声明的字符串属性取默认空串")
    void shouldReturnDefaultEmptyStrings(String attribute) throws Exception {
        Object actual =
            ExcelExport.class.getMethod(attribute).invoke(getAnnotation("defaultValue"));
        assertThat(actual).isEqualTo("");
    }

    private ExcelExport getAnnotation(String fieldName) throws NoSuchFieldException {
        Field field = ExcelExportTest.class.getDeclaredField(fieldName);
        return field.getAnnotation(ExcelExport.class);
    }
}
