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

package top.continew.starter.openapi.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.core.wrapper.RepeatReadRequestWrapper;
import top.continew.starter.openapi.audit.OpenApiSignAuditEvent;
import top.continew.starter.openapi.autoconfigure.OpenApiSignProperties;
import top.continew.starter.openapi.constant.OpenApiSignErrorCodes;
import top.continew.starter.openapi.context.OpenAppContext;
import top.continew.starter.openapi.context.OpenAppContextHolder;
import top.continew.starter.openapi.model.OpenApiCredential;
import top.continew.starter.openapi.protocol.ContentDigest;
import top.continew.starter.openapi.protocol.OpenApiSignProtocol;
import top.continew.starter.openapi.protocol.QueryCanonicalizer;
import top.continew.starter.openapi.protocol.SignatureVerifier;
import top.continew.starter.openapi.protocol.StringToSign;
import top.continew.starter.openapi.spi.DefaultReplayGuard;
import top.continew.starter.openapi.spi.ReplayGuard;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 开放接口验签过滤器测试：覆盖 11 个错误码出口与 1 个放行路径
 *
 * @author Charles7c
 * @since 3.1.0
 */
class OpenApiSignFilterTest {

    /**
     * 测试密钥（与设计文档 3.7 节披露的字面量一致，仅用于测试）
     */
    private static final String TEST_SECRET_KEY = "{{SECRET_c0c4usr9}}";

    private static final String ACCESS_KEY = "ak_example_0001";

    private static final String HOST = "api.example.com";

    private static final String PATH = "/open/api/order/create";

    private static final String NONCE = "aB3cD5eF7gH9jK2m000000";

    private final List<OpenApiSignAuditEvent> auditEvents = new ArrayList<>();

    /**
     * 放行路径：合法签名 + 授权允许，请求进入业务链，开放应用上下文在业务内可见且结束后清理
     */
    @Test
    void validRequestShouldPassThrough() throws Exception {
        OpenApiSignFilter filter =
            newFilter(new OpenApiCredential(1001L, TEST_SECRET_KEY, false, null), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<OpenAppContext> contextInChain = new AtomicReference<>();
        AtomicReference<HttpServletRequest> requestInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            contextInChain.set(OpenAppContextHolder.getContext());
            requestInChain.set((HttpServletRequest) req);
        };
        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(contextInChain.get()).isNotNull();
        assertThat(contextInChain.get().getAppId()).isEqualTo(1001L);
        assertThat(contextInChain.get().getAuthorizedApi()).isEqualTo("POST:" + PATH);
        assertThat(OpenAppContextHolder.getContext()).isNull();
        // 业务层读到的请求为可重复读包装器，且拿到同一份缓存字节
        assertThat(requestInChain.get()).isInstanceOf(RepeatReadRequestWrapper.class);
        assertThat(auditEvents).hasSize(1);
        assertThat(auditEvents.get(0).getEventType()).isEqualTo(OpenApiSignAuditEvent.SIGN_SUCCESS);
        assertThat(auditEvents.get(0).getAccessKey()).isEqualTo("ak_exa****0001");
        assertThat(auditEvents.get(0).getAppId()).isEqualTo(1001L);
    }

