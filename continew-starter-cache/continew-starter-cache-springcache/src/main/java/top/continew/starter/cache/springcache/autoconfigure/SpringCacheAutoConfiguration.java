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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonObjectWriter;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Spring Cache 自动配置
 *
 * @author Charles7c
 * @author KAI
 * @since 1.2.0
 */
@AutoConfiguration
@EnableConfigurationProperties(CacheProperties.class)
@PropertySource(value = "classpath:default-cache-springcache.yml",
    factory = GeneralPropertySourceFactory.class)
public class SpringCacheAutoConfiguration implements CachingConfigurer {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(SpringCacheAutoConfiguration.class);

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
        // 基于容器 ObjectMapper 重建并启用默认多态类型信息，与 2.x GenericJackson2JsonRedisSerializer 行为保持一致。
        // 写入侧以 Object 为声明类型（见 RootTypedWriter），保证根值携带多态类型 id，与读取侧对称
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer
            .builder(objectMapper::rebuild)
            .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build())
            .writer(new RootTypedWriter())
            .build();
        RedisCacheConfiguration redisCacheConfiguration =
            RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(serializer));
        CacheProperties.Redis redisCacheProperties = cacheProperties.getRedis();
        Duration timeToLive = redisCacheProperties.getTimeToLive();
        if (timeToLive != null) {
            redisCacheConfiguration =
                redisCacheConfiguration.entryTtl(Objects.requireNonNull(timeToLive));
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
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'Spring Cache' completed initialization.");
    }

    /**
     * 根值类型标注写入器：固定以 Object 为声明类型序列化，并为 final Map 实现做浅拷贝
     *
     * <p>
     * 默认的 {@code writeValueAsBytes} 以运行时类型为声明类型，而 Spring Data Redis 4 的
     * 默认类型解析只对非 final 的非 JDK 类型写入类型 id，Long 与 {@code List.of()} /
     * {@code Stream.toList()} 产物会被写成裸值，读取侧却按带类型 id 的格式解析，
     * 导致 int 范围 Long 退化为 Integer、不可变集合反序列化直接失败。
     * 固定以 Object 声明后根值全部携带类型 id，与读取侧对称。
     * </p>
     * <p>
     * Jackson 3 无法剥离注入到不可变 Map 内容中的类型 id 属性，读回时 {@code @class} 会
     * 混入 Map 内容造成静默污染，因此 final Map 根值先浅拷贝为 LinkedHashMap，使类型 id
     * 挂在可正确反序列化的实现类上；读回值与原值按 Map 语义（内容与容量）等价。
     * </p>
     */
    private static final class RootTypedWriter implements JacksonObjectWriter {

        /**
         * 缓存的 ObjectWriter（ObjectWriter 不可变且线程安全，声明类型固定为 Object，可复用）
         */
        private volatile ObjectWriter objectWriter;

        @Override
        public byte[] write(ObjectMapper mapper, Object value) {
            ObjectWriter writer = this.objectWriter;
            if (writer == null) {
                writer = mapper.writerFor(Object.class);
                this.objectWriter = writer;
            }
            return writer.writeValueAsBytes(copyFinalMap(value));
        }

        /**
         * JDK 不可变/包装 Map 实现根值浅拷贝为 LinkedHashMap，避免类型 id 混入读回内容
         *
         * @param value 根值
         * @return 可携带类型 id 的等价根值
         */
        private static Object copyFinalMap(Object value) {
            if (value instanceof Map<?, ?> map && isUnrecoverableMap(value.getClass())) {
                return new LinkedHashMap<>(map);
            }
            return value;
        }

        /**
         * 判断 Map 实现类是否属于无法被读取侧正确恢复类型 id 的 JDK 内部实现
         *
         * <p>
         * Jackson 3 读取侧对 JDK 不可变 Map（{@code ImmutableCollections$*}）与包装 Map
         * （{@code Collections$*}）无法剥离注入到 Map 内容中的类型 id 属性；可变 Map
         * （HashMap 等）可正常恢复。用户自定义 Map 子类（含 final 子类）不受影响，不拷贝。
         * </p>
         *
         * @param type Map 实现类
         * @return 是否需要拷贝
         */
        private static boolean isUnrecoverableMap(Class<?> type) {
            String name = type.getName();
            if (!name.startsWith("java.")) {
                return false;
            }
            return Modifier.isFinal(type.getModifiers())
                || name.startsWith("java.util.Collections$")
                || name.startsWith("java.util.ImmutableCollections$");
        }
    }
}
