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

import top.continew.starter.messaging.mqtt.exception.MqttException;

import java.util.List;

/**
 * 消息主题工具类
 *
 * @author echo
 * @since 2.15.0
 */
public class TopicUtils {

    public static final char TOPIC_WILDCARDS_ONE = '+';

    public static final char TOPIC_WILDCARDS_MORE = '#';

    private TopicUtils() {
    }

    /**
     * 校验 topicFilter
     *
     * @param topicFilterList topicFilter 集合
     */
    public static void validateTopicFilter(List<String> topicFilterList) {
        for (String topicFilter : topicFilterList) {
            validateTopicFilter(topicFilter);
        }
    }

    /**
     * 校验 topicFilter
     *
     * @param topicFilter topicFilter
     */
    public static void validateTopicFilter(String topicFilter) throws MqttException {
        if (topicFilter == null || topicFilter.isEmpty()) {
            throw new MqttException("TopicFilter is blank:" + topicFilter);
        }
        char[] topicFilterChars = topicFilter.toCharArray();
        int topicFilterLength = topicFilterChars.length;
        int topicFilterIdxEnd = topicFilterLength - 1;
        char ch;
        for (int i = 0; i < topicFilterLength; i++) {
            ch = topicFilterChars[i];
            if (Character.isWhitespace(ch)) {
                throw new MqttException(
                    "Mqtt subscribe topicFilter has white space:" + topicFilter);
            } else if (ch == TOPIC_WILDCARDS_MORE) {
                // 校验: # 通配符只能在最后一位
                if (i < topicFilterIdxEnd) {
                    throw new MqttException("Mqtt subscribe topicFilter illegal:" + topicFilter);
                }
            } else if (ch == TOPIC_WILDCARDS_ONE
                && ((i > 0 && topicFilterChars[i - 1] != '/')
                    || (i < topicFilterIdxEnd && topicFilterChars[i + 1] != '/'))) {
                // 校验: 单独 + 是允许的，判断 + 号前一位是否为 /，如果有后一位也必须为 /
                throw new MqttException("Mqtt subscribe topicFilter illegal:" + topicFilter);
            }
        }
    }

