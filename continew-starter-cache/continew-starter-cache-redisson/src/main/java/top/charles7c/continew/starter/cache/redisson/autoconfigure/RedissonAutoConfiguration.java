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

package top.charles7c.continew.starter.cache.redisson.autoconfigure;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.charles7c.continew.starter.core.constant.StringConsts;

import java.util.List;

/**
 * Redisson 自动配置
 *
 * @author gengwei.zheng（<a href="https://gitee.com/herodotus/dante-engine">Dante Engine</a>）
 * @author Charles7c
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.data.redisson", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonAutoConfiguration {

    private final RedissonProperties properties;
    private final RedisProperties redisProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public RedissonAutoConfigurationCustomizer redissonAutoConfigurationCustomizer() {
        return config -> {
            RedissonProperties.Mode mode = properties.getMode();
            String protocol = redisProperties.getSsl().isEnabled() ? "rediss://" : "redis://";
            switch (mode) {
                case CLUSTER -> {
                    ClusterServersConfig clusterServersConfig = config.useClusterServers();
                    ClusterServersConfig customClusterServersConfig = properties.getClusterServersConfig();
                    if (null != customClusterServersConfig) {
                        BeanUtil.copyProperties(customClusterServersConfig, clusterServersConfig);
                        clusterServersConfig.setNodeAddresses(customClusterServersConfig.getNodeAddresses());
                    }
                    // 下方配置如果为空，则使用 Redis 的配置
                    if (CollUtil.isEmpty(clusterServersConfig.getNodeAddresses())) {
                        List<String> nodeList = redisProperties.getCluster().getNodes();
                        nodeList.stream().map(node -> protocol + node).forEach(clusterServersConfig::addNodeAddress);
                    }
                    if (StrUtil.isBlank(clusterServersConfig.getPassword())) {
                        clusterServersConfig.setPassword(redisProperties.getPassword());
                    }
                }
                case SENTINEL -> {
                    SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
                    SentinelServersConfig customSentinelServersConfig = properties.getSentinelServersConfig();
                    if (null != customSentinelServersConfig) {
                        BeanUtil.copyProperties(customSentinelServersConfig, sentinelServersConfig);
                        sentinelServersConfig.setSentinelAddresses(customSentinelServersConfig.getSentinelAddresses());
                    }
                    // 下方配置如果为空，则使用 Redis 的配置
                    if (CollUtil.isEmpty(sentinelServersConfig.getSentinelAddresses())) {
                        List<String> nodeList = redisProperties.getSentinel().getNodes();
                        nodeList.stream().map(node -> protocol + node).forEach(sentinelServersConfig::addSentinelAddress);
                    }
                    if (StrUtil.isBlank(sentinelServersConfig.getPassword())) {
                        sentinelServersConfig.setPassword(redisProperties.getPassword());
                    }
                    if (StrUtil.isBlank(sentinelServersConfig.getMasterName())) {
                        sentinelServersConfig.setMasterName(redisProperties.getSentinel().getMaster());
                    }
                }
                default -> {
                    SingleServerConfig singleServerConfig = config.useSingleServer();
                    SingleServerConfig customSingleServerConfig = properties.getSingleServerConfig();
                    if (null != customSingleServerConfig) {
                        BeanUtil.copyProperties(properties.getSingleServerConfig(), singleServerConfig);
                    }
                    // 下方配置如果为空，则使用 Redis 的配置
                    singleServerConfig.setDatabase(redisProperties.getDatabase());
                    if (StrUtil.isBlank(singleServerConfig.getPassword())) {
                        singleServerConfig.setPassword(redisProperties.getPassword());
                    }
                    if (StrUtil.isBlank(singleServerConfig.getAddress())) {
                        singleServerConfig.setAddress(protocol + redisProperties.getHost() + StringConsts.COLON + redisProperties.getPort());
                    }
                }
            }
            // Jackson 处理
            config.setCodec(new JsonJacksonCodec(objectMapper));
            log.info("[ContiNew Starter] - Auto Configuration 'Redisson' completed initialization.");
        };
    }
}
