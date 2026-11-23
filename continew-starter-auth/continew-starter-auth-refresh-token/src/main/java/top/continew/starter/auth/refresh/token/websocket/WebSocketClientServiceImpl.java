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

package top.continew.starter.auth.refresh.token.websocket;

import static org.springframework.web.socket.WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import top.continew.starter.auth.refresh.token.api.AccessSessionValidator;
import top.continew.starter.auth.refresh.token.support.AccessTokenClaimsReader;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.auth.refresh.token.support.WebSocketTokenExtractor;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;

import java.util.List;

/**
 * WebSocket 握手的当前登录用户 Provider。
 *
 * <p>握手仅校验选中的 Access Token。登录主体和绑定会话均有效时，以令牌指纹作为
 * 可记录的客户端 ID，原始凭证经 {@link WebSocketCredentialRegistry} 登记，供撤销广播
 * 与周期校验反查。</p>
 *
 * @author Charles7c
 * @author luoqiz
 * @since 2.17.0
 */
public class WebSocketClientServiceImpl implements WebSocketClientService {

    private final AccessSessionValidator accessSessionValidator;
    private final RefreshTokenCodec tokenCodec;
    private final WebSocketCredentialRegistry credentialRegistry;

    public WebSocketClientServiceImpl(AccessSessionValidator accessSessionValidator,
        RefreshTokenCodec tokenCodec, WebSocketCredentialRegistry credentialRegistry) {
        this.accessSessionValidator = accessSessionValidator;
        this.tokenCodec = tokenCodec;
        this.credentialRegistry = credentialRegistry;
    }

    @Override
    public String getClientId(ServletServerHttpRequest request) {
        String token = WebSocketTokenExtractor.extract(request);
        if (token == null
            || AccessTokenClaimsReader.resolveLoginIdQuietly(token) == null
            || accessSessionValidator.isInvalid(token)) {
            throw new BusinessException("登录已过期，请重新登录");
        }
        normalizeProtocolHeaders(request.getHeaders());
        String clientId = tokenCodec.fingerprint(token);
        credentialRegistry.register(clientId, token);
        return clientId;
    }

    private static void normalizeProtocolHeaders(HttpHeaders headers) {
        List<String> values = headers.getOrEmpty(SEC_WEBSOCKET_PROTOCOL);
        if (values.size() > 1) {
            // Spring 只对单行协议头拆分逗号，合并同名头须保留候选的声明顺序。
            headers.set(SEC_WEBSOCKET_PROTOCOL, String.join(", ", values));
        }
    }
}
