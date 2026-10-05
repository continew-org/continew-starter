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

import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.exception.RefreshTokenException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Refresh Token 编解码器。
 *
 * <p>令牌格式固定为 {@code sessionId.secret}。sessionId 只负责定位服务端会话，
 * secret 才是凭证；服务端只保存 secret 的 HMAC-SHA256 指纹。指纹密钥与快照加密密钥
 * 通过 HKDF-SHA256 从服务端密钥派生，两个用途的密钥相互独立。</p>
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
public class RefreshTokenCodec {

    private static final int SESSION_ID_BYTES = 16;
    private static final int SECRET_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int MAX_TOKEN_LENGTH = 96;
    private static final byte[] ENCRYPTION_AAD =
        "continew-refresh-rotation-v1".getBytes(StandardCharsets.UTF_8);
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{22}");
    private static final Pattern SECRET_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenProperties properties;

    public RefreshTokenCodec(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成 128 bit 随机会话 ID。
     *
     * @return 随机生成的会话 ID
     */
    public String newSessionId() {
        return this.randomBase64Url(SESSION_ID_BYTES);
    }

    /**
     * 为指定 Session 生成 256 bit Refresh Token。
     *
     * @param sessionId 会话 ID
     * @return 新签发的令牌及指纹
     * @throws IllegalArgumentException 会话 ID 格式无效
     */
    public IssuedToken issue(String sessionId) {
        if (!SESSION_ID_PATTERN.matcher(sessionId).matches()) {
            throw new IllegalArgumentException("Refresh Session ID 格式无效");
        }
        String secret = this.randomBase64Url(SECRET_BYTES);
        String rawToken = sessionId + "." + secret;
        return new IssuedToken(rawToken, sessionId, this.fingerprint(rawToken));
    }

    /**
     * 严格解析 Refresh Token，并计算整令牌（含会话绑定）的指纹。
     *
     * @param rawToken 客户端提交的明文 Refresh Token
     * @return 解析结果，不向调用方暴露 secret
     * @throws RefreshTokenException 令牌为空、超长或格式非法
     */
    public ParsedToken parse(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > MAX_TOKEN_LENGTH) {
            throw this.invalidToken();
        }
        int separator = rawToken.indexOf('.');
        if (separator <= 0 || separator != rawToken.lastIndexOf('.')) {
            throw this.invalidToken();
        }
        String sessionId = rawToken.substring(0, separator);
        String secret = rawToken.substring(separator + 1);
        if (!SESSION_ID_PATTERN.matcher(sessionId).matches()
            || !SECRET_PATTERN.matcher(secret).matches()) {
            throw this.invalidToken();
        }
        return new ParsedToken(rawToken, sessionId, this.fingerprint(rawToken));
    }

    /**
     * 对任意 bearer 凭证生成不可逆、带服务端密钥的稳定指纹。
     *
     * <p>Refresh Token 的指纹输入是完整令牌（{@code sessionId.secret}）而非单独的
     * secret：指纹与所属会话绑定后，拼接他人 sessionId 与自己的 secret 无法命中任何
     * 会话指纹或轮换快照键，跨会话凭证混淆在第一道门禁即被拒绝。</p>
     *
     * @param credential bearer 凭证
     * @return 不可逆指纹
     */
    public String fingerprint(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("凭证不能为空");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(this.deriveKey("fingerprint"), HMAC_SHA256));
            byte[] digest = mac.doFinal(credential.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("无法计算 Refresh Token 指纹", e);
        }
    }

    /**
     * 使用常量时间比较两个指纹。
     *
     * @param actualFingerprint   实际计算得到的指纹
     * @param expectedFingerprint 期望的指纹
     * @return 两者一致返回 {@code true}；任一为 {@code null} 返回 {@code false}
     */
    public boolean matches(String actualFingerprint, String expectedFingerprint) {
        if (actualFingerprint == null || expectedFingerprint == null) {
            return false;
        }
        return MessageDigest.isEqual(actualFingerprint.getBytes(StandardCharsets.US_ASCII),
            expectedFingerprint.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * 使用 AES-256-GCM 加密短时轮换快照。
     *
     * @param value 待加密的明文快照
     * @return Base64Url 编码的密文，随机 IV 前置
     * @throws IllegalArgumentException 待加密内容为空
     */
    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("待加密凭证不能为空");
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(this.deriveKey("encryption"), "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(ENCRYPTION_AAD);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("无法加密 Refresh Token 轮换快照", e);
        }
    }

    /**
     * 解密并校验 AES-GCM 轮换快照。
     *
     * @param value Base64Url 编码的密文
     * @return 解密后的明文快照
     * @throws RefreshTokenException 密文为空、格式非法或完整性校验失败
     */
    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            throw this.invalidToken();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(value);
            if (payload.length <= GCM_IV_BYTES) {
                throw this.invalidToken();
            }
            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] encrypted = new byte[payload.length - GCM_IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(this.deriveKey("encryption"), "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(ENCRYPTION_AAD);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw this.invalidToken();
        }
    }

    /**
     * 通过 HKDF-SHA256（RFC 5869）从服务端密钥派生指定用途的密钥。
     *
     * <p>指纹密钥与快照加密密钥使用不同 info（用途标签）派生，互不相关；单轮哈希
     * 拼接式派生无法隔离用途，且对低熵密钥的暴力破解成本更低。</p>
     *
     * @param purpose 密钥用途标签（fingerprint / encryption）
     * @return 32 字节派生密钥
     */
    private byte[] deriveKey(String purpose) {
        try {
            // Extract：PRK = HMAC-Hash(salt, IKM)，salt 取用途标签以隔离用途
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(purpose.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] pseudoRandomKey = mac.doFinal(properties.getSecret()
                .getBytes(StandardCharsets.UTF_8));
            // Expand：OKM = HMAC-Hash(PRK, info || 0x01)，info 为固定上下文
            mac.init(new SecretKeySpec(pseudoRandomKey, HMAC_SHA256));
            mac.update("continew-refresh-token/v1/".getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0x01);
            return mac.doFinal();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("无法派生 Refresh Token 密钥", e);
        }
    }

    private String randomBase64Url(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private RefreshTokenException invalidToken() {
        return RefreshTokenException.unauthorized("登录状态已失效，请重新登录");
    }

    /**
     * 新签发的 Refresh Token
     *
     * @param rawToken    完整令牌文本（sessionId.secret）
     * @param sessionId   会话 ID
     * @param fingerprint secret 的 HMAC-SHA256 指纹
     */
    public record IssuedToken(String rawToken, String sessionId, String fingerprint) {
    }

    /**
     * 已解析的 Refresh Token；不向调用方暴露 secret
     *
     * @param rawToken    完整令牌文本（sessionId.secret）
     * @param sessionId   会话 ID
     * @param fingerprint secret 的 HMAC-SHA256 指纹
     */
    public record ParsedToken(String rawToken, String sessionId, String fingerprint) {
    }
}
