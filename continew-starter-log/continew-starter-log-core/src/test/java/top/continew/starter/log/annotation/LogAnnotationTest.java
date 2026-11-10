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

package top.continew.starter.log.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import top.continew.starter.log.enums.Include;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * {@link Log} 与 {@link ConditionalOnEnabledLog} 注解元数据测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LogAnnotationTest {

    /**
     * 日志注解应支持类与方法、运行期可见且可被文档化
     */
    @Test
    @DisplayName("@Log 元注解为目标类型与运行期保留")
    void logAnnotationShouldDeclareRuntimeRetentionOnTypeAndMethod() {
        Retention retention = Log.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(Log.class.getAnnotation(Documented.class)).isNotNull();
        Target target = Log.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactlyInAnyOrder(ElementType.METHOD, ElementType.TYPE);
    }

    /**
     * 日志注解各属性的默认值应为空描述、空模块、空包含集与非忽略
     */
    @Test
    @DisplayName("@Log 属性默认值为空串/空数组/false")
    void logAnnotationShouldDeclareExpectedDefaults() throws Exception {
        assertThat(defaultOf("value")).isEqualTo("");
        assertThat(defaultOf("module")).isEqualTo("");
        assertThat((Include[]) defaultOf("includes")).isEmpty();
        assertThat((Include[]) defaultOf("excludes")).isEmpty();
        assertThat(defaultOf("ignore")).isEqualTo(false);
    }

    /**
     * 启用日志条件注解应基于 continew-starter.log.enabled 开关且属性缺失时默认启用
     */
    @Test
    @DisplayName("@ConditionalOnEnabledLog 绑定开关且缺失时默认启用")
    void conditionalOnEnabledLogShouldBindEnabledPropertyWithMatchIfMissing() {
        Retention retention = ConditionalOnEnabledLog.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        ConditionalOnProperty condition = ConditionalOnEnabledLog.class
            .getAnnotation(ConditionalOnProperty.class);
        assertThat(condition).isNotNull();
        assertThat(condition.prefix()).isEqualTo("continew-starter.log");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    /**
     * 启用日志条件注解与日志注解一致地作用于类与方法
     */
    @Test
    @DisplayName("@ConditionalOnEnabledLog 目标为类型与方法")
    void conditionalOnEnabledLogShouldTargetTypeAndMethod() {
        Target target = ConditionalOnEnabledLog.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactlyInAnyOrder(ElementType.TYPE, ElementType.METHOD);
        assertThat(ConditionalOnEnabledLog.class.getAnnotation(Documented.class)).isNotNull();
    }

    private Object defaultOf(String attributeName) throws Exception {
        Method attribute = Log.class.getMethod(attributeName);
        return attribute.getDefaultValue();
    }
}
