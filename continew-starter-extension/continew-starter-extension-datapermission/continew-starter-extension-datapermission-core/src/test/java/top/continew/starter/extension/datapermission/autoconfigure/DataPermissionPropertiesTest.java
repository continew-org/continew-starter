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

package top.continew.starter.extension.datapermission.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.AnnotationUtils;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * {@link DataPermissionProperties} 配置属性测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DataPermissionPropertiesTest {

    @Test
    @DisplayName("配置前缀为 continew-starter.data-permission 且与常量定义一致")
    void shouldBindDataPermissionPrefix() {
        // prefix 与 value 互为别名，源码按位置声明在 value 上，须经别名合成后读取 prefix
        ConfigurationProperties annotation =
            AnnotationUtils.getAnnotation(DataPermissionProperties.class,
                ConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("continew-starter.data-permission");
        assertThat(annotation.prefix()).isEqualTo(PropertiesConstants.DATA_PERMISSION);
    }

    @Test
    @DisplayName("enabled 默认开启")
    void shouldDefaultEnabledToTrue() {
        assertThat(new DataPermissionProperties().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("可通过 setter 关闭数据权限")
    void shouldAllowDisablingViaSetter() {
        DataPermissionProperties properties = new DataPermissionProperties();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }
}
