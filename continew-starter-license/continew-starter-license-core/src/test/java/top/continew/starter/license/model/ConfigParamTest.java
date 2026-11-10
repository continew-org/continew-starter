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

package top.continew.starter.license.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ConfigParam} 读写行为测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ConfigParamTest {

    @Test
    @DisplayName("全部字段 setter 写入后 getter 原样读出")
    void shouldRoundTripAllProperties() {
        ConfigParam configParam = new ConfigParam();
        configParam.setSubject("license-subject");
        configParam.setPublicAlias("public-alias");
        configParam.setStorePass("store-pass-123");

        assertThat(configParam.getSubject()).isEqualTo("license-subject");
        assertThat(configParam.getPublicAlias()).isEqualTo("public-alias");
        assertThat(configParam.getStorePass()).isEqualTo("store-pass-123");
    }

    @Test
    @DisplayName("新建对象全部字段为 null")
    void shouldCreateWithAllFieldsNull() {
        ConfigParam configParam = new ConfigParam();
        assertThat(configParam.getSubject()).isNull();
        assertThat(configParam.getPublicAlias()).isNull();
        assertThat(configParam.getStorePass()).isNull();
    }
}
