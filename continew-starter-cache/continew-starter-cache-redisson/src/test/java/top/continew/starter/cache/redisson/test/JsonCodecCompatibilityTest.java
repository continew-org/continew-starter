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

package top.continew.starter.cache.redisson.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.client.handler.State;
import org.redisson.codec.JsonJackson3Codec;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Redisson JsonCodec 存量数据兼容测试
 *
 * <p>
 * 3.0.0 起 Redis 存储序列化由 Jackson 2（JsonJacksonCodec）切换为 Jackson 3（JsonJackson3Codec）。
 * Jackson 3 版 codec 对传入的 ObjectMapper 不会追加多态类型信息（2.x 版会自动开启 {@code @class}），
 * 因此生产构造需显式开启，本测试按 RedissonAutoConfiguration 的生产构造验证带类型往返。
 * </p>
 * <p>
 * 存量兼容结论（实测）：Jackson 2 为防止 long/int 精度问题将 Long 写为 WRAPPER_ARRAY
 * （如 {@code ["java.lang.Long",7]}），Jackson 3 无法读取该编码，此为 2.x 存量数据不兼容的主因，
 * 升级后需清空 Redis 存量业务键（或等待其过期）。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
class JsonCodecCompatibilityTest {

    /**
     * 构建 Jackson 2 codec（模拟 2.x 版本行为：传入容器 ObjectMapper，codec 内部追加默认多态类型信息）
     *
     * @return {@link org.redisson.codec.JsonJacksonCodec} Jackson 2 codec
     */
    private static org.redisson.codec.JsonJacksonCodec legacyCodec() {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new org.redisson.codec.JsonJacksonCodec(mapper);
    }

    /**
     * 按生产构造构建 Jackson 3 codec（与 RedissonAutoConfiguration 保持一致）
     *
     * @return {@link JsonJackson3Codec} Jackson 3 codec
     */
    private static JsonJackson3Codec productionCodec() {
        ObjectMapper codecMapper = JsonMapper.builder()
            .build()
            .rebuild()
            .activateDefaultTypingAsProperty(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(), DefaultTyping.NON_FINAL, "@class")
            .build();
        return new JsonJackson3Codec(codecMapper);
    }

    private static void assertDecodedAsPayload(Object actual) {
        Payload payload = assertInstanceOf(Payload.class, actual);
        assertEquals(7L, payload.userId);
        assertEquals("alice", payload.username);
        assertEquals(LocalDateTime.of(2026, 10, 23, 8, 0, 0), payload.loginTime);
        assertEquals(List.of("admin", "user"), payload.roles);
    }

    @Test
    @DisplayName("生产构造 codec 序列化携带 @class 且往返类型正确")
    void shouldRoundTripWithProductionCodec() throws Exception {
        JsonJackson3Codec codec = productionCodec();
        Payload source = new Payload();
        ByteBuf bytes = codec.getValueEncoder().encode(source);
        String wire = new String(ByteBufUtil.getBytes(bytes));
        assertTrue(wire.contains("\"@class\""), wire);
        Object decoded = codec.getValueDecoder()
            .decode(Unpooled.wrappedBuffer(ByteBufUtil.getBytes(bytes)), new State());
        assertDecodedAsPayload(decoded);
    }

    @Test
    @DisplayName("生产构造 codec 无法读取 Jackson 2 存量数据（Long 的 WRAPPER_ARRAY 为不兼容主因）")
    void shouldFailToReadLegacyPayloadWithProductionCodec() throws Exception {
        org.redisson.codec.JsonJacksonCodec legacyCodec = legacyCodec();
        JsonJackson3Codec newCodec = productionCodec();
        ByteBuf legacyBytes = legacyCodec.getValueEncoder().encode(new Payload());
        assertThrows(JacksonException.class, () -> newCodec
            .getValueDecoder()
            .decode(Unpooled.wrappedBuffer(ByteBufUtil.getBytes(legacyBytes)), new State()));
    }

    /**
     * 模拟 Sa-Token 会话等业务对象的样本模型
     */
    static class Payload {

        private String username = "alice";
        private Long userId = 7L;
        private LocalDateTime loginTime = LocalDateTime.of(2026, 10, 23, 8, 0, 0);
        private List<String> roles = List.of("admin", "user");

        public String getUsername() {
            return username;
        }

        public Long getUserId() {
            return userId;
        }

        public LocalDateTime getLoginTime() {
            return loginTime;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
