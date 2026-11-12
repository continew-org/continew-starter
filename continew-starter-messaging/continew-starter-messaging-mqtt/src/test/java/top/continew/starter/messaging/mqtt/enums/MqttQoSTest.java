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

package top.continew.starter.messaging.mqtt.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.messaging.mqtt.exception.MqttException;

/**
 * {@link MqttQoS} QoS 值与枚举互转测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("MqttQoS 枚举转换测试")
class MqttQoSTest {

    @Test
    @DisplayName("枚举值域与数值一一对应")
    void shouldExposeQosValues() {
        assertEquals(0, MqttQoS.AT_MOST_ONCE.value());
        assertEquals(1, MqttQoS.AT_LEAST_ONCE.value());
        assertEquals(2, MqttQoS.EXACTLY_ONCE.value());
        assertEquals(0x80, MqttQoS.FAILURE.value());
    }

    @Test
    @DisplayName("valueOf 按数值还原枚举，非法数值拒绝")
    void shouldRestoreEnumFromValue() {
        assertEquals(MqttQoS.AT_MOST_ONCE, MqttQoS.valueOf(0));
        assertEquals(MqttQoS.AT_LEAST_ONCE, MqttQoS.valueOf(1));
        assertEquals(MqttQoS.EXACTLY_ONCE, MqttQoS.valueOf(2));
        assertEquals(MqttQoS.FAILURE, MqttQoS.valueOf(0x80));
        assertThrows(MqttException.class, () -> MqttQoS.valueOf(3));
    }

    @Test
    @DisplayName("toString 输出 QoS 前缀形式")
    void shouldRenderPrefixedString() {
        assertEquals("QoS1", MqttQoS.AT_LEAST_ONCE.toString());
        assertEquals("QoS128", MqttQoS.FAILURE.toString());
    }
}
