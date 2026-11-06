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

/**
 * 待签串（string-to-sign）构造
 *
 * <p>固定 9 行，行间以 LF 连接，末尾不加换行：</p>
 * <ol>
 * <li>协议标识</li>
 * <li>HTTP method（大写）</li>
 * <li>path（不含 query 与 context-path）</li>
 * <li>canonical query（无 query 时为空行）</li>
 * <li>规范化 host 头</li>
 * <li>规范化摘要头</li>
 * <li>SignedHeaders 固定清单</li>
 * <li>Timestamp（Unix 秒十进制串）</li>
 * <li>Nonce</li>
 * </ol>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class StringToSign {

    private static final char LINE_SEPARATOR = '\n';

    private StringToSign() {
    }

    /**
     * 构造 9 行待签串
     *
     * @param method                   HTTP 方法（原值，内部转大写）
     * @param path                     请求路径（不含 query 与 context-path，为空时取 /）
     * @param canonicalQuery           规范化 query（无 query 时为空串）
     * @param host                     请求 Host 头原值
     * @param contentDigestHeaderValue 摘要头原值（含 sha-256= 前缀）
     * @param timestamp                Unix 秒十进制字符串
     * @param nonce                    随机串
     * @return 9 行待签串
     */
    public static String build(String method, String path, String canonicalQuery, String host,
        String contentDigestHeaderValue, String timestamp, String nonce) {
        String normalizedPath = (path == null || path.isEmpty()) ? "/" : path;
        String normalizedQuery = canonicalQuery == null ? "" : canonicalQuery;
        return OpenApiSignProtocol.SCHEME + LINE_SEPARATOR
            + method.toUpperCase() + LINE_SEPARATOR
            + normalizedPath + LINE_SEPARATOR
            + normalizedQuery + LINE_SEPARATOR
            + OpenApiSignProtocol.HOST_HEADER.toLowerCase() + ':' + host + LINE_SEPARATOR
            + OpenApiSignProtocol.CONTENT_DIGEST_HEADER.toLowerCase() + ':'
            + contentDigestHeaderValue + LINE_SEPARATOR
            + OpenApiSignProtocol.SIGNED_HEADERS + LINE_SEPARATOR
            + timestamp + LINE_SEPARATOR
            + nonce;
    }
}
