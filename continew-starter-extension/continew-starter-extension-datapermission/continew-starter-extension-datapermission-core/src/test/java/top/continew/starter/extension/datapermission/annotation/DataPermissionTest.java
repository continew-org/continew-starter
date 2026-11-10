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

package top.continew.starter.extension.datapermission.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

/**
 * {@link DataPermission} 注解元数据与属性默认值测试
 *
 * <p>
 * 通过反射固化注解的使用位置、保留策略与各属性默认值，防止列名约定被无意改动。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DataPermissionTest {

    @Test
    @DisplayName("仅可用于方法且运行期保留")
    void shouldTargetMethodsAndRetainAtRuntime() {
        Target target = DataPermission.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.METHOD);
        Retention retention = DataPermission.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(DataPermission.class.isAnnotationPresent(Documented.class)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] 属性 {0} 默认值为 \"{1}\"")
    @MethodSource("defaultValueProvider")
    void shouldDeclareExpectedDefaultValue(String attribute, String expected) throws Exception {
        DataPermission annotation = DataPermissionTest.class
            .getDeclaredMethod("annotatedWithDefaults").getAnnotation(DataPermission.class);
        assertThat(DataPermission.class.getMethod(attribute).invoke(annotation))
            .isEqualTo(expected);
    }

    private static Stream<Arguments> defaultValueProvider() {
        return Stream.of(arguments("tableAlias", ""), arguments("id", "id"),
            arguments("deptId", "dept_id"),
            arguments("userId", "create_user"), arguments("roleId", "role_id"),
            arguments("deptTableAlias", "sys_dept"),
            arguments("roleDeptTableAlias", "sys_role_dept"));
    }

    @Test
    @DisplayName("标注在方法上后各属性按声明值读取，未声明属性保持默认")
    void shouldExposeDeclaredValuesOnAnnotatedMethod() throws Exception {
        DataPermission annotation = DataPermissionTest.class
            .getDeclaredMethod("annotatedWithCustomValues").getAnnotation(DataPermission.class);
        assertThat(annotation.tableAlias()).isEqualTo("u");
        assertThat(annotation.deptId()).isEqualTo("d_id");
        assertThat(annotation.userId()).isEqualTo("u_id");
        assertThat(annotation.deptTableAlias()).isEqualTo("dept");
        assertThat(annotation.id()).isEqualTo("id");
        assertThat(annotation.roleId()).isEqualTo("role_id");
        assertThat(annotation.roleDeptTableAlias()).isEqualTo("sys_role_dept");
    }

    @DataPermission
    void annotatedWithDefaults() {
        // 注解载体，供反射读取各属性默认值
    }

    @DataPermission(tableAlias = "u", deptId = "d_id", userId = "u_id", deptTableAlias = "dept")
    void annotatedWithCustomValues() {
        // 注解载体，供反射读取方法上声明的属性值
    }
}
