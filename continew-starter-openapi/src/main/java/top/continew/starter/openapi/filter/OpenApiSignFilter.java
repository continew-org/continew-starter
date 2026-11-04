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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.ServletUtils;
import top.continew.starter.core.wrapper.RepeatReadRequestWrapper;
import top.continew.starter.core.wrapper.RequestBodySizeExceededException;
import top.continew.starter.openapi.audit.OpenApiSignAuditEvent;
import top.continew.starter.openapi.audit.OpenApiSignAuditSink;
import top.continew.starter.openapi.autoconfigure.OpenApiSignProperties;
import top.continew.starter.openapi.constant.OpenApiSignErrorCodes;
import top.continew.starter.openapi.context.OpenAppContext;
import top.continew.starter.openapi.context.OpenAppContextHolder;
import top.continew.starter.openapi.exception.OpenApiSignException;
import top.continew.starter.openapi.model.OpenApiCredential;
import top.continew.starter.openapi.protocol.ContentDigest;
import top.continew.starter.openapi.protocol.OpenApiSignProtocol;
import top.continew.starter.openapi.protocol.QueryCanonicalizer;
import top.continew.starter.openapi.protocol.SignHeaderParser;
import top.continew.starter.openapi.protocol.SignatureVerifier;
import top.continew.starter.openapi.protocol.StringToSign;
import top.continew.starter.openapi.spi.AuthorizationChecker;
import top.continew.starter.openapi.spi.CredentialResolver;
import top.continew.starter.openapi.spi.ReplayGuard;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 开放接口验签过滤器
 *
 * <p>按固定顺序执行验签链路（任一步失败即短路返回，后续步骤不再执行）：头解析、编码校验、
 * query 规范化、multipart/form 判定与 body 大小校验、body 摘要比对、timestamp 窗口、
 * nonce 原子占位、凭证查询、应用状态、HMAC 验算、授权判定、审计、放行。nonce 占位成功后
 * 若后续步骤失败，删除占位键释放名额，保证接入方可重试；占位失败的请求未持有占位，不触发
 * 释放。验签通过后建立开放应用上下文并在 finally 中清理，请求体经
 * {@link RepeatReadRequestWrapper} 按 max-body-size 有界缓存后与业务层共用同一份字节。
 * 失败响应在 {@code doFilter} 之前写出，不进入 MVC 异常处理。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class OpenApiSignFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiSignFilter.class);

    /**
     * 链路追踪 MDC 键名（与 trace 模块约定一致，缺链路上下文时审计事件该项为空）
     */
    private static final String TRACE_ID_KEY = "traceId";

    private final OpenApiSignProperties properties;

    private final CredentialResolver credentialResolver;

    private final AuthorizationChecker authorizationChecker;

    private final ReplayGuard replayGuard;

    private final OpenApiSignAuditSink auditSink;

    public OpenApiSignFilter(OpenApiSignProperties properties,
        CredentialResolver credentialResolver,
        AuthorizationChecker authorizationChecker, ReplayGuard replayGuard,
        OpenApiSignAuditSink auditSink) {
        this.properties = properties;
        this.credentialResolver = credentialResolver;
        this.authorizationChecker = authorizationChecker;
        this.replayGuard = replayGuard;
        this.auditSink = auditSink;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
        throws ServletException,
        IOException {
        String requestId = UUID.randomUUID().toString();
        try {
            doVerify(request, response, filterChain, requestId);
        } finally {
            OpenAppContextHolder.remove();
        }
    }

    /**
     * 执行验签链路并放行
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链
     * @param requestId   请求关联 ID
     */
    private void doVerify(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain,
        String requestId) throws ServletException, IOException {
        String method = request.getMethod().toUpperCase();
        String path = resolvePath(request);
        String clientIp = ServletUtils.getClientIP(request);
        String traceId = MDC.get(TRACE_ID_KEY);
        RequestContext context = new RequestContext(method, path, clientIp, traceId, requestId);
        String accessKey = null;
        Long appId = null;
        String nonceKey = null;
        boolean nonceAcquired = false;
        try {
            SignHeaderParser.ParsedHeader header = parseHeaders(request);
            accessKey = header.getAccessKey();
            String host = requireValidHeader(request, OpenApiSignProtocol.HOST_HEADER);
            String contentDigestHeader =
                requireValidHeader(request, OpenApiSignProtocol.CONTENT_DIGEST_HEADER);
            checkCharset(request);

            String canonicalQuery = canonicalizeQuery(request);

            // multipart 与 form 须在包装前判定：multipart 不缓存原文，form 会被容器重新编码
            if (ServletUtils.isMultipart(request)) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.MULTIPART_UNSUPPORTED);
            }
            if (isFormRequest(request)) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.FORM_UNSUPPORTED);
            }
            // Content-Length 超限在读取流之前短路；chunked 等无长度请求由有界拷贝兜底
            if (request.getContentLengthLong() > properties.getSign().getMaxBodySize()) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.BODY_TOO_LARGE);
            }
            RepeatReadRequestWrapper wrappedRequest = wrapRequest(request);
            verifyBodyDigest(wrappedRequest, contentDigestHeader);

            checkTimestamp(header);
            nonceKey =
                OpenApiSignProtocol.NONCE_KEY_PREFIX + header.getAccessKey() + StringConstants.COLON
                    + header.getNonce();
            nonceAcquired = replayGuard.tryAcquire(nonceKey,
                Duration.ofSeconds(properties.getReplay().getNonceTtl()));
            if (!nonceAcquired) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.NONCE_REPLAY);
            }

            OpenApiCredential credential = resolveCredential(accessKey);
            appId = credential.getAppId();
            checkAppStatus(credential);

            String stringToSign = StringToSign.build(method, path, canonicalQuery, host,
                contentDigestHeader, header.getTimestamp(), header.getNonce());
            if (!SignatureVerifier.verify(credential.getSecretKey(), stringToSign,
                header.getSignature())) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.SIGNATURE_MISMATCH);
            }

            OpenAppContextHolder.setContext(new OpenAppContext(appId, accessKey, method
                + StringConstants.COLON + path, requestId));
            if (!authorizationChecker.check(appId, method, path)) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.APP_NO_PERMISSION);
            }
            recordAudit(context, appId, accessKey,
                new AuditOutcome(OpenApiSignAuditEvent.SIGN_SUCCESS, null));
            if (LOGGER.isDebugEnabled()) {
                // 只记摘要与字节数，请求体原文不进日志
                LOGGER.debug(
                    "[ContiNew Starter] - Open API signature verified, method: {}, path: {}, appId: {}, bodyDigest: {}, bodySize: {}",
                    method, path, appId, ContentDigest.sha256Hex(wrappedRequest.getCachedContent()
                        .toByteArray()),
                    wrappedRequest.getCachedContent().size());
            }
            filterChain.doFilter(wrappedRequest, response);
        } catch (OpenApiSignException e) {
            // 仅释放本次成功占位的 nonce：占位失败说明名额已被首个合法请求持有，释放会误删其占位
            if (nonceAcquired) {
                replayGuard.release(nonceKey);
            }
            String eventType = e.getErrorCode() == OpenApiSignErrorCodes.APP_NO_PERMISSION
                ? OpenApiSignAuditEvent.APP_DENIED
                : OpenApiSignAuditEvent.SIGN_DENIED;
            recordAudit(context, appId, accessKey, new AuditOutcome(eventType, e.getErrorCode()));
            LOGGER.debug(
                "[ContiNew Starter] - Open API signature rejected, method: {}, path: {}, code: {}",
                method, path, e.getErrorCode().getCode());
            writeErrorResponse(response, e.getErrorCode());
        } catch (Exception e) {
            // SPI 内部故障不属于签名拒绝语义：不写审计（避免误导统计），释放已占位 nonce 后交容器 500
            if (nonceAcquired) {
                replayGuard.release(nonceKey);
            }
            LOGGER
                .error(
                    "[ContiNew Starter] - Open API signature verification failed unexpectedly, method: {}, path: {}, requestId: {}",
                    method, path, requestId, e);
            throw new ServletException(e);
        }
    }

    /**
     * 解析并校验签名头（Authorization）
     *
     * @param request 请求对象
     * @return 签名头字段
     */
    private SignHeaderParser.ParsedHeader parseHeaders(HttpServletRequest request) {
        String authorization =
            requireValidHeader(request, OpenApiSignProtocol.AUTHORIZATION_HEADER);
        return SignHeaderParser.parse(authorization, properties.getAlgorithm());
    }

    /**
     * 读取单一签名相关头并校验（缺失、重复、长度超限、非 ASCII 均拒绝）
     *
     * @param request    请求对象
     * @param headerName 头名称
     * @return 头原值
     */
    private String requireValidHeader(HttpServletRequest request, String headerName) {
        String value = requireSingleHeader(request, headerName);
        if (value.getBytes(StandardCharsets.UTF_8).length > properties.getSign()
            .getMaxHeaderSize()
            || !isAscii(value)) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        return value;
    }

    /**
     * 判定是否 form 表单请求（Content-Type 以 application/x-www-form-urlencoded 开头，大小写不敏感）
     *
     * @param request 请求对象
     * @return 是 form 请求时返回 true
     */
    private boolean isFormRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
            && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded");
    }

    /**
     * 包装请求为可重复读（按 max-body-size 有界缓存，包装期间超限即拒绝）
     *
     * @param request 请求对象
     * @return 包装后的请求对象
     */
    private RepeatReadRequestWrapper wrapRequest(HttpServletRequest request) throws IOException {
        if (request instanceof RepeatReadRequestWrapper wrapped) {
            return wrapped;
        }
        try {
            return new RepeatReadRequestWrapper(request, properties.getSign().getMaxBodySize());
        } catch (RequestBodySizeExceededException e) {
            // 包装期间超限（chunked 等无 Content-Length 请求的实际大小在拷贝时才可知）
            throw new OpenApiSignException(OpenApiSignErrorCodes.BODY_TOO_LARGE);
        }
    }

    /**
     * 读取单一头，缺失或重复即拒绝
     *
     * @param request    请求对象
     * @param headerName 头名称
     * @return 头原值
     */
    private String requireSingleHeader(HttpServletRequest request, String headerName) {
        List<String> values = Collections.list(request.getHeaders(headerName));
        if (values.size() != 1) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        return values.get(0);
    }

    /**
     * 校验请求字符编码，协议全链路仅接受 UTF-8
     *
     * @param request 请求对象
     */
    private void checkCharset(HttpServletRequest request) {
        String characterEncoding = request.getCharacterEncoding();
        if (characterEncoding != null
            && !StandardCharsets.UTF_8.name().equalsIgnoreCase(characterEncoding)) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
    }

    /**
     * 规范化 query（含长度上限与旧协议公共参数判定）
     *
     * @param request 请求对象
     * @return 规范化后的 query，无 query 时为空串
     */
    private String canonicalizeQuery(HttpServletRequest request) {
        String rawQuery = request.getQueryString();
        if (rawQuery != null
            && rawQuery.getBytes(StandardCharsets.UTF_8).length > properties.getSign()
                .getMaxQuerySize()) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        return QueryCanonicalizer.canonicalize(rawQuery);
    }

    /**
     * 重算 body 摘要，先校验摘要头格式与 body 大小，再与摘要头值常量时间比对
     *
     * @param wrappedRequest      包装后的请求对象
     * @param contentDigestHeader 摘要头原值
     */
    private void verifyBodyDigest(RepeatReadRequestWrapper wrappedRequest,
        String contentDigestHeader) {
        if (!contentDigestHeader.startsWith(OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX)) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        String headerDigest =
            contentDigestHeader.substring(OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX
                .length());
        if (headerDigest.length() != 64 || !isLowerCaseHex(headerDigest)) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        byte[] body = wrappedRequest.getCachedContent().toByteArray();
        if (wrappedRequest.getContentLengthLong() > properties.getSign().getMaxBodySize()
            || body.length > properties.getSign().getMaxBodySize()) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.BODY_TOO_LARGE);
        }
        String expectedDigest = ContentDigest.sha256Hex(body);
        if (!MessageDigest.isEqual(expectedDigest.getBytes(StandardCharsets.US_ASCII), headerDigest
            .getBytes(StandardCharsets.US_ASCII))) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.BODY_DIGEST_MISMATCH);
        }
    }

    /**
     * 校验时间戳窗口（含时钟偏差容忍）
     *
     * @param header 签名头字段
     */
    private void checkTimestamp(SignHeaderParser.ParsedHeader header) {
        long window =
            properties.getSign().getTimestampWindow() + properties.getSign().getClockSkew();
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        if (Math.abs(currentTimeSeconds - header.getTimestampSeconds()) > window) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_EXPIRED);
        }
    }

    /**
     * 查询凭证，查不到或密钥缺失即拒绝
     *
     * @param accessKey 应用访问标识
     * @return 凭证信息
     */
    private OpenApiCredential resolveCredential(String accessKey) {
        Optional<OpenApiCredential> credential = credentialResolver.resolve(accessKey);
        if (credential.isEmpty() || credential.get().getSecretKey() == null
            || credential.get().getSecretKey().isEmpty()) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.CREDENTIAL_INVALID);
        }
        return credential.get();
    }

    /**
     * 校验应用状态（禁用、过期）
     *
     * @param credential 凭证信息
     */
    private void checkAppStatus(OpenApiCredential credential) {
        if (credential.isDisabled()) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.APP_DISABLED);
        }
        if (credential.getExpireTime() != null
            && credential.getExpireTime().isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.APP_EXPIRED);
        }
    }

    /**
     * 记录审计事件（accessKey 脱敏后进入事件，密钥与请求体原文不进入）
     *
     * @param context   请求基础信息
     * @param appId     应用 ID（未识别时为 null）
     * @param accessKey 应用访问标识（未识别时为 null）
     * @param outcome   审计结果（事件类型与失败错误码，成功时错误码为 null）
     */
    private void recordAudit(RequestContext context, Long appId, String accessKey,
        AuditOutcome outcome) {
        auditSink.accept(OpenApiSignAuditEvent.builder()
            .eventType(outcome.eventType())
            .appId(appId)
            .accessKey(OpenApiSignAuditEvent.maskAccessKey(accessKey))
            .method(context.method())
            .path(context.path())
            .errorCode(outcome.errorCode() == null ? null : outcome.errorCode().getCode())
            .requestId(context.requestId())
            .traceId(context.traceId())
            .occurredAt(Instant.now())
            .clientIp(context.clientIp())
            .build());
    }

    /**
     * 解析请求路径（去掉 context-path，为空时取 /）
     *
     * @param request 请求对象
     * @return 请求路径
     */
    private String resolvePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = (contextPath.isEmpty() || !requestUri.startsWith(contextPath))
            ? requestUri
            : requestUri.substring(contextPath.length());
        return path.isEmpty() ? StringConstants.SLASH : path;
    }

    /**
     * 在放行前写出失败响应（错误 msg 为错误码固定文案，不拼接异常文本）
     *
     * @param response  响应对象
     * @param errorCode 错误码
     */
    private void writeErrorResponse(HttpServletResponse response,
        OpenApiSignErrorCodes errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter()
            .write("{\"code\":\"" + errorCode.getCode() + "\",\"msg\":\"" + escapeJson(errorCode
                .getMsg()) + "\",\"data\":null}");
    }

    /**
     * JSON 字符串转义（错误文案为固定中文，此处仅作防御性处理）
     *
     * @param value 待转义串
     * @return 转义后的串
     */
    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                default -> escaped.append(c < 0x20 ? String.format("\\u%04x", (int) c) : c);
            }
        }
        return escaped.toString();
    }

    /**
     * 判断串是否全为 ASCII 可打印区间字符（非 ASCII 头值拒绝）
     *
     * @param value 待校验串
     * @return 全为 ASCII 时返回 true
     */
    private boolean isAscii(String value) {
        return value.chars().allMatch(c -> c >= 0x20 && c < 0x7F);
    }

    /**
     * 判断串是否全为小写 hex 字符
     *
     * @param value 待校验串
     * @return 全为小写 hex 时返回 true
     */
    private boolean isLowerCaseHex(String value) {
        return value.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
    }

    /**
     * 请求基础信息（验签全程不变，进入链路前一次构造，供日志与审计复用）
     *
     * @param method    HTTP 方法
     * @param path      请求路径
     * @param clientIp  客户端 IP
     * @param traceId   链路追踪 ID（可为 null）
     * @param requestId 请求关联 ID
     */
    private record RequestContext(String method, String path, String clientIp, String traceId,
        String requestId) {
    }

    /**
     * 审计结果（事件类型与失败错误码，成功时错误码为 null）
     *
     * @param eventType 事件类型
     * @param errorCode 失败错误码（成功时为 null）
     */
    private record AuditOutcome(String eventType, OpenApiSignErrorCodes errorCode) {
    }
}
