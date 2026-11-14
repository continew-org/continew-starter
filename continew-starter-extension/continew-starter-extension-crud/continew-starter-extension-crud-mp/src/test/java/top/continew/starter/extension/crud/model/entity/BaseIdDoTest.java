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

package top.continew.starter.extension.crud.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * {@link BaseIdDO} 实体基类单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class BaseIdDoTest {

    /**
     * 最小测试实体
     */
    static class TestDO extends BaseIdDO {
    }

    /**
     * ID 读写往返应保持一致。
     */
    @Test
    void shouldSetAndGetId() {
        TestDO entity = new TestDO();
        entity.setId(1L);
        assertThat(entity.getId()).isEqualTo(1L);
    }

    /**
     * 新建实体未设置 ID 时应保持为 null。
     */
    @Test
    void shouldDefaultIdToNull() {
        assertThat(new TestDO().getId()).isNull();
    }

    /**
     * 实体实现 Serializable，经 Java 原生序列化往返后 ID 不应丢失。
     */
    @Test
    void shouldSurviveJavaSerialization() throws Exception {
        TestDO entity = new TestDO();
        entity.setId(2L);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(entity);
        }
        TestDO deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer
            .toByteArray()))) {
            deserialized = (TestDO) in.readObject();
        }
        assertThat(deserialized.getId()).isEqualTo(2L);
    }
}
