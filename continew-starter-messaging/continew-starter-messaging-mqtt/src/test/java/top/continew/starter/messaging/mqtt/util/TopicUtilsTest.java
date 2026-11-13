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

package top.continew.starter.messaging.mqtt.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.messaging.mqtt.exception.MqttException;

import java.util.List;

/**
 * {@link TopicUtils} 通配符匹配与校验语义测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("TopicUtils 通配符匹配与校验测试")
class TopicUtilsTest {

    @Test
    @DisplayName("精确匹配：完全一致才命中，长度不对齐不命中")
    void shouldMatchExactTopicOnly() {
        assertTrue(TopicUtils.match("a/b", "a/b"));
        assertFalse(TopicUtils.match("a/b", "a/b/c"));
        assertFalse(TopicUtils.match("a/b/c", "a/b"));
    }

    @Test
    @DisplayName("单级通配 +：命中任意一层，不跨层级；空层级现行为不命中")
    void shouldMatchSingleLevelWildcardWithinOneLayer() {
        assertTrue(TopicUtils.match("a/+/c", "a/b/c"));
        // 现行为锁定：空层级不命中 + 通配，与 MQTT 规范「+ 可匹配空层级」不一致（产品线索待裁决）
        assertFalse(TopicUtils.match("+/b", "/b"));
        assertFalse(TopicUtils.match("a/+/c", "a//c"));
        assertFalse(TopicUtils.match("a/+/c", "a/x/y/c"));
        assertFalse(TopicUtils.match("a/+/c", "a/b/d"));
    }

    @Test
    @DisplayName("多级通配 #：命中剩余全部层级，也命中父级")
    void shouldMatchMultiLevelWildcardFromItsPosition() {
        assertTrue(TopicUtils.match("a/#", "a/b/c"));
        assertTrue(TopicUtils.match("a/#", "a"));
        assertTrue(TopicUtils.match("#", "a/b/c"));
        assertTrue(TopicUtils.match("a/b/#", "a/b"));
        assertFalse(TopicUtils.match("a/#", "ab"));
        // # 只能在末位，中途出现直接判定非法
        assertThrows(MqttException.class, () -> TopicUtils.match("a/#/c", "a/b/c"));
    }

    @Test
    @DisplayName("isTopicFilter 判断过滤器与 topicName 的通配符边界")
    void shouldDetectWildcardChars() {
        assertTrue(TopicUtils.isTopicFilter("a/+"));
        assertTrue(TopicUtils.isTopicFilter("a/#"));
        assertFalse(TopicUtils.isTopicFilter("a/b"));
    }

    @Test
    @DisplayName("validateTopicFilter：空白、# 非末位、+ 不独立成层均拒绝")
    void shouldValidateFilterPlacement() {
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicFilter((String) null));
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicFilter(""));
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicFilter("a b"));
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicFilter("a/#/b"));
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicFilter("a+/b"));
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicFilter("/a+"));
        assertDoesNotThrow(() -> TopicUtils.validateTopicFilter("a/b#"));
        assertDoesNotThrow(() -> TopicUtils.validateTopicFilter("a/+/b/#"));
    }

    @Test
    @DisplayName("validateTopicFilter 集合逐项校验")
    void shouldValidateFilterList() {
        assertDoesNotThrow(() -> TopicUtils.validateTopicFilter(List.of("a/b", "a/+")));
        List<String> filters = List.of("a/b", "a b");
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicFilter(filters));
    }

    @Test
    @DisplayName("validateTopicName：空串与携带通配符的 topic 名拒绝")
    void shouldRejectInvalidTopicName() {
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicName(""));
        assertThrows(MqttException.class, () -> TopicUtils.validateTopicName("a/+"));
        assertDoesNotThrow(() -> TopicUtils.validateTopicName("a/b/c"));
    }

    @Test
    @DisplayName("getTopicFilter 将模板占位符替换为单级通配符")
    void shouldReplacePlaceholderWithWildcard() {
        assertEquals("sensor/+/data", TopicUtils.getTopicFilter("sensor/${id}/data"));
        assertEquals("plain", TopicUtils.getTopicFilter("plain"));
        assertEquals("++", TopicUtils.getTopicFilter("${a}${b}"));
        assertEquals("prefix/+", TopicUtils.getTopicFilter("prefix/${tail}"));
    }
}
