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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.StringValueResolver;
import top.continew.starter.cache.multicache.aspect.MultiCacheRefreshAspect;
import top.continew.starter.cache.multicache.cache.CacheInvalidationPublisher;
import top.continew.starter.cache.multicache.cache.MultiLevelCacheManager;
import top.continew.starter.cache.multicache.util.CacheUtils;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

import jakarta.annotation.PostConstruct;

/**
 * 多级缓存自动配置（Caffeine L1 + Redis L2）
 *
 * @author Charles7c
 * @since 3.0.0
 */
@EnableCaching
@AutoConfiguration(
    beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@EnableConfigurationProperties(MultiLevelCacheProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.CACHE_MULTI, name = PropertiesConstants.ENABLED,
    havingValue = "true", matchIfMissing = true)
@PropertySource(value = "classpath:default-cache-multicache.yml",
    factory = GeneralPropertySourceFactory.class)
public class MultiCacheAutoConfiguration implements EmbeddedValueResolverAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiCacheAutoConfiguration.class);

    private StringValueResolver valueResolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.valueResolver = resolver;
    }

    /**
     * 多级缓存管理器（接管 Spring Cache）
     *
     * <p>Bean 名保持 {@code cacheManager}（Spring Cache 抽象的惯用名）；
     * 声明在 Boot 的 CacheAutoConfiguration 之前处理，确保本管理器确定性接管，
     * 而不是与 Boot 的 RedisCacheManager 竞争装配顺序。</p>
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public MultiLevelCacheManager cacheManager(RedisConnectionFactory connectionFactory,
        ObjectProvider<RedisCacheConfiguration> redisCacheConfiguration,
        MultiLevelCacheProperties properties,
        ObjectProvider<CacheInvalidationPublisher> invalidationPublisher,
        ObjectProvider<MultiLevelCacheConfigurer> configurerProvider) {
        MultiLevelCacheConfigurer configurer = configurerProvider.getIfAvailable();
        MultiLevelCacheManager cacheManager = new MultiLevelCacheManager(
            RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory),
            redisCacheConfiguration.getIfAvailable(RedisCacheConfiguration::defaultCacheConfig),
            properties, invalidationPublisher.getIfAvailable(),
            configurer == null ? null : configurer.getCacheNames());
        invalidationPublisher.ifAvailable(publisher -> publisher.setCacheManager(cacheManager));
        CacheUtils.setCacheManager(cacheManager);
        return cacheManager;
    }

    /**
     * L1 失效广播器 + 订阅容器（集群部署时保持各节点 L1 最终一致）
     */
    @Bean
    @ConditionalOnProperty(prefix = PropertiesConstants.CACHE_MULTI, name = "broadcast-enabled",
        havingValue = "true", matchIfMissing = true)
    public CacheInvalidationPublisher cacheInvalidationPublisher(
        StringRedisTemplate stringRedisTemplate,
        MultiLevelCacheProperties properties) {
        String topic = properties.getBroadcastTopic();
        if (valueResolver != null && topic != null) {
            topic = valueResolver.resolveStringValue(topic);
        }
        return new CacheInvalidationPublisher(stringRedisTemplate, topic);
    }

    @Bean
    @ConditionalOnProperty(prefix = PropertiesConstants.CACHE_MULTI, name = "broadcast-enabled",
        havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer cacheInvalidationListenerContainer(
        RedisConnectionFactory connectionFactory,
        CacheInvalidationPublisher publisher) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(publisher, new PatternTopic(publisher.getTopic()));
        return container;
    }

    /**
     * 多级缓存定时刷新切面（{@code @MultiCacheRefresh}）
     */
    @Bean
    @ConditionalOnMissingBean
    public MultiCacheRefreshAspect multiCacheRefreshAspect(CacheManager cacheManager) {
        return new MultiCacheRefreshAspect(cacheManager);
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'Multi Level Cache' completed initialization.");
    }
}
