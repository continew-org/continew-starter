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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.client.handler.State;
import org.redisson.codec.JsonJackson3Codec;
import org.redisson.codec.JsonJacksonCodec;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Redisson JsonCodec 存量数据兼容测试
 *
 * <p>
 * 3.0.0 起 Redis 存储序列化由 Jackson 2（JsonJacksonCodec）切换为 Jackson 3（JsonJackson3Codec）。
 * 经实测两个版本的线格式对多态类型信息的编码不同：Jackson 2 对 {@code Long} 等最终类型写入
 * {@code WRAPPER_ARRAY}（如 {@code ["java.lang.Long",1]}），Jackson 3 仅对非最终类型写 {@code @class}
 * 属性。因此 JsonJackson3Codec 无法读取 2.x 时期写入的存量数据（反之亦然），升级后需要清空 Redis
 * 中的存量键（或等待其过期），详见升级文档。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
class JsonCodecCompatibilityTest {

    /**
     * 模拟 Sa-Token 会话等业务对象的样本模型
     */
    static class SessionPayload {

        private String username;
        private Long userId;
        private LocalDateTime loginTime;
        private List<String> roles;

        SessionPayload() {
        }

        SessionPayload(String username, Long userId, LocalDateTime loginTime, List<String> roles) {
            this.username = username;
            this.userId = userId;
            this.loginTime = loginTime;
            this.roles = roles;
        }

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

    private static SessionPayload sample() {
        return new SessionPayload("admin", 1L, LocalDateTime.of(2026, 10, 23, 8, 0, 0), List
            .of("admin", "user"));
    }

    /**
     * 构建 Jackson 2 codec（模拟 2.x 版本行为：传入容器 ObjectMapper，codec 内部追加默认多态类型信息）
     *
     * @return {@link JsonJacksonCodec} Jackson 2 codec
     */
    private static JsonJacksonCodec legacyCodec() {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JsonJacksonCodec(mapper);
    }

    private static void assertSamePayload(Object actual, SessionPayload expected) {
        SessionPayload payload = assertInstanceOf(SessionPayload.class, actual);
        assertEquals(expected.username, payload.username);
        assertEquals(expected.userId, payload.userId);
        assertEquals(expected.roles, payload.roles);
        assertEquals(expected.loginTime, payload.loginTime);
    }

    @Test
    @DisplayName("Jackson 3 Codec 序列化与反序列化自洽")
    void shouldRoundTripWithJackson3Codec() throws Exception {
        JsonJackson3Codec codec = new JsonJackson3Codec();
        SessionPayload source = sample();
        ByteBuf bytes = codec.getValueEncoder().encode(source);
        Object decoded = codec.getValueDecoder()
            .decode(Unpooled.wrappedBuffer(ByteBufUtil.getBytes(bytes)), new State());
        assertSamePayload(decoded, source);
    }

    @Test
    @DisplayName("Jackson 3 Codec 无法读取 Jackson 2 存量数据（升级需清理 Redis，已明确记录）")
    void shouldFailToReadLegacyPayloadWithJackson3Codec() throws Exception {
        JsonJacksonCodec legacyCodec = legacyCodec();
        JsonJackson3Codec newCodec = new JsonJackson3Codec();
        ByteBuf legacyBytes = legacyCodec.getValueEncoder().encode(sample());
        assertThrows(tools.jackson.databind.exc.MismatchedInputException.class, () -> newCodec
            .getValueDecoder()
            .decode(Unpooled.wrappedBuffer(ByteBufUtil.getBytes(legacyBytes)), new State()));
    }
}
