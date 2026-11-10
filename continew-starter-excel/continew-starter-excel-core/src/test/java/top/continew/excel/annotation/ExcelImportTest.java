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
 * {@link ExcelImport} 注解属性测试：通过反射读取测试字段上的注解实际值进行断言。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelImportTest {

    @ExcelImport(value = "昵称", kv = "0-未知;1-男;2-女", required = true, maxLength = 100, unique = true)
    private String nickname;

    @ExcelImport("默认字段")
    private String defaultValue;

    @Test
    @DisplayName("注解作用于字段且运行时保留")
    void shouldTargetFieldAndRetainAtRuntime() {
        Target target = ExcelImport.class.getAnnotation(Target.class);
        Retention retention = ExcelImport.class.getAnnotation(Retention.class);
        assertThat(target.value()).containsExactly(ElementType.FIELD);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    @DisplayName("value 属性返回字段名称")
    void shouldReturnValue() throws NoSuchFieldException {
        ExcelImport annotation = getAnnotation("nickname");
        assertThat(annotation.value()).isEqualTo("昵称");
    }

    @Test
    @DisplayName("显式声明的 kv 与声明值一致")
    void shouldReturnExplicitKv() throws NoSuchFieldException {
        ExcelImport annotation = getAnnotation("nickname");
        assertThat(annotation.kv()).isEqualTo("0-未知;1-男;2-女");
    }

    @Test
    @DisplayName("显式声明的 maxLength 与声明值一致")
    void shouldReturnExplicitMaxLength() throws NoSuchFieldException {
        ExcelImport annotation = getAnnotation("nickname");
        assertThat(annotation.maxLength()).isEqualTo(100);
    }

    @ParameterizedTest(name = "[{index}] 属性 {0} 显式声明为 true")
    @CsvSource({"required", "unique"})
    @DisplayName("显式声明的布尔属性与声明值一致")
    void shouldReturnExplicitBooleanValues(String attribute) throws Exception {
        boolean actual =
            (Boolean) ExcelImport.class.getMethod(attribute).invoke(getAnnotation("nickname"));
        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("未声明的 maxLength 取默认值 255")
    void shouldReturnDefaultMaxLength() throws NoSuchFieldException {
        ExcelImport annotation = getAnnotation("defaultValue");
        assertThat(annotation.maxLength()).isEqualTo(255);
    }

    @Test
    @DisplayName("未声明的 kv 取默认空串")
    void shouldReturnDefaultKv() throws NoSuchFieldException {
        ExcelImport annotation = getAnnotation("defaultValue");
        assertThat(annotation.kv()).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] 属性 {0} 默认值为 false")
    @CsvSource({"required", "unique"})
    @DisplayName("未声明的布尔属性取默认值 false")
    void shouldReturnDefaultBooleanValues(String attribute) throws Exception {
        boolean actual =
            (Boolean) ExcelImport.class.getMethod(attribute).invoke(getAnnotation("defaultValue"));
        assertThat(actual).isFalse();
    }

    private ExcelImport getAnnotation(String fieldName) throws NoSuchFieldException {
        Field field = ExcelImportTest.class.getDeclaredField(fieldName);
        return field.getAnnotation(ExcelImport.class);
    }
}
