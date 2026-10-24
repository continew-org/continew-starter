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

package top.continew.starter.cache.multicache.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 多级缓存 L1 失效广播（集群一致性）
 *
 * <p>
 * 任一节点执行 evict/clear 时，清除本地 L1、L2 后向 Redis topic 广播失效消息；
 * 其他节点收到消息后仅清除本地 L1，不级联再广播，防止广播风暴。
 * 消息体格式：{@code nodeId|allEntries|cacheName|key}（key 为 String 化后的缓存键）。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class CacheInvalidationPublisher implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInvalidationPublisher.class);

    private static final String SEPARATOR = "|";
    private static final String NULL_KEY = "~";

    private final StringRedisTemplate stringRedisTemplate;
    private final String topic;
    private final String nodeId = UUID.randomUUID().toString();

    /**
     * 本节点缓存管理器（收到广播时按 cacheName 清 L1）
     */
    private volatile MultiLevelCacheManager cacheManager;

    public CacheInvalidationPublisher(StringRedisTemplate stringRedisTemplate, String topic) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.topic = topic;
    }

    public void setCacheManager(MultiLevelCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public String getTopic() {
        return topic;
    }

    /**
     * 发布失效消息（key 为 null 且 allEntries 为 true 时表示清空整个 cacheName）
     *
     * @param cacheName   缓存名称
     * @param key         缓存键（null 表示全部失效）
     * @param allEntries 是否清空整个 cacheName
     */
    public void publish(String cacheName, Object key, boolean allEntries) {
        try {
            String message = nodeId + SEPARATOR + allEntries + SEPARATOR + cacheName + SEPARATOR
                + (key == null ? NULL_KEY : String.valueOf(key));
            stringRedisTemplate.convertAndSend(topic, message);
        } catch (RuntimeException e) {
            // 广播失败不影响主流程，其他节点 L1 会随本地 TTL 自然过期
            LOGGER.warn("[ContiNew Starter] - Failed to publish cache invalidation message, "
                + "cacheName: {}, key: {}. Cause: {}", cacheName, key, e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            String[] parts = body.split("\\" + SEPARATOR, 4);
            if (parts.length < 4) {
                return;
            }
            // 忽略本节点发出的消息（本节点已在 evict 时清过 L1）
            if (nodeId.equals(parts[0])) {
                return;
            }
            boolean allEntries = Boolean.parseBoolean(parts[1]);
            String cacheName = parts[2];
            String key = NULL_KEY.equals(parts[3]) ? null : parts[3];
            if (cacheManager == null) {
                return;
            }
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof MultiLevelCache multiLevelCache) {
                if (allEntries || key == null) {
                    multiLevelCache.clearLocal();
                } else {
                    multiLevelCache.evictLocal(key);
                }
                LOGGER.debug("[ContiNew Starter] - Cleared local cache by broadcast, "
                    + "cacheName: {}, key: {}, allEntries: {}", cacheName, key, allEntries);
            }
        } catch (RuntimeException e) {
            LOGGER.warn(
                "[ContiNew Starter] - Failed to handle cache invalidation message. Cause: {}",
                e.getMessage());
        }
    }
}
