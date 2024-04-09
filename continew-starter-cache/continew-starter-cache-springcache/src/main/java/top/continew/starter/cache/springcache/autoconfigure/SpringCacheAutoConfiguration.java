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

package top.continew.starter.cache.springcache.autoconfigure;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

import java.util.Map;

/**
 * Spring Cache 自动配置
 *
 * @author Charles7c
 * @since 1.2.0
 */
@AutoConfiguration
@PropertySource(value = "classpath:default-cache-springcache.yml", factory = GeneralPropertySourceFactory.class)
public class SpringCacheAutoConfiguration implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SpringCacheAutoConfiguration.class);

    private final ObjectMapper objectMapper;

    public SpringCacheAutoConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Redis 缓存配置
     *
     * <p>解决 Spring Cache（@Cacheable）Jackson 解析缓存乱码问题</p>
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        ObjectMapper objectMapperCopy = objectMapper.copy();
        objectMapperCopy.activateDefaultTyping(objectMapperCopy
            .getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapperCopy)));
        CacheProperties.Redis redisCacheProperties = cacheProperties.getRedis();
        if (null != redisCacheProperties.getTimeToLive()) {
            redisCacheConfiguration = redisCacheConfiguration.entryTtl(redisCacheProperties.getTimeToLive());
        }
        if (!redisCacheProperties.isCacheNullValues()) {
            redisCacheConfiguration = redisCacheConfiguration.disableCachingNullValues();
        }
        return redisCacheConfiguration;
    }

    /**
     * 自定义缓存 key 生成策略
     *
     * <p>
     * 如果 @Cacheable 不指定 key，则默认使用该策略
     * </p>
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            String key = CharSequenceUtil.toUnderlineCase(method.getName()).toUpperCase();
            Map<String, Object> paramMap = MapUtil.newHashMap(params.length);
            for (int i = 0; i < params.length; i++) {
                paramMap.put(String.valueOf(i), params[i]);
            }
            return "%s:%s".formatted(key, DigestUtil.sha256Hex(JSONUtil.toJsonStr(paramMap)));
        };
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Spring Cache' completed initialization.");
    }
}
