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

package top.continew.starter.openapi.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import top.continew.starter.openapi.constant.OpenApiSignErrorCodes;
import top.continew.starter.openapi.exception.OpenApiSignException;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 待签串与签名测试向量
 *
 * <p>向量以设计文档第 3 节协议契约为基准：body 摘要值（SHA-256("")、V2 body、F2 篡改 body）
 * 与设计文档逐字一致；签名值按测试 SK 字面量经 JDK 实现与 Node crypto 两个独立来源实算后
 * 固化，双源结果一致（设计文档中的签名值因生成时密钥占位符被替换而无法按披露的字面量复现，
 * 详情见模块文档测试向量一节）。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class StringToSignVectorTest {

    /**
     * 测试密钥（与设计文档 3.7 节披露的字面量一致，仅用于测试）
     */
    private static final String TEST_SECRET_KEY = "{{SECRET_c0c4usr9}}";

    private static final String HOST = "api.example.com";

    private static final String TIMESTAMP = "1730000000";

    /**
     * SHA-256("") 契约值
     */
    private static final String EMPTY_BODY_DIGEST =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    /**
     * V2 body 摘要契约值
     */
    private static final String V2_BODY_DIGEST =
        "e40373cbad970f998f7cddcc3b611ca5b1e0cdf3a4e311d86da94103214a848f";

    /**
     * V2 篡改 body 摘要契约值
     */
    private static final String F2_BODY_DIGEST =
        "880fa16479a6662b197691dfd68488336bac291a7419c745ca8d91a270f4dd93";

    /**
     * V1 签名（JDK 与 Node crypto 双源实算值）
     */
    private static final String V1_SIGNATURE =
        "769cd5261488441f15900690b4c0bf825a5c51acb5839f16ce0547f433d4ce18";

    /**
     * V2 签名（JDK 与 Node crypto 双源实算值）
     */
    private static final String V2_SIGNATURE =
        "076e7252ba75ee0e116d1c9b49396075681d0590715bf69c81934b6ac8194d56";

    /**
     * F1 签名（timestamp 改为 1729998700 后重算，双源实算值）
     */
    private static final String F1_SIGNATURE =
        "e1afa8bd690d1172d367bffbc4bb3e9629291cd4ba0a4c4e4a04404b06f3c2cb";

    /**
     * F2 篡改摘要头后重算的签名（双源实算值）
     */
    private static final String F2_SIGNATURE =
        "046de4b9b51a65fb9525322fd94fe3dd052fcdf7dfe5012c323e10d9f25e40c6";

    /**
     * V1 正例：GET + query + 空 body，待签串 9 行结构与签名逐字段对齐契约
     */
    @Test
    void v1GetWithQueryShouldProduceVectorSignature() {
        String canonicalQuery = QueryCanonicalizer.canonicalize("page=1&size=20&status=paid");
        assertThat(canonicalQuery).isEqualTo("page=1&size=20&status=paid");
        String stringToSign =
            StringToSign.build("GET", "/open/api/order/query", canonicalQuery, HOST,
                OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX + EMPTY_BODY_DIGEST, TIMESTAMP,
                "aB3cD5eF7gH9jK2m");
        assertThat(stringToSign.split("\n", -1)).containsExactly("OCN1-HMAC-SHA256", "GET",
            "/open/api/order/query", "page=1&size=20&status=paid", "host:" + HOST,
            "x-ocn-content-digest:sha-256=" + EMPTY_BODY_DIGEST, "host;x-ocn-content-digest",
            TIMESTAMP,
            "aB3cD5eF7gH9jK2m");
        assertThat(stringToSign).doesNotEndWith("\n");
        assertThat(SignatureVerifier.verify(TEST_SECRET_KEY, stringToSign, V1_SIGNATURE)).isTrue();
    }

    /**
     * V2 正例：POST + JSON body 原文字节摘要（不重排 JSON），签名对齐契约
     */
    @Test
    void v2PostWithJsonBodyShouldProduceVectorSignature() {
        byte[] body = "{\"amount\":100,\"currency\":\"CNY\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(ContentDigest.sha256Hex(body)).isEqualTo(V2_BODY_DIGEST);
        String stringToSign = StringToSign.build("POST", "/open/api/order/create", "", HOST,
            OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX + V2_BODY_DIGEST, TIMESTAMP,
            "nKq2mZp7Rs1TvWx4Yb6Hc");
        assertThat(stringToSign.split("\n", -1)).containsExactly("OCN1-HMAC-SHA256", "POST",
            "/open/api/order/create", "", "host:" + HOST,
            "x-ocn-content-digest:sha-256=" + V2_BODY_DIGEST,
            "host;x-ocn-content-digest", TIMESTAMP, "nKq2mZp7Rs1TvWx4Yb6Hc");
        assertThat(SignatureVerifier.verify(TEST_SECRET_KEY, stringToSign, V2_SIGNATURE)).isTrue();
    }

    /**
     * F1 失败例：timestamp 与服务端相差 1300 秒，超出 300 秒窗口，重算签名与原签名不同
     */
    @Test
    void f1ExpiredTimestampShouldChangeSignature() {
        String expiredTimestamp = "1729998700";
        assertThat(Math.abs(Long.parseLong(TIMESTAMP) - Long.parseLong(expiredTimestamp)))
            .isGreaterThan(300);
        String stringToSign = StringToSign.build("POST", "/open/api/order/create", "", HOST,
            OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX + V2_BODY_DIGEST, expiredTimestamp,
            "nKq2mZp7Rs1TvWx4Yb6Hc");
        assertThat(SignatureVerifier.verify(TEST_SECRET_KEY, stringToSign, F1_SIGNATURE)).isTrue();
        // 过期请求的签名与 V2 原签名不同，窗口判定在 HMAC 验算之前拦截
        assertThat(F1_SIGNATURE).isNotEqualTo(V2_SIGNATURE);
    }

    /**
     * F2 失败例路径 a：body 被改而摘要头未改，重算摘要与头值不一致
     */
    @Test
    void f2TamperedBodyShouldMismatchDigestHeader() {
        byte[] tamperedBody =
            "{\"amount\":999,\"currency\":\"CNY\"}".getBytes(StandardCharsets.UTF_8);
        String recalculatedDigest = ContentDigest.sha256Hex(tamperedBody);
        assertThat(recalculatedDigest).isEqualTo(F2_BODY_DIGEST).isNotEqualTo(V2_BODY_DIGEST);
    }

    /**
     * F2 失败例路径 b：摘要头与 body 同步篡改，重算签名与请求携带的 V2 签名不一致
     */
    @Test
    void f2TamperedDigestHeaderShouldMismatchSignature() {
        String stringToSign = StringToSign.build("POST", "/open/api/order/create", "", HOST,
            OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX + F2_BODY_DIGEST, TIMESTAMP,
            "nKq2mZp7Rs1TvWx4Yb6Hc");
        String tamperedSignature =
            HexFormat.of().formatHex(SignatureVerifier.hmacSha256(TEST_SECRET_KEY,
                stringToSign));
        assertThat(tamperedSignature).isEqualTo(F2_SIGNATURE).isNotEqualTo(V2_SIGNATURE);
    }

    /**
     * 空 body 摘要取 SHA-256 空串契约值
     */
    @Test
    void emptyBodyDigestShouldMatchContractValue() {
        assertThat(ContentDigest.emptySha256Hex()).isEqualTo(EMPTY_BODY_DIGEST);
    }

    /**
     * 签名密钥错误时验签不通过
     */
    @Test
    void wrongSecretKeyShouldFailVerification() {
        String stringToSign = StringToSign.build("POST", "/open/api/order/create", "", HOST,
            OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX + V2_BODY_DIGEST, TIMESTAMP,
            "nKq2mZp7Rs1TvWx4Yb6Hc");
        assertThat(SignatureVerifier.verify("another-secret-key", stringToSign, V2_SIGNATURE))
            .isFalse();
    }

    /**
     * 非法 hex 签名直接判不匹配
     */
    @Test
    void invalidHexSignatureShouldFailVerification() {
        String stringToSign = StringToSign.build("POST", "/open/api/order/create", "", HOST,
            OpenApiSignProtocol.DIGEST_ALGORITHM_PREFIX + V2_BODY_DIGEST, TIMESTAMP,
            "nKq2mZp7Rs1TvWx4Yb6Hc");
        assertThat(SignatureVerifier.verify(TEST_SECRET_KEY, stringToSign, "zz-not-hex")).isFalse();
    }

    /**
     * query 规范化：RFC 3986 编码、键字节序排序、空值保留等号、重复键按值排序
     */
    @Test
    void canonicalQueryShouldFollowNormalizationRules() {
        assertThat(QueryCanonicalizer.canonicalize(null)).isEmpty();
        assertThat(QueryCanonicalizer.canonicalize("")).isEmpty();
        assertThat(QueryCanonicalizer.canonicalize("b=2&a=1")).isEqualTo("a=1&b=2");
        assertThat(QueryCanonicalizer.canonicalize("B=1&a=1")).isEqualTo("B=1&a=1");
        assertThat(QueryCanonicalizer.canonicalize("a%20b=1")).isEqualTo("a%20b=1");
        assertThat(QueryCanonicalizer.canonicalize("a+b=1")).isEqualTo("a%20b=1");
        assertThat(QueryCanonicalizer.canonicalize("a=")).isEqualTo("a=");
        assertThat(QueryCanonicalizer.canonicalize("a")).isEqualTo("a=");
        assertThat(QueryCanonicalizer.canonicalize("a=2&a=1")).isEqualTo("a=1&a=2");
        assertThat(QueryCanonicalizer.canonicalize("a=b=c")).isEqualTo("a=b%3Dc");
        assertThat(QueryCanonicalizer.canonicalize("k=%E5%80%BC")).isEqualTo("k=%E5%80%BC");
        assertThat(QueryCanonicalizer.canonicalize("k=a~b.c_d-e")).isEqualTo("k=a~b.c_d-e");
    }

    /**
     * query 含旧协议签名公共参数按协议混用拒绝
     */
    @Test
    void legacySignParamsInQueryShouldBeRejected() {
        for (String legacyParam : new String[] {"sign=abc", "accessKey=1", "timestamp=1",
            "nonce=1"}) {
            assertThatThrownBy(() -> QueryCanonicalizer.canonicalize(legacyParam))
                .isInstanceOf(OpenApiSignException.class)
                .extracting(e -> ((OpenApiSignException) e).getErrorCode())
                .isEqualTo(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
    }

    /**
     * 非法百分号序列拒绝
     */
    @Test
    void malformedPercentSequenceShouldBeRejected() {
        assertThatThrownBy(() -> QueryCanonicalizer.canonicalize("k=%zz"))
            .isInstanceOf(OpenApiSignException.class);
    }

    /**
     * 空片段（连续 &）拒绝
     */
    @Test
    void emptyPairShouldBeRejected() {
        assertThatThrownBy(() -> QueryCanonicalizer.canonicalize("a=1&&b=2"))
            .isInstanceOf(OpenApiSignException.class);
    }

    /**
     * 签名头解析：合法头解析出各字段，格式违规与取值违规拒绝
     */
    @Test
    void signHeaderParsingShouldEnforceFieldConstraints() {
        String authorization =
            authorization("ak_example_0001", "host;x-ocn-content-digest", "1730000000",
                "aB3cD5eF7gH9jK2m", "a".repeat(64));
        SignHeaderParser.ParsedHeader header = SignHeaderParser.parse(authorization,
            OpenApiSignProtocol.SCHEME);
        assertThat(header.getAccessKey()).isEqualTo("ak_example_0001");
        assertThat(header.getTimestampSeconds()).isEqualTo(1730000000L);
        assertThat(header.getNonce()).isEqualTo("aB3cD5eF7gH9jK2m");

        // 字段顺序错误、SignedHeaders 取值错误、Timestamp 非 10 位、Nonce 长度不足、Signature 非 64 hex 均拒绝
        String reordered = "OCN1-HMAC-SHA256 SignedHeaders=" + OpenApiSignProtocol.SIGNED_HEADERS
            + ", Credential=ak_example_0001, Timestamp=1730000000, Nonce=aB3cD5eF7gH9jK2m, Signature="
            + "a".repeat(64);
        assertThatThrownBy(() -> SignHeaderParser.parse(reordered, OpenApiSignProtocol.SCHEME))
            .isInstanceOf(OpenApiSignException.class);
        String wrongSignedHeaders =
            authorization("ak_example_0001", "host", "1730000000", "aB3cD5eF7gH9jK2m",
                "a".repeat(64));
        assertThatThrownBy(() -> SignHeaderParser.parse(wrongSignedHeaders,
            OpenApiSignProtocol.SCHEME))
            .isInstanceOf(OpenApiSignException.class);
        String shortTimestamp =
            authorization("ak_example_0001", "host;x-ocn-content-digest", "1730000",
                "aB3cD5eF7gH9jK2m", "a".repeat(64));
        assertThatThrownBy(() -> SignHeaderParser.parse(shortTimestamp, OpenApiSignProtocol.SCHEME))
            .isInstanceOf(OpenApiSignException.class);
        String shortNonce =
            authorization("ak_example_0001", "host;x-ocn-content-digest", "1730000000", "short",
                "a".repeat(64));
        assertThatThrownBy(() -> SignHeaderParser.parse(shortNonce, OpenApiSignProtocol.SCHEME))
            .isInstanceOf(OpenApiSignException.class);
        String uppercaseSignature =
            authorization("ak_example_0001", "host;x-ocn-content-digest", "1730000000",
                "aB3cD5eF7gH9jK2m", "A".repeat(64));
        assertThatThrownBy(() -> SignHeaderParser.parse(uppercaseSignature,
            OpenApiSignProtocol.SCHEME))
            .isInstanceOf(OpenApiSignException.class);
    }

    /**
     * 按协议字段顺序拼接 Authorization 头值
     *
     * @param accessKey     应用访问标识
     * @param signedHeaders SignedHeaders 取值
     * @param timestamp     Unix 秒字符串
     * @param nonce         随机串
     * @param signature     签名 hex
     * @return Authorization 头值
     */
    private String authorization(String accessKey, String signedHeaders, String timestamp,
        String nonce,
        String signature) {
        return "OCN1-HMAC-SHA256 Credential=" + accessKey + ", SignedHeaders=" + signedHeaders
            + ", Timestamp="
            + timestamp + ", Nonce=" + nonce + ", Signature=" + signature;
    }
}
