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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BusinessException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;

/**
 * 反射工具类测试
 *
 * <p>
 * 锁定非静态字段收集的字段顺序契约（子类字段在前、父类字段在后、同名字段并存）
 * 与方法引用创建的失败包装行为。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ReflectUtilsTest {

    /**
     * 静态字段宿主：验证静态字段被过滤
     */
    private static class ParentBean {

        private static String staticField;

        private String parentField;

        public String getParentField() {
            return parentField;
        }
    }

    /**
     * 与父类同名字段的子类：验证同名字段并存且子类在前
     */
    private static class ChildBean extends ParentBean {

        private String parentField;

        private String childField;

        public String getChildField() {
            return childField;
        }
    }

    @Test
    @DisplayName("getNonStaticFieldsName 过滤静态字段并保留实例字段")
    void shouldCollectNonStaticFieldNames() {
        List<String> names = ReflectUtils.getNonStaticFieldsName(ChildBean.class);
        assertThat(names).containsExactly("parentField", "childField", "parentField");
    }

    @Test
    @DisplayName("getNonStaticFields 子类同名字段在前、父类字段在后")
    void shouldKeepChildFieldBeforeParentField() {
        List<Field> fields = ReflectUtils.getNonStaticFields(ChildBean.class);
        assertThat(fields).extracting(Field::getName)
            .containsExactly("parentField", "childField", "parentField");
        assertThat(fields).extracting(Field::getDeclaringClass)
            .containsExactly(ChildBean.class, ChildBean.class, ParentBean.class);
    }

    @Test
    @DisplayName("无任何字段的类返回空列表")
    void shouldReturnEmptyListForFieldlessClass() {
        assertThat(ReflectUtils.getNonStaticFieldsName(Object.class)).isEmpty();
    }

    @Test
    @DisplayName("getNonStaticFields 返回可变列表")
    void shouldReturnMutableFieldList() {
        List<Field> fields = ReflectUtils.getNonStaticFields(ParentBean.class);
        fields.add(fields.get(0));
        assertThat(fields).hasSize(2);
    }

    @Test
    @DisplayName("createMethodReference 创建的函数按方法语义求值")
    void shouldCreateWorkingMethodReference() {
        Function<ChildBean, String> getter = ReflectUtils
            .createMethodReference(ChildBean.class, "getChildField");
        ChildBean bean = new ChildBean();
        bean.childField = "value";
        assertThat(getter.apply(bean)).isEqualTo("value");
    }

    @Test
    @DisplayName("createMethodReference 对父类方法同样生效")
    void shouldCreateReferenceForParentMethod() throws Exception {
        Function<ChildBean, String> getter = ReflectUtils
            .createMethodReference(ChildBean.class, "getParentField");
        ChildBean bean = new ChildBean();
        // 同名遮蔽下须显式写入父类声明字段，验证 getter 读取路径
        Field parentField = ParentBean.class.getDeclaredField("parentField");
        parentField.setAccessible(true);
        parentField.set(bean, "parent-value");
        assertThat(getter.apply(bean)).isEqualTo("parent-value");
    }

    @Test
    @DisplayName("createMethodReference 对不存在的方法包装为 BusinessException")
    void shouldWrapFailureAsBusinessException() {
        assertThatThrownBy(() -> ReflectUtils
            .createMethodReference(ChildBean.class, "notExists"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ChildBean.notExists");
    }
}
