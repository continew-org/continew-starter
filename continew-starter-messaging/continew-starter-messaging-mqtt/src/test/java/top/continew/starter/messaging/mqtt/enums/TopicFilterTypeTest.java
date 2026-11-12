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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.messaging.mqtt.exception.MqttException;

/**
 * {@link TopicFilterType} 类型识别与共享订阅匹配语义测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("TopicFilterType 类型识别与匹配测试")
class TopicFilterTypeTest {

    @Test
    @DisplayName("getType 按前缀识别 $queue/$share/普通过滤器")
    void shouldClassifyFilterByPrefix() {
        assertEquals(TopicFilterType.QUEUE, TopicFilterType.getType("$queue/device/data"));
        assertEquals(TopicFilterType.SHARE, TopicFilterType.getType("$share/g1/device/data"));
        assertEquals(TopicFilterType.NONE, TopicFilterType.getType("device/data"));
        assertEquals(TopicFilterType.NONE, TopicFilterType.getType("$other/device"));
    }

    @Test
    @DisplayName("NONE 过滤器直接复用 TopicUtils 匹配")
    void shouldMatchNoneFilterByTopicUtils() {
        assertTrue(TopicFilterType.NONE.match("device/+/data", "device/room1/data"));
        assertFalse(TopicFilterType.NONE.match("device/+/data", "device/room1/data/extra"));
    }

    @Test
    @DisplayName("$queue 过滤器剥前缀后匹配原始 topic")
    void shouldMatchQueueFilterWithoutPrefix() {
        assertTrue(TopicFilterType.QUEUE.match("$queue/device/data", "device/data"));
        assertFalse(TopicFilterType.QUEUE.match("$queue/device/data", "device/other"));
    }

    @Test
    @DisplayName("$share 过滤器剥群组前缀后匹配，群组名不影响匹配结果")
    void shouldMatchShareFilterAfterStrippingGroup() {
        assertTrue(TopicFilterType.SHARE.match("$share/g1/device/data", "device/data"));
        assertTrue(TopicFilterType.SHARE.match("$share/g2/device/+/state", "device/r1/state"));
        assertFalse(TopicFilterType.SHARE.match("$share/g1/device/data", "device/other"));
    }

    @Test
    @DisplayName("getShareGroupName 提取群组名，缺少群组层级时拒绝")
    void shouldExtractShareGroupName() {
        assertEquals("g1", TopicFilterType.getShareGroupName("$share/g1/device/data"));
        assertThrows(MqttException.class, () -> TopicFilterType.getShareGroupName("$share/g1"));
    }
}
