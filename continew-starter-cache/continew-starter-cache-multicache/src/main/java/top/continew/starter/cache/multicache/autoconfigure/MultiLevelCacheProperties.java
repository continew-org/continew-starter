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

package top.continew.starter.cache.multicache.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多级缓存配置属性
 *
 * @author KAI
 * @since 3.0.0
 */
@ConfigurationProperties(prefix = PropertiesConstants.CACHE_MULTI)
public class MultiLevelCacheProperties {

    /**
     * 是否启用多级缓存
     */
    private boolean enabled = true;

    /**
     * 是否启用集群 L1 失效广播（多节点部署时开启）
     */
    private boolean broadcastEnabled = true;

    /**
     * L1 失效广播 topic（默认按应用名隔离，多应用共用同一 Redis 时互不干扰；
     * 支持占位符，如需跨应用共享失效广播可显式配置为公共 topic）
     */
    private String broadcastTopic = "continew:cache:evict:${spring.application.name:default}";

    /**
     * Redis（L2）不可用时是否降级为仅本地缓存
     */
    private boolean degradeOnRedisFailure = true;

    /**
     * 本地（L1）缓存全局配置
     */
    private Local local = new Local();

    /**
     * 远程（L2）缓存全局配置
     */
    private Remote remote = new Remote();

    /**
     * 按 cacheName 单独覆盖的配置
     */
    private Map<String, CacheSpec> specs = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }

    public void setBroadcastEnabled(boolean broadcastEnabled) {
        this.broadcastEnabled = broadcastEnabled;
    }

    public String getBroadcastTopic() {
        return broadcastTopic;
    }

    public void setBroadcastTopic(String broadcastTopic) {
        this.broadcastTopic = broadcastTopic;
    }

    public boolean isDegradeOnRedisFailure() {
        return degradeOnRedisFailure;
    }

    public void setDegradeOnRedisFailure(boolean degradeOnRedisFailure) {
        this.degradeOnRedisFailure = degradeOnRedisFailure;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Remote getRemote() {
        return remote;
    }

    public void setRemote(Remote remote) {
        this.remote = remote;
    }

    public Map<String, CacheSpec> getSpecs() {
        return specs;
    }

    public void setSpecs(Map<String, CacheSpec> specs) {
        this.specs = specs;
    }

    /**
     * 本地（L1）缓存配置
     */
    public static class Local {

        /**
         * 写入后过期时长（null 表示永不过期）
         */
        private Duration expireAfterWrite = Duration.ofHours(2);

        /**
         * 最大缓存元素数（<=0 表示不启用本地缓存，退化为纯 Redis 缓存）
         */
        private long limit = 1000;

        public Duration getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public long getLimit() {
            return limit;
        }

        public void setLimit(long limit) {
            this.limit = limit;
        }
    }

    /**
     * 远程（L2）缓存配置
     */
    public static class Remote {

        /**
         * 写入后过期时长（null 表示沿用 spring.cache.redis.time-to-live 的 Spring Cache 配置，
         * 显式设置后覆盖该配置）
         */
        private Duration expireAfterWrite;

        /**
         * 是否缓存空值（缓存空值可防止缓存穿透）
         */
        private boolean cacheNullValues = true;

        public Duration getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public boolean isCacheNullValues() {
            return cacheNullValues;
        }

        public void setCacheNullValues(boolean cacheNullValues) {
            this.cacheNullValues = cacheNullValues;
        }
    }

    /**
     * 单个 cacheName 的覆盖配置（未设置的项继承全局配置）
     */
    public static class CacheSpec {

        /**
         * 本地（L1）覆盖配置
         */
        private Local local;

        /**
         * 远程（L2）覆盖配置
         */
        private Remote remote;

        public Local getLocal() {
            return local;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public Remote getRemote() {
            return remote;
        }

        public void setRemote(Remote remote) {
            this.remote = remote;
        }
    }
}
