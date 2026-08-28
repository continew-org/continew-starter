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

import top.continew.starter.messaging.mqtt.exception.MqttException;
import top.continew.starter.messaging.mqtt.util.TopicUtils;

/**
 * topic 筛选器类型
 *
 * @author echo
 * @since 2.15.0
 */
public enum TopicFilterType {

    /**
     * 默认 TopicFilter
     */
    NONE {

        @Override
        public boolean match(String topicFilter, String topicName) {
            return TopicUtils.match(topicFilter, topicName);
        }
    },

    /**
     * $queue/ 为前缀的共享订阅是不带群组的共享订阅
     */
    QUEUE {

        @Override
        public boolean match(String topicFilter, String topicName) {
            int prefixLen = TopicFilterType.SHARE_QUEUE_PREFIX.length();
            return TopicUtils.match(topicFilter.substring(prefixLen), topicName);
        }
    },

    /**
     * $share/{group-name}/ 为前缀的共享订阅是带群组的共享订阅
     */
    SHARE {

        @Override
        public boolean match(String topicFilter, String topicName) {
            // 去除前缀 $share/<group-name>/ ,匹配 topicName / 前缀
            int prefixLen = TopicFilterType.findShareTopicIndex(topicFilter);
            return TopicUtils.match(topicFilter.substring(prefixLen), topicName);
        }
    };

    /**
     * 共享订阅的 topic
     */
    public static final String SHARE_QUEUE_PREFIX = "$queue/";

    public static final String SHARE_GROUP_PREFIX = "$share/";

    /**
     * 判断 topicFilter 和 topicName 匹配情况
     *
     * @param topicFilter topicFilter
     * @param topicName   topicName
     * @return 是否匹配
     */
    public abstract boolean match(String topicFilter, String topicName);

    /**
     * 获取 topicFilter 类型
     *
     * @param topicFilter topicFilter
     * @return TopicFilterType
     */
    public static TopicFilterType getType(String topicFilter) {
        if (topicFilter.startsWith(TopicFilterType.SHARE_QUEUE_PREFIX)) {
            return TopicFilterType.QUEUE;
        } else if (topicFilter.startsWith(TopicFilterType.SHARE_GROUP_PREFIX)) {
            return TopicFilterType.SHARE;
        } else {
            return TopicFilterType.NONE;
        }
    }

    /**
     * 读取共享订阅的分组名
     *
     * @param topicFilter topicFilter
     * @return 共享订阅分组名
     */
    public static String getShareGroupName(String topicFilter) {
        int prefixLength = TopicFilterType.SHARE_GROUP_PREFIX.length();
        int topicFilterLength = topicFilter.length();
        for (int i = prefixLength; i < topicFilterLength; i++) {
            char ch = topicFilter.charAt(i);
            if ('/' == ch) {
                return topicFilter.substring(prefixLength, i);
            }
        }
        throw new MqttException("分享订阅topic过滤器: " + topicFilter + " 不符合 $share/<group-name>/xxx");
    }

    private static int findShareTopicIndex(String topicFilter) {
        int prefixLength = TopicFilterType.SHARE_GROUP_PREFIX.length();
        int topicFilterLength = topicFilter.length();
        for (int i = prefixLength; i < topicFilterLength; i++) {
            char ch = topicFilter.charAt(i);
            if ('/' == ch) {
                return i + 1;
            }
        }
        throw new MqttException("分享订阅主题过滤器: " + topicFilter + " 不符合 $share/<group-name>/xxx");
    }
}
