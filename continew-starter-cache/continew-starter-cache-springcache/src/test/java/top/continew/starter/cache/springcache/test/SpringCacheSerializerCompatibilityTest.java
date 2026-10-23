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

package top.continew.starter.cache.springcache.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Cache 序列化器存量数据兼容测试
 *
 * <p>
 * 3.0.0 起缓存值序列化由 Jackson 2 的 {@code GenericJackson2JsonRedisSerializer}（已废弃）切换为
 * Jackson 3 的 {@code GenericJacksonJsonRedisSerializer}。两个版本的多态类型信息均以
 * {@code @class} 属性编码，本测试验证升级后可正常读取 2.x 时期写入的存量缓存。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SuppressWarnings({"deprecation", "removal"})
class SpringCacheSerializerCompatibilityTest {

    /**
     * 模拟 @Cacheable 缓存值的样本模型
     */
    static class CachedValue {

        private String username;
        private Long userId;
        private LocalDateTime createTime;
        private List<String> roles;

        CachedValue() {
        }

        CachedValue(String username, Long userId, LocalDateTime createTime, List<String> roles) {
            this.username = username;
            this.userId = userId;
            this.createTime = createTime;
            this.roles = roles;
        }

        public String getUsername() {
            return username;
        }

        public Long getUserId() {
            return userId;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public List<String> getRoles() {
            return roles;
        }
    }

    private static CachedValue sample() {
        return new CachedValue("admin", 1L, LocalDateTime.of(2026, 10, 23, 8, 0, 0), List
            .of("admin", "user"));
    }

    /**
     * 构建 2.x 版本的缓存值序列化器（模拟 SpringCacheAutoConfiguration 旧实现）
     *
     * @return Jackson 2 序列化器
     */
    private static com.fasterxml.jackson.databind.ObjectMapper legacyMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
            com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    /**
     * 构建 3.0.0 版本的缓存值序列化器
     *
     * @return Jackson 3 序列化器
     */
    private static GenericJacksonJsonRedisSerializer newSerializer() {
        return GenericJacksonJsonRedisSerializer
            .builder(tools.jackson.databind.json.JsonMapper::builder)
            .enableUnsafeDefaultTyping()
            .build();
    }

    private static void assertSameValue(Object actual, CachedValue expected) {
        CachedValue value = assertInstanceOf(CachedValue.class, actual);
        assertEquals(expected.username, value.username);
        assertEquals(expected.userId, value.userId);
        assertEquals(expected.roles, value.roles);
        assertEquals(expected.createTime, value.createTime);
    }

    @Test
    @DisplayName("Jackson 3 序列化器可读取 Jackson 2 写入的存量缓存")
    void shouldReadLegacyCacheValue() {
        org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer legacy =
            new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer(
                legacyMapper());
        byte[] legacyBytes = legacy.serialize(sample());
        Object decoded = newSerializer().deserialize(legacyBytes);
        assertSameValue(decoded, sample());
    }

    @Test
    @DisplayName("Jackson 3 序列化器序列化与反序列化自洽")
    void shouldRoundTripWithJackson3Serializer() {
        GenericJacksonJsonRedisSerializer serializer = newSerializer();
        byte[] bytes = serializer.serialize(sample());
        Object decoded = serializer.deserialize(bytes);
        assertSameValue(decoded, sample());
    }
}
