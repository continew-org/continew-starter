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

import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.encrypt.util.EncryptUtils;

import java.nio.charset.StandardCharsets;

/**
 * {@link ResponseBodyEncryptWrapper} 响应体加密包装器测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ResponseBodyEncryptWrapperTest {

    private static final String SECRET_KEY_HEADER = "X-Api-Encrypt";

    private static String rsaPublicKey;
    private static String rsaPrivateKey;

    @BeforeAll
    static void initKeys() {
        RSA rsa = new RSA();
        rsaPublicKey = rsa.getPublicKeyBase64();
        rsaPrivateKey = rsa.getPrivateKeyBase64();
    }

    private ResponseBodyEncryptWrapper wrapper() {
        try {
            return new ResponseBodyEncryptWrapper(new MockHttpServletResponse());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void writerShouldBufferWrittenContent() throws Exception {
        ResponseBodyEncryptWrapper wrapper = wrapper();
        wrapper.getWriter().write("hello");
        assertThat(wrapper.getContent()).isEqualTo("hello");
    }

    @Test
    void outputStreamShouldBufferWrittenBytes() throws Exception {
        ResponseBodyEncryptWrapper wrapper = wrapper();
        wrapper.getOutputStream().write("hello".getBytes(StandardCharsets.UTF_8));
        // 覆盖区间写变体，与整体写走不同重载
        wrapper.getOutputStream().write(" world!".getBytes(StandardCharsets.UTF_8), 0, 6);
        assertThat(new String(wrapper.getResponseData(), StandardCharsets.UTF_8))
            .isEqualTo("hello world");
    }

    @Test
    void resetShouldClearFlushedBuffer() throws Exception {
        // getWriter 带缓冲，reset 只清已 flush 到字节流的内容
        ResponseBodyEncryptWrapper wrapper = wrapper();
        wrapper.getWriter().write("hello");
        wrapper.flushBuffer();
        wrapper.reset();
        assertThat(wrapper.getContent()).isEmpty();
    }

    @Test
    void flushBufferShouldNotThrow() {
        assertThatCode(() -> wrapper().flushBuffer()).doesNotThrowAnyException();
    }

    @Test
    void getEncryptContentShouldSetHeadersAndReturnDecipherableCipher() throws Exception {
        ResponseBodyEncryptWrapper wrapper = wrapper();
        wrapper.getWriter().write("hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String cipher = wrapper.getEncryptContent(response, rsaPublicKey, SECRET_KEY_HEADER);
        assertThat(response.getHeader(SECRET_KEY_HEADER)).isNotBlank();
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
            .isEqualTo(SECRET_KEY_HEADER);
        // 完整回环：响应头密钥经 RSA 私钥解出后，应能还原 AES 加密前的原始响应体
        String aesKey = EncryptUtils.decodeByBase64(EncryptUtils
            .decryptByRsa(response.getHeader(SECRET_KEY_HEADER), rsaPrivateKey));
        assertThat(EncryptUtils.decryptByAes(cipher, aesKey)).isEqualTo("hello");
    }

    @Test
    void asyncWriteFlagsShouldBeLockedFalse() throws Exception {
        // 锁定当前行为：同步加密输出流不支持异步写监听，就绪标记恒为 false
        assertThat(wrapper().getOutputStream().isReady()).isFalse();
    }
}
