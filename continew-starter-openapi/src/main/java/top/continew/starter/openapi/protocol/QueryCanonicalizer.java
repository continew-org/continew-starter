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

import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.openapi.constant.OpenApiSignErrorCodes;
import top.continew.starter.openapi.exception.OpenApiSignException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * query 规范化（canonical query）
 *
 * <p>规则：取原始 query 串按 {@code &} 拆对、按首个 {@code =} 拆键值；键与值先做 URL 解码
 * （UTF-8，{@code +} 还原为空格），再做 RFC 3986 百分号编码（未保留字符为字母数字与
 * {@code - . _ ~}，其余字节转大写 {@code %XX}）；按编码后键字节序升序排序，键相同按值排序；
 * 以 {@code =} 与 {@code &} 连接。旧协议签名公共参数出现在 query 中按协议混用拒绝。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class QueryCanonicalizer {

    private static final String PAIR_SEPARATOR = "&";

    private static final String KEY_VALUE_SEPARATOR = "=";

    private static final char[] UPPERCASE_HEX = "0123456789ABCDEF".toCharArray();

    private QueryCanonicalizer() {
    }

    /**
     * 规范化 query
     *
     * @param rawQuery 原始 query 串（不含 {@code ?}），null 或空串视为无 query
     * @return 规范化后的 query，无 query 时为空串
     */
    public static String canonicalize(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<String[]> encodedPairs = new ArrayList<>();
        for (String pair : rawQuery.split(PAIR_SEPARATOR, -1)) {
            if (pair.isEmpty()) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
            }
            int separatorIndex = pair.indexOf(KEY_VALUE_SEPARATOR);
            String rawName = separatorIndex >= 0 ? pair.substring(0, separatorIndex) : pair;
            String rawValue = separatorIndex >= 0 ? pair.substring(separatorIndex + 1) : null;
            String name = decode(rawName);
            if (OpenApiSignProtocol.LEGACY_SIGN_PARAM_NAMES.contains(name)) {
                throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
            }
            String value = rawValue == null ? StringConstants.EMPTY : decode(rawValue);
            encodedPairs.add(new String[] {percentEncode(name), percentEncode(value)});
        }
        encodedPairs.sort(
            Comparator.<String[], String>comparing(pair -> pair[0]).thenComparing(pair -> pair[1]));
        StringBuilder canonical = new StringBuilder();
        for (String[] pair : encodedPairs) {
            if (canonical.length() > 0) {
                canonical.append(PAIR_SEPARATOR);
            }
            canonical.append(pair[0]).append(KEY_VALUE_SEPARATOR).append(pair[1]);
        }
        return canonical.toString();
    }

    /**
     * URL 解码（UTF-8，{@code +} 还原为空格）
     *
     * @param value 待解码串
     * @return 解码后的串
     */
    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new OpenApiSignException(OpenApiSignErrorCodes.SIGN_HEADER_INVALID);
        }
    }

    /**
     * RFC 3986 百分号编码，未保留字符直写，其余字节转大写 %XX
     *
     * @param value 待编码串，null 原样返回
     * @return 编码后的串
     */
    private static String percentEncode(String value) {
        if (value == null) {
            return null;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            int unsigned = b & 0xFF;
            char c = (char) unsigned;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '-'
                || c == '.' || c == '_' || c == '~') {
                encoded.append(c);
            } else {
                encoded.append('%').append(UPPERCASE_HEX[unsigned >>> 4])
                    .append(UPPERCASE_HEX[unsigned & 0xF]);
            }
        }
        return encoded.toString();
    }
}