    /**
     * 判断是否 topic filter
     *
     * @param topicFilter topicFilter
     * @return 是否 topic filter
     */
    public static boolean isTopicFilter(String topicFilter) {
        char[] topicFilterChars = topicFilter.toCharArray();
        for (char ch : topicFilterChars) {
            if (TOPIC_WILDCARDS_ONE == ch || TOPIC_WILDCARDS_MORE == ch) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验 topicName
     *
     * @param topicName topicName
     */
    public static void validateTopicName(String topicName) throws MqttException {
        if (topicName.isEmpty()) {
            throw new MqttException("Topic is blank:" + topicName);
        }
        if (isTopicFilter(topicName)) {
            throw new MqttException("Topic has wildcards char [+] or [#], topicName:" + topicName);
        }
    }

    /**
     * 判断 topicFilter topicName 是否匹配
     *
     * @param topicFilter topicFilter
     * @param topicName   topicName
     * @return 是否匹配
     */
    public static boolean match(String topicFilter, String topicName) {
        char[] topicFilterChars = topicFilter.toCharArray();
        char[] topicNameChars = topicName.toCharArray();
        int topicFilterLength = topicFilterChars.length;
        int topicNameLength = topicNameChars.length;
        int topicFilterIdxEnd = topicFilterLength - 1;
        int topicNameIdxEnd = topicNameLength - 1;
        // 是否进入 + 号层级通配符
        boolean inLayerWildcard = false;
        int wildcardCharLen = 0;
        for (int i = 0; i < topicFilterLength; i++) {
            char ch = topicFilterChars[i];
            if (ch == TOPIC_WILDCARDS_MORE) {
                // 校验: # 通配符只能在最后一位
                if (i < topicFilterIdxEnd) {
                    throw new MqttException("Mqtt subscribe topicFilter illegal:" + topicFilter);
                }
                return true;
            }
            if (ch == TOPIC_WILDCARDS_ONE) {
                validateSingleLevelPlacement(i, topicFilterChars, topicFilterIdxEnd, topicFilter);
                // 末层 + 通配符：剩余 topicName 中不能再出现层级分隔符 /
                if (isTerminalSingleLevel(i, wildcardCharLen, topicFilterIdxEnd, topicNameLength)) {
                    return matchTerminalSingleLevel(i, wildcardCharLen, topicNameChars,
                        topicNameLength);
                }
                inLayerWildcard = true;
            } else if (ch == '/') {
                inLayerWildcard = false;
                // 预读下一位，如果是 #，并且 topicName 位数已经不足
                if (isMultiLevelShortcut(i, topicFilterChars, topicFilterLength, topicNameLength)) {
                    return true;
                }
            }
            // topicName 长度不够了
            if (topicNameIdxEnd < i) {
                return false;
            }
            // 进入 + 号层级通配符匹配
            if (inLayerWildcard) {
                int start = i + wildcardCharLen;
                int separator = findLayerSeparator(start, topicNameChars, topicNameLength);
                if (separator >= 0) {
                    // 非层级分隔符累计后，在分隔符处回退一个偏移
                    wildcardCharLen += (separator - start) - 1;
                    continue;
                }
                // 剩余均为同层字符，累计偏移量
                wildcardCharLen += (topicNameLength - start);
            }
            // topicName index
            int topicNameIdx = i + wildcardCharLen;
            // topic 已经完成，topicName 还有数据
            if (topicNameIdx > topicNameIdxEnd) {
                return false;
            }
            if (ch != topicNameChars[topicNameIdx]) {
                return false;
            }
        }
        // 判断 topicName 是否还有数据
        return topicFilterLength + wildcardCharLen + 1 > topicNameLength;
    }

    /**
     * 校验 + 通配符的位置：必须单独成层（前一位为 / 开头，后一位为 / 或在末尾）
     */
    private static void validateSingleLevelPlacement(int i, char[] topicFilterChars,
        int topicFilterIdxEnd, String topicFilter) {
        boolean invalidPrefix = i > 0 && topicFilterChars[i - 1] != '/';
        boolean invalidSuffix = i < topicFilterIdxEnd && topicFilterChars[i + 1] != '/';
        if (invalidPrefix || invalidSuffix) {
            throw new MqttException("Mqtt subscribe topicFilter illegal:" + topicFilter);
        }
    }

    /**
     * 判断 + 是否位于 filter 末层且 topicName 仍有剩余字符可匹配
     */
    private static boolean isTerminalSingleLevel(int i, int wildcardCharLen,
        int topicFilterIdxEnd, int topicNameLength) {
        int topicNameIdx = i + wildcardCharLen;
        return i == topicFilterIdxEnd && topicNameLength > topicNameIdx;
    }

    /**
     * 处理末层 + 通配符：剩余 topicName 中不能再出现层级分隔符 /
     */
    private static boolean matchTerminalSingleLevel(int i, int wildcardCharLen,
        char[] topicNameChars, int topicNameLength) {
        int topicNameIdx = i + wildcardCharLen;
        for (int j = topicNameIdx; j < topicNameLength; j++) {
            if (topicNameChars[j] == '/') {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断 / 后紧跟 # 且 topicName 位数已不足的快捷匹配场景
     */
    private static boolean isMultiLevelShortcut(int slashIndex, char[] topicFilterChars,
        int topicFilterLength, int topicNameLength) {
        int next = slashIndex + 1;
        return topicFilterLength > next && topicFilterChars[next] == TOPIC_WILDCARDS_MORE
            && topicNameLength < next;
    }

    /**
     * 在 topicName 中从指定位置开始查找层级分隔符 /
     *
     * @return 分隔符下标；未找到返回 -1
     */
    private static int findLayerSeparator(int start, char[] topicNameChars, int topicNameLength) {
        for (int j = start; j < topicNameLength; j++) {
            if (topicNameChars[j] == '/') {
                return j;
            }
        }
        return -1;
    }

    /**
     * 获取处理完成之后的 topic
     *
     * @param topicTemplate topic 模板
     * @return 获取处理完成之后的 topic
     */
    public static String getTopicFilter(String topicTemplate) {
        // 替换 ${name} 为 +
        StringBuilder sb = new StringBuilder(topicTemplate.length());
        int cursor = 0;
        for (int start, end; (start = topicTemplate.indexOf("${", cursor)) != -1
            && (end = topicTemplate
                .indexOf('}', start)) != -1;) {
            sb.append(topicTemplate, cursor, start);
            sb.append('+');
            cursor = end + 1;
        }
        sb.append(topicTemplate.substring(cursor));
        return sb.toString();
    }
}
