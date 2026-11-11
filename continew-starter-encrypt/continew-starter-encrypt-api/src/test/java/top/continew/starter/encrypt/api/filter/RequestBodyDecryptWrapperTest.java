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

package top.continew.starter.encrypt.api.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import top.continew.starter.encrypt.util.EncryptUtils;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

/**
 * {@link RequestBodyDecryptWrapper} 请求体解密包装器测试。
 *
 * <p>以真实 RSA/AES 密钥构造密文请求，覆盖「请求头取 RSA 密钥 → 解出 AES 密钥 →
 * 解密请求体」完整链路与包装器对外行为。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RequestBodyDecryptWrapperTest {

    // AES 密钥长度必须为 16/24/32 位，样例密钥运行时按 32 位生成
    private static final String AES_KEY = RandomUtil.randomString(32);
    private static final String PLAIN_BODY = "{\"username\":\"continew\"}";
    private static final String SECRET_KEY_HEADER = "X-Api-Encrypt";

    private static String rsaPrivateKey;
    private static String encryptedSecretKey;

    @BeforeAll
    static void initKeys() {
        RSA rsa = new RSA();
        rsaPrivateKey = rsa.getPrivateKeyBase64();
        // 模拟客户端：AES 密钥先 Base64 编码再经 RSA 公钥加密后放入请求头
        String secretKeyByBase64 = EncryptUtils.encodeByBase64(AES_KEY);
        encryptedSecretKey = rsa.encryptBase64(secretKeyByBase64, KeyType.PublicKey);
    }

    private RequestBodyDecryptWrapper wrapper() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SECRET_KEY_HEADER, encryptedSecretKey);
        request.setCharacterEncoding("UTF-8");
        String encryptedBody = EncryptUtils.encryptByAes(PLAIN_BODY, AES_KEY);
        request.setContent(encryptedBody.getBytes(StandardCharsets.UTF_8));
        try {
            return new RequestBodyDecryptWrapper(request, rsaPrivateKey, SECRET_KEY_HEADER);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void shouldDecryptBodyFromHeaderKey() throws Exception {
        RequestBodyDecryptWrapper wrapper = wrapper();
        try (BufferedReader reader = wrapper.getReader()) {
            assertThat(reader.readLine()).isEqualTo(PLAIN_BODY);
        }
    }

    @Test
    void contentLengthShouldExposeDecryptedLength() {
        RequestBodyDecryptWrapper wrapper = wrapper();
        assertThat(wrapper.getContentLength())
            .isEqualTo(PLAIN_BODY.getBytes(StandardCharsets.UTF_8).length);
        assertThat(wrapper.getContentLengthLong())
            .isEqualTo(PLAIN_BODY.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void contentTypeShouldBeForcedToJson() {
        assertThat(wrapper().getContentType()).isEqualTo("application/json");
    }

    @Test
    void inputStreamShouldReturnDecryptedBytes() throws Exception {
        RequestBodyDecryptWrapper wrapper = wrapper();
        assertThat(wrapper.getInputStream().available()).isEqualTo(PLAIN_BODY.length());
        byte[] body = wrapper.getInputStream().readAllBytes();
        assertThat(new String(body, StandardCharsets.UTF_8)).isEqualTo(PLAIN_BODY);
    }

    @Test
    void asyncReadFlagsShouldBeLockedFalse() throws Exception {
        // 锁定当前行为：同步解密输入流不支持异步读监听，就绪标记恒为 false
        assertThat(wrapper().getInputStream().isFinished()).isFalse();
        assertThat(wrapper().getInputStream().isReady()).isFalse();
        assertThatCode(() -> wrapper().getInputStream().setReadListener(null))
            .doesNotThrowAnyException();
    }
}
