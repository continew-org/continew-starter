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

package top.continew.starter.extension.datapermission.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

/**
 * {@link DataPermissionConstants} 常量测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DataPermissionConstantsTest {

    @Test
    @DisplayName("祖先节点列名为 ancestors")
    void shouldExposeAncestorsColumn() {
        assertThat(DataPermissionConstants.ANCESTORS_COLUMN).isEqualTo("ancestors");
    }

    @Test
    @DisplayName("COUNT 方法名后缀为 _COUNT")
    void shouldExposeCountMethodSuffix() {
        assertThat(DataPermissionConstants.COUNT_METHOD_SUFFIX).isEqualTo("_COUNT");
    }

    @Test
    @DisplayName("常量类为 final 且构造器私有，禁止继承与实例化")
    void shouldNotBeExtendableOrInstantiable() throws Exception {
        assertThat(Modifier.isFinal(DataPermissionConstants.class.getModifiers())).isTrue();
        assertThat(Modifier
            .isPrivate(DataPermissionConstants.class.getDeclaredConstructor().getModifiers()))
            .isTrue();
    }
}
