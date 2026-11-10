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

package top.continew.starter.extension.tenant.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import top.continew.starter.core.constant.PropertiesConstants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 租户注解元数据契约测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantAnnotationTest {

    @Test
    @DisplayName("TenantIgnore 可标注于类型与方法，运行期可见且被 @Documented")
    void shouldKeepTenantIgnoreMetadata() {
        assertThat(TenantIgnore.class.getAnnotation(Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(TenantIgnore.class.getAnnotation(Documented.class)).isNotNull();
        assertThat(TenantIgnore.class.getAnnotation(Target.class).value())
            .containsExactlyInAnyOrder(ElementType.TYPE, ElementType.METHOD);
    }

    @Test
    @DisplayName("ConditionalOnEnabledTenant 绑定 continew-starter.tenant.enabled，缺省视为启用")
    void shouldBindConditionalOnEnabledTenantToTenantEnabledProperty() {
        assertThat(ConditionalOnEnabledTenant.class.getAnnotation(Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
        ConditionalOnProperty conditional = ConditionalOnEnabledTenant.class
            .getAnnotation(ConditionalOnProperty.class);
        assertThat(conditional).isNotNull();
        assertThat(conditional.prefix()).isEqualTo(PropertiesConstants.TENANT);
        assertThat(conditional.name()).containsExactly(PropertiesConstants.ENABLED);
        assertThat(conditional.havingValue()).isEqualTo("true");
        assertThat(conditional.matchIfMissing()).isTrue();
    }
}
