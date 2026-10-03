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

package top.continew.starter.auth.refresh.token.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec.IssuedToken;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec.ParsedToken;
import top.continew.starter.auth.refresh.token.exception.RefreshTokenException;
import top.continew.starter.core.exception.BusinessException;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Refresh Token 编解码安全边界测试。
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
class RefreshTokenCodecTest {

    private RefreshTokenCodec codec;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setSecret("test-only-refresh-token-secret-with-32-bytes");
        codec = new RefreshTokenCodec(properties);
    }

    @Test
    void shouldIssueSessionBoundToken() {
        String sessionId = codec.newSessionId();
        IssuedToken issued = codec.issue(sessionId);
        ParsedToken parsed = codec.parse(issued.rawToken());

        assertEquals(sessionId, parsed.sessionId());
        assertEquals(66, issued.rawToken().length());
        assertTrue(codec.matches(issued.fingerprint(), parsed.fingerprint()));
        assertFalse(issued.rawToken().contains(issued.fingerprint()));
    }

    @Test
    void shouldGenerateIndependentSecretsForSameSession() {
        String sessionId = codec.newSessionId();
        IssuedToken first = codec.issue(sessionId);
        IssuedToken second = codec.issue(sessionId);

        assertNotEquals(first.rawToken(), second.rawToken());
        assertNotEquals(first.fingerprint(), second.fingerprint());
    }

    @Test
    void shouldRejectMalformedToken() {
        RefreshTokenException exception = assertThrows(RefreshTokenException.class,
            () -> codec.parse("missing-separator"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertThrows(BusinessException.class, () -> codec.parse("a.b.c"));
        assertThrows(BusinessException.class, () -> codec.parse("../unsafe.secret"));
    }

    @Test
    void shouldEncryptWithRandomIvAndDetectTampering() {
        String value = codec.issue(codec.newSessionId()).rawToken();
        String first = codec.encrypt(value);
        String second = codec.encrypt(value);

        assertNotEquals(first, second);
        assertEquals(value, codec.decrypt(first));
        byte[] tamperedPayload = Base64.getUrlDecoder().decode(first);
        tamperedPayload[tamperedPayload.length - 1] ^= 1;
        String tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedPayload);
        assertThrows(BusinessException.class, () -> codec.decrypt(tampered));
    }

    @Test
    void shouldIsolateDerivedKeysByServerSecret() {
        RefreshTokenProperties otherProperties = new RefreshTokenProperties();
        otherProperties.setSecret("another-only-refresh-token-secret-32-bytes!");
        RefreshTokenCodec otherCodec = new RefreshTokenCodec(otherProperties);

        IssuedToken issued = codec.issue(codec.newSessionId());
        ParsedToken parsed = codec.parse(issued.rawToken());

        // 同一凭证在不同服务端密钥下的指纹必须不同：Redis 泄露的指纹无法在其他部署复用
        assertNotEquals(parsed.fingerprint(), otherCodec.fingerprint(issued.rawToken()));
    }
}
