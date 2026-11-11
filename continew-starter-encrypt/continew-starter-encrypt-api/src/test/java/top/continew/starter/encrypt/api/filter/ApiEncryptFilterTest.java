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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import top.continew.starter.core.util.SpringUtils;
import top.continew.starter.encrypt.api.annotation.ApiEncrypt;
import top.continew.starter.encrypt.api.autoconfigure.ApiEncryptProperties;
import top.continew.starter.encrypt.util.EncryptUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * {@link ApiEncryptFilter} API 加密过滤器测试。
 *
 * <p>以 mockStatic(SpringUtils) 提供注解处理器方法，覆盖请求包装、响应加密与
 * 各放行分支，并用真实 RSA/AES 密钥做密文回环验证。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ApiEncryptFilterTest {

    private static final String SECRET_KEY_HEADER = "X-Api-Encrypt";
    // AES 密钥长度必须为 16/24/32 位，样例密钥运行时按 32 位生成
    private static final String AES_KEY = RandomUtil.randomString(32);
    private static final String PLAIN_BODY = "{\"page\":1}";

    private static String rsaPublicKey;
    private static String rsaPrivateKey;
    private static String encryptedSecretKey;

    /**
     * 样例控制器：三个方法分别覆盖全加密、仅请求解密与无注解。
     */
    static class SampleController {

        @ApiEncrypt
        public String encryptAll() {
            return "ok";
        }

        @ApiEncrypt(response = false)
        public String encryptRequestOnly() {
            return "ok";
        }

        public String plain() {
            return "ok";
        }
    }

    @BeforeAll
    static void initKeys() {
        RSA rsa = new RSA();
        rsaPublicKey = rsa.getPublicKeyBase64();
        rsaPrivateKey = rsa.getPrivateKeyBase64();
        String secretKeyByBase64 = EncryptUtils.encodeByBase64(AES_KEY);
        encryptedSecretKey = rsa.encryptBase64(secretKeyByBase64, KeyType.PublicKey);
    }

    private ApiEncryptFilter filter() {
        ApiEncryptProperties properties = new ApiEncryptProperties();
        properties.setPublicKey(rsaPublicKey);
        properties.setPrivateKey(rsaPrivateKey);
        return new ApiEncryptFilter(properties);
    }

    /**
     * 链上下发的请求对象，由过滤器执行时填充。
     */
    private final AtomicReference<ServletRequest> chainRequest = new AtomicReference<>();

    /**
     * 构造模拟请求，携带合法的 RSA 加密密钥头与 AES 加密请求体。
     *
     * @param httpMethod     HTTP 方法
     * @param methodName     处理器方法名
     * @param withSecretKey  是否携带密钥头
     * @return 模拟请求
     */
    private MockHttpServletRequest request(String httpMethod, String methodName,
        boolean withSecretKey)
        throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest(httpMethod, "/test");
        if (withSecretKey) {
            request.addHeader(SECRET_KEY_HEADER, encryptedSecretKey);
        }
        String encryptedBody = EncryptUtils.encryptByAes(PLAIN_BODY, AES_KEY);
        request.setContent(encryptedBody.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /**
     * 执行过滤器，捕获链上实际下发的请求与响应对象。
     *
     * @param request         模拟请求
     * @param handlerMethod   处理器方法（null 表示无注解处理器）
     * @param chainBodyWriter 链内对下发响应的写入动作，可为 null
     * @return 最终响应与链上下发请求
     */
    private MockHttpServletResponse doFilter(MockHttpServletRequest request,
        HandlerMethod handlerMethod,
        BiConsumer<ServletRequest, ServletResponse> chainBodyWriter) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {
            chainRequest.set(req);
            if (chainBodyWriter != null) {
                chainBodyWriter.accept(req, resp);
            }
        };
        try (MockedStatic<SpringUtils> springUtils = mockStatic(SpringUtils.class)) {
            springUtils.when(() -> SpringUtils.getHandlerMethod(any()))
                .thenReturn(handlerMethod);
            filter().doFilter(request, response, chain);
        }
        return response;
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = SampleController.class.getMethod(methodName);
        return new HandlerMethod(new SampleController(), method);
    }

    @Test
    void getShouldNotWrapRequestOrResponse() throws Exception {
        MockHttpServletResponse response =
            doFilter(request("GET", "encryptAll", true), handlerMethod("encryptAll"), null);
        assertThat(response).isNotInstanceOf(ResponseBodyEncryptWrapper.class);
    }

    @Test
    void postWithSecretKeyShouldWrapRequest() throws Exception {
        // response=false 的注解方法：仅请求侧替换为解密包装器
        MockHttpServletRequest request = request("POST", "encryptRequestOnly", true);
        doFilter(request, handlerMethod("encryptRequestOnly"), null);
        assertThat(chainRequest.get()).isInstanceOf(RequestBodyDecryptWrapper.class);
    }

    @Test
    void putWithSecretKeyShouldWrapRequest() throws Exception {
        MockHttpServletRequest request = request("PUT", "encryptRequestOnly", true);
        doFilter(request, handlerMethod("encryptRequestOnly"), null);
        assertThat(chainRequest.get()).isInstanceOf(RequestBodyDecryptWrapper.class);
    }

    @Test
    void postWithoutSecretKeyShouldNotWrapRequest() throws Exception {
        MockHttpServletRequest request = request("POST", "encryptAll", false);
        doFilter(request, handlerMethod("encryptAll"), null);
        assertThat(chainRequest.get()).isNotInstanceOf(RequestBodyDecryptWrapper.class);
    }

    @Test
    void responseEncryptShouldWriteCipherAndExposeHeader() throws Exception {
        MockHttpServletRequest request = request("POST", "encryptAll", true);
        MockHttpServletResponse response =
            doFilter(request, handlerMethod("encryptAll"), (req, resp) -> {
                try {
                    ((ResponseBodyEncryptWrapper) resp).getWriter().write("ok");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        // 加密后原响应内容被密文替换，且密钥头经 RSA 公钥加密后随响应下发
        assertThat(response.getHeader(SECRET_KEY_HEADER)).isNotBlank();
        String aesKey = EncryptUtils.decodeByBase64(EncryptUtils
            .decryptByRsa(response.getHeader(SECRET_KEY_HEADER), rsaPrivateKey));
        assertThat(EncryptUtils.decryptByAes(response.getContentAsString(), aesKey))
            .isEqualTo("ok");
    }

    @Test
    void responseWithoutAnnotationShouldStayPlain() throws Exception {
        MockHttpServletRequest request = request("POST", "plain", true);
        MockHttpServletResponse response =
            doFilter(request, handlerMethod("plain"), (req, resp) -> {
                try {
                    resp.getWriter().write("ok");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        assertThat(response.getContentAsString()).isEqualTo("ok");
    }
}
