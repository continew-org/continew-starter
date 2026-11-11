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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

/**
 * {@link LicenseExtraModel} 读写与序列化行为测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseExtraModelTest {

    @Test
    @DisplayName("全部字段 setter 写入后 getter 原样读出")
    void shouldRoundTripAllProperties() {
        LicenseExtraModel model = new LicenseExtraModel();
        model.setIpAddress(Set.of("192.168.1.10", "10.0.0.5"));
        model.setMacAddress(Set.of("AA-BB-CC-DD-EE-FF"));
        model.setCpuSerial("BFEBFBFF000806EC");
        model.setMainBoardSerial("MB-serial-01");

        assertThat(model.getIpAddress()).containsExactlyInAnyOrder("192.168.1.10", "10.0.0.5");
        assertThat(model.getMacAddress()).containsExactly("AA-BB-CC-DD-EE-FF");
        assertThat(model.getCpuSerial()).isEqualTo("BFEBFBFF000806EC");
        assertThat(model.getMainBoardSerial()).isEqualTo("MB-serial-01");
    }

    @Test
    @DisplayName("Java 序列化往返后字段完整保留")
    void shouldSurviveJavaSerialization() throws Exception {
        LicenseExtraModel model = new LicenseExtraModel();
        model.setIpAddress(Set.of("192.168.1.10"));
        model.setMacAddress(Set.of("AA-BB-CC-DD-EE-FF"));
        model.setCpuSerial("BFEBFBFF000806EC");
        model.setMainBoardSerial("MB-serial-01");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(model);
        }
        LicenseExtraModel restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer
            .toByteArray()))) {
            restored = (LicenseExtraModel) in.readObject();
        }

        assertThat(restored.getIpAddress()).containsExactly("192.168.1.10");
        assertThat(restored.getMacAddress()).containsExactly("AA-BB-CC-DD-EE-FF");
        assertThat(restored.getCpuSerial()).isEqualTo("BFEBFBFF000806EC");
        assertThat(restored.getMainBoardSerial()).isEqualTo("MB-serial-01");
    }
}