    /**
     * 头缺失（无 Authorization）按 E400 拒绝
     */
    @Test
    void missingAuthorizationShouldRejectWithHeaderInvalid() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        request.removeHeader(OpenApiSignProtocol.AUTHORIZATION_HEADER);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.SIGN_HEADER_INVALID.getCode());
    }

    /**
     * 重复 Authorization 头按 E400 拒绝，不取第一个
     */
    @Test
    void duplicatedAuthorizationHeaderShouldReject() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        request.addHeader(OpenApiSignProtocol.AUTHORIZATION_HEADER,
            "OCN1-HMAC-SHA256 Credential=x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    /**
     * Authorization 字段格式错（键名错误）按 E400 拒绝
     */
    @Test
    void malformedAuthorizationShouldReject() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        request.removeHeader(OpenApiSignProtocol.AUTHORIZATION_HEADER);
        request.addHeader(OpenApiSignProtocol.AUTHORIZATION_HEADER,
            "OCN1-HMAC-SHA256 Credential=" + ACCESS_KEY
                + ", SignedHeaders=host;x-ocn-content-digest, Timestamp=1730000000, Nonce=" + NONCE
                + ", Sign=broken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    /**
     * query 携带旧协议签名公共参数按 E400 拒绝（协议混用）
     */
    @Test
    void legacySignParamInQueryShouldReject() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), "sign=abc",
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    /**
     * 请求字符编码非 UTF-8 按 E400 拒绝
     */
    @Test
    void nonUtf8CharsetShouldReject() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        request.setCharacterEncoding("GBK");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    /**
     * multipart 请求按 E415 拒绝
     */
    @Test
    void multipartRequestShouldRejectWithUnsupported() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        request.setContentType("multipart/form-data; boundary=----boundary");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(415);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.MULTIPART_UNSUPPORTED.getCode());
    }

    /**
     * form 表单请求按 E415 FORM_UNSUPPORTED 拒绝（容器会重新编码表单，摘要契约无法成立）
     */
    @Test
    void formRequestShouldRejectWithUnsupported() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        request.setContentType("application/x-www-form-urlencoded");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(415);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.FORM_UNSUPPORTED.getCode());
    }

    /**
     * body 超过大小上限按 E413 拒绝
     */
    @Test
    void oversizedBodyShouldRejectWithTooLarge() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        byte[] oversizedBody = new byte[1048577];
        MockHttpServletRequest request = signedRequest("POST", PATH, oversizedBody, null,
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.BODY_TOO_LARGE.getCode());
    }

    /**
     * body 被篡改而摘要头未改，按 E403 BODY_DIGEST_MISMATCH 拒绝
     */
    @Test
    void tamperedBodyShouldRejectWithDigestMismatch() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        // 构造签名用原始 body，请求携带篡改后的 body，摘要头仍是原始 body 的摘要
        request
            .setContent("{\"amount\":999,\"currency\":\"CNY\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.BODY_DIGEST_MISMATCH.getCode());
    }

    /**
     * timestamp 超出窗口按 E401 SIGN_EXPIRED 拒绝
     */
    @Test
    void expiredTimestampShouldReject() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000 - 1300, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.SIGN_EXPIRED.getCode());
    }

    /**
     * 同一 nonce 第二次请求按 E409 NONCE_REPLAY 拒绝，且审计记录该拒绝
     */
    @Test
    void replayedNonceShouldReject() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        long timestampSeconds = System.currentTimeMillis() / 1000;
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(signedRequest("POST", PATH, body(), null, timestampSeconds, NONCE),
            firstResponse,
            new MockFilterChain());
        assertThat(firstResponse.getStatus()).isEqualTo(200);

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(signedRequest("POST", PATH, body(), null, timestampSeconds, NONCE),
            secondResponse,
            new MockFilterChain());
        assertThat(secondResponse.getStatus()).isEqualTo(409);
        assertThat(secondResponse.getContentAsString())
            .contains(OpenApiSignErrorCodes.NONCE_REPLAY.getCode());
        assertThat(auditEvents.get(1).getEventType()).isEqualTo(OpenApiSignAuditEvent.SIGN_DENIED);
    }

    /**
     * 占位失败的请求不触发释放：同一 nonce 重放两次均拒绝，首个合法请求的占位不被误删
     */
    @Test
    void replayedNonceShouldNotReleasePlaceholder() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        long timestampSeconds = System.currentTimeMillis() / 1000;
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(signedRequest("POST", PATH, body(), null, timestampSeconds, NONCE),
            firstResponse, new MockFilterChain());
        assertThat(firstResponse.getStatus()).isEqualTo(200);

        // 第二次重放按占位失败拒绝；若释放条件误判，此处会误删首个合法请求的占位
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(signedRequest("POST", PATH, body(), null, timestampSeconds, NONCE),
            secondResponse, new MockFilterChain());
        assertThat(secondResponse.getStatus()).isEqualTo(409);

        // 第三次仍拒绝，证明占位未被第二次请求删除
        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
        filter.doFilter(signedRequest("POST", PATH, body(), null, timestampSeconds, NONCE),
            thirdResponse, new MockFilterChain());
        assertThat(thirdResponse.getStatus()).isEqualTo(409);
        assertThat(thirdResponse.getContentAsString())
            .contains(OpenApiSignErrorCodes.NONCE_REPLAY.getCode());
    }

    /**
     * 占位成功但后续验签失败时释放占位，同一 nonce 换正确密钥重发可最终通过
     */
    @Test
    void failedVerificationShouldReleaseNonceForRetry() throws Exception {
        DefaultReplayGuard sharedGuard = new DefaultReplayGuard();
        long timestampSeconds = System.currentTimeMillis() / 1000;
        // 服务端密钥与签名密钥不一致：占位成功后 HMAC 验算失败，catch 释放占位
        OpenApiSignFilter wrongKeyFilter =
            newFilter(new OpenApiCredential(1001L, "another-secret-key", false, null), true,
                sharedGuard);
        MockHttpServletResponse failedResponse = new MockHttpServletResponse();
        wrongKeyFilter.doFilter(signedRequest("POST", PATH, body(), null, timestampSeconds, NONCE),
            failedResponse, new MockFilterChain());
        assertThat(failedResponse.getStatus()).isEqualTo(401);

        // 同一 nonce 携带正确签名重发：占位已被释放，可再次占位并通过验签
        OpenApiSignFilter correctFilter = newFilter(credential(), true, sharedGuard);
        MockHttpServletResponse retryResponse = new MockHttpServletResponse();
        correctFilter.doFilter(signedRequest("POST", PATH, body(), null, timestampSeconds, NONCE),
            retryResponse, new MockFilterChain());
        assertThat(retryResponse.getStatus()).isEqualTo(200);
    }

    /**
     * accessKey 查不到凭证按 E401 CREDENTIAL_INVALID 拒绝
     */
    @Test
    void unknownAccessKeyShouldRejectWithCredentialInvalid() throws Exception {
        OpenApiSignFilter filter = newFilter(null, true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.CREDENTIAL_INVALID.getCode());
    }

    /**
     * 应用禁用按 E403 APP_DISABLED 拒绝
     */
    @Test
    void disabledAppShouldReject() throws Exception {
        OpenApiSignFilter filter =
            newFilter(new OpenApiCredential(1001L, TEST_SECRET_KEY, true, null), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.APP_DISABLED.getCode());
    }

    /**
     * 应用过期按 E403 APP_EXPIRED 拒绝
     */
    @Test
    void expiredAppShouldReject() throws Exception {
        OpenApiSignFilter filter =
            newFilter(new OpenApiCredential(1001L, TEST_SECRET_KEY, false, LocalDateTime
                .now().minusDays(1)), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.APP_EXPIRED.getCode());
    }

    /**
     * 服务端密钥与签名密钥不一致按 E401 SIGNATURE_MISMATCH 拒绝
     */
    @Test
    void wrongSignatureShouldRejectWithMismatch() throws Exception {
        OpenApiSignFilter filter =
            newFilter(new OpenApiCredential(1001L, "another-secret-key", false, null),
                true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.SIGNATURE_MISMATCH.getCode());
    }

    /**
     * 接口不在应用清单按 E403 APP_NO_PERMISSION 拒绝，审计事件类型为 APP_DENIED
     */
    @Test
    void unauthorizedApiShouldRejectWithNoPermission() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), false);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
            .contains(OpenApiSignErrorCodes.APP_NO_PERMISSION.getCode());
        assertThat(auditEvents.get(0).getEventType()).isEqualTo(OpenApiSignAuditEvent.APP_DENIED);
    }

    /**
     * 摘要头缺失按 E400 拒绝
     */
    @Test
    void missingContentDigestHeaderShouldReject() throws Exception {
        OpenApiSignFilter filter = newFilter(credential(), true);
        MockHttpServletRequest request = signedRequest("POST", PATH, body(), null,
            System.currentTimeMillis() / 1000, NONCE);
        request.removeHeader(OpenApiSignProtocol.CONTENT_DIGEST_HEADER);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    private OpenApiCredential credential() {
        return new OpenApiCredential(1001L, TEST_SECRET_KEY, false, null);
    }

    private OpenApiSignFilter newFilter(OpenApiCredential credential, boolean authorized) {
        return newFilter(credential, authorized, new DefaultReplayGuard());
    }

    private OpenApiSignFilter newFilter(OpenApiCredential credential, boolean authorized,
        ReplayGuard replayGuard) {
        OpenApiSignProperties properties = new OpenApiSignProperties();
        return new OpenApiSignFilter(properties, accessKey -> Optional.ofNullable(credential),
            (appId, method, path) -> authorized, replayGuard, auditEvents::add);
    }

    private byte[] body() {
        return "{\"amount\":100,\"currency\":\"CNY\"}".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 构造携带完整签名头的请求（按协议动态签名）
     *
     * @param method           HTTP 方法
     * @param uri              请求路径
     * @param body             请求体（null 视为空）
     * @param queryString      原始 query（可为 null）
     * @param timestampSeconds 签名时间戳（Unix 秒）
     * @param nonce            随机串
     * @return 携带签名头的请求对象
     */
    private MockHttpServletRequest signedRequest(String method, String uri, byte[] body,
        String queryString,
        long timestampSeconds, String nonce) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        if (queryString != null) {
            request.setQueryString(queryString);
        }
        request.setContentType("application/json");
        byte[] bodyBytes = body == null ? new byte[0] : body;
        request.setContent(bodyBytes);
        String digest = ContentDigest.sha256Hex(bodyBytes);
        request.addHeader(OpenApiSignProtocol.HOST_HEADER, HOST);
        request.addHeader(OpenApiSignProtocol.CONTENT_DIGEST_HEADER,
            OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX
                + digest);
        String stringToSign = StringToSign.build(method, uri, canonicalQuery(queryString), HOST,
            OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX + digest, String.valueOf(timestampSeconds),
            nonce);
        String signature =
            HexFormat.of().formatHex(SignatureVerifier.hmacSha256(TEST_SECRET_KEY, stringToSign));
        request.addHeader(OpenApiSignProtocol.AUTHORIZATION_HEADER,
            "OCN1-HMAC-SHA256 Credential=" + ACCESS_KEY
                + ", SignedHeaders=" + OpenApiSignProtocol.SIGNED_HEADERS + ", Timestamp="
                + timestampSeconds
                + ", Nonce=" + nonce + ", Signature=" + signature);
        return request;
    }

    /**
     * 计算规范化 query；含旧协议公共参数等非法 query 时以空串签名（服务端会在规范化步骤先拒绝）
     *
     * @param queryString 原始 query
     * @return 规范化 query
     */
    private String canonicalQuery(String queryString) {
        try {
            return QueryCanonicalizer.canonicalize(queryString);
        } catch (Exception e) {
            return "";
        }
    }
}
