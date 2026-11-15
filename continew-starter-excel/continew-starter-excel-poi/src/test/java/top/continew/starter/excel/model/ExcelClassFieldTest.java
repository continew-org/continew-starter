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

package top.continew.starter.excel.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link ExcelClassField} Excel 字段信息单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExcelClassFieldTest {

    /**
     * 字段名、表头名、映射、示例值、排序与注解标记读写往返应保持一致。
     */
    @Test
    void shouldSetAndGetAllProperties() {
        ExcelClassField cf = new ExcelClassField();
        Map<String, String> kvMap = new LinkedHashMap<>();
        kvMap.put("1", "男");
        kvMap.put("2", "女");

        cf.setFieldName("gender");
        cf.setName("性别");
        cf.setKvMap(kvMap);
        cf.setExample(18);
        cf.setSort(2);
        cf.setHasAnnotation(1);

        assertThat(cf.getFieldName()).isEqualTo("gender");
        assertThat(cf.getName()).isEqualTo("性别");
        assertThat(cf.getKvMap()).containsEntry("1", "男").containsEntry("2", "女");
        assertThat(cf.getExample()).isEqualTo(18);
        assertThat(cf.getSort()).isEqualTo(2);
        assertThat(cf.getHasAnnotation()).isEqualTo(1);
    }

    /**
     * 新建实例各属性应保持默认值（对象为 null、数值为 0）。
     */
    @Test
    void shouldDefaultToNullAndZero() {
        ExcelClassField cf = new ExcelClassField();

        assertThat(cf.getFieldName()).isNull();
        assertThat(cf.getName()).isNull();
        assertThat(cf.getKvMap()).isNull();
        assertThat(cf.getExample()).isNull();
        assertThat(cf.getSort()).isZero();
        assertThat(cf.getHasAnnotation()).isZero();
    }
}
