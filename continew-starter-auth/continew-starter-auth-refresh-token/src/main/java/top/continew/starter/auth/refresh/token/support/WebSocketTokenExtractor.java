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

package top.continew.starter.auth.refresh.token.support;

import static org.springframework.web.socket.WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL;

import org.springframework.http.server.ServletServerHttpRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * WebSocket 握手凭证读取器。
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class WebSocketTokenExtractor {

    private static final String BEARER_PROTOCOL_PREFIX = "Bearer.";

    private WebSocketTokenExtractor() {
    }

    /**
     * 取子协议中第一个可解码的 Bearer 凭证；没有候选时才读取 {@code token} 查询参数。
     *
     * @param request 握手请求
     * @return 明文 Access Token；未取得凭证时返回 {@code null}
     */
    public static String extract(ServletServerHttpRequest request) {
        for (String header : request.getHeaders().getOrEmpty(SEC_WEBSOCKET_PROTOCOL)) {
            for (String protocol : header.split(",")) {
                String token = decodeBearer(protocol.strip());
                if (token != null) {
                    return token;
                }
            }
        }
        String token = request.getServletRequest().getParameter("token");
        return token == null || token.isBlank() ? null : token;
    }

    private static String decodeBearer(String protocol) {
        if (!protocol.startsWith(BEARER_PROTOCOL_PREFIX)
            || protocol.length() == BEARER_PROTOCOL_PREFIX.length()) {
            return null;
        }
        String encoded = protocol.substring(BEARER_PROTOCOL_PREFIX.length());
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
