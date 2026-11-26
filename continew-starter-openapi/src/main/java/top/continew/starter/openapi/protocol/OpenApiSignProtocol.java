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

import java.util.Set;

/**
 * 开放接口验签协议常量（协议标识 OCN1-HMAC-SHA256）
 *
 * <p>协议契约的固定字面量集中于此：签名头格式、必签头清单、摘要头命名、
 * nonce 去重键前缀与旧协议公共参数名单。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class OpenApiSignProtocol {

    /**
     * 协议标识，Authorization 头的 scheme 部分
     */
    public static final String SCHEME = "OCN1-HMAC-SHA256";

    /**
     * 签名头名称
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 请求体摘要头名称
     */
    public static final String CONTENT_DIGEST_HEADER = "X-Ocn-Content-Digest";

    /**
     * 请求体摘要头值的算法前缀
     */
    public static final String DIGEST_ALGORITHM_PREFIX = "sha-256=";

    /**
     * SignedHeaders 固定取值（小写、分号分隔、字典序）
     */
    public static final String SIGNED_HEADERS = "host;x-ocn-content-digest";

    /**
     * 必签头名：Host
     */
    public static final String HOST_HEADER = "Host";

    /**
     * nonce 去重键前缀（完整键为 openapi:nonce:&lt;accessKey&gt;:&lt;nonce&gt;）
     */
    public static final String NONCE_KEY_PREFIX = "openapi:nonce:";

    /**
     * 旧协议签名公共参数名单，出现在 query 中即判定为协议混用并拒绝
     */
    public static final Set<String> LEGACY_SIGN_PARAM_NAMES = Set
        .of("accessKey", "timestamp", "nonce", "sign");

    private OpenApiSignProtocol() {
    }
}
