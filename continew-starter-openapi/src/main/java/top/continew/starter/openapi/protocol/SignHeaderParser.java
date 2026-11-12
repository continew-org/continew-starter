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

import top.continew.starter.openapi.constant.OpenApiSignErrorCodes;
import top.continew.starter.openapi.exception.OpenApiSignException;

import java.util.regex.Pattern;

/**
 * Authorization 签名头解析
 *
 * <p>头格式（字段间逗号分隔，字段名大小写敏感）：
 * {@code OCN1-HMAC-SHA256 Credential=<accessKey>, SignedHeaders=host;x-ocn-content-digest,
 * Timestamp=<unix秒>, Nonce=<nonce>, Signature=<hex>}。长度上限、重复头与非 ASCII 字符
 * 由调用方在读取头时校验，本类只做格式与取值约束校验。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class SignHeaderParser {

    /**
     * Timestamp 取值：10 位十进制数字
     */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{10}");

    /**
     * Nonce 取值：{@code [A-Za-z0-9]} 的 16 至 64 位
     */
    private static final Pattern NONCE_PATTERN = Pattern.compile("[A-Za-z0-9]{16,64}");

    /**
     * Signature 取值：64 位小写 hex
     */
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("[0-9a-f]{64}");

    /**
     * 字段键名，按协议固定顺序排列
     */
    private static final String[] FIELD_KEYS = {"Credential", "SignedHeaders", "Timestamp", "Nonce",
        "Signature"};

    private static final String SCHEME_VALUE_SEPARATOR = " ";

    private static final String FIELD_SEPARATOR = ",";

    private static final String KEY_VALUE_SEPARATOR = "=";

    private SignHeaderParser() {
    }

    /**
     * 解析并校验签名头
     *
     * @param authorizationHeader Authorization 头原值
     * @param scheme              期望的协议标识（取配置项 algorithm）
     * @return 解析后的签名头字段
     */
    public static ParsedHeader parse(String authorizationHeader, String scheme) {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        int schemeSeparator = authorizationHeader.indexOf(SCHEME_VALUE_SEPARATOR);
        if (schemeSeparator < 0
            || !scheme.equals(authorizationHeader.substring(0, schemeSeparator))) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        String[] fields = authorizationHeader.substring(schemeSeparator + 1).split(FIELD_SEPARATOR);
        String[] values = new String[FIELD_KEYS.length];
        if (fields.length != FIELD_KEYS.length) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
        for (int i = 0; i < FIELD_KEYS.length; i++) {
            String field = fields[i].trim();
            int valueSeparator = field.indexOf(KEY_VALUE_SEPARATOR);
            if (valueSeparator <= 0 || !FIELD_KEYS[i].equals(field.substring(0, valueSeparator))) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
            }
            values[i] = field.substring(valueSeparator + 1);
        }
        ParsedHeader header =
            new ParsedHeader(values[0], values[1], values[2], values[3], values[4]);
        validateValues(header);
        return header;
    }

    /**
     * 校验各字段取值约束
     *
     * @param header 解析结果
     */
    private static void validateValues(ParsedHeader header) {
        if (header.getAccessKey().isEmpty()
            || !OpenApiSignProtocol.SIGNED_HEADERS.equals(header.getSignedHeaders())
            || !TIMESTAMP_PATTERN.matcher(header.getTimestamp()).matches()
            || !NONCE_PATTERN.matcher(header.getNonce()).matches()
            || !SIGNATURE_PATTERN.matcher(header.getSignature()).matches()) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
    }

    /**
     * 签名头解析结果
     *
     * @author Charles7c
     * @since 3.1.0
     */
    public static final class ParsedHeader {

        private final String accessKey;

        private final String signedHeaders;

        private final String timestamp;

        private final String nonce;

        private final String signature;

        private ParsedHeader(String accessKey, String signedHeaders, String timestamp, String nonce,
            String signature) {
            this.accessKey = accessKey;
            this.signedHeaders = signedHeaders;
            this.timestamp = timestamp;
            this.nonce = nonce;
            this.signature = signature;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public String getSignedHeaders() {
            return signedHeaders;
        }

        public String getTimestamp() {
            return timestamp;
        }

        /**
         * 获取 Unix 秒时间戳
         *
         * @return 时间戳秒值
         */
        public long getTimestampSeconds() {
            return Long.parseLong(timestamp);
        }

        public String getNonce() {
            return nonce;
        }

        public String getSignature() {
            return signature;
        }
    }
}
