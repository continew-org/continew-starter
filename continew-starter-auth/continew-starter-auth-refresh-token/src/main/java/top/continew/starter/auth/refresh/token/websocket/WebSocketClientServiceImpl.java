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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import top.continew.starter.auth.refresh.token.api.AccessSessionValidator;
import top.continew.starter.auth.refresh.token.support.AccessTokenClaimsReader;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;

/**
 * WebSocket 握手的当前登录用户 Provider。
 *
 * <p>握手请求必须携带有效 Access Token，且其绑定的认证会话未失效，否则拒绝建立
 * 实时连接。客户端 ID 使用 Access Token 的服务端指纹而非原始凭证：该值会作为 DAO
 * 索引键进入连接生命周期日志，凭证与可记录标识必须分离；原始 Token 经
 * {@link WebSocketCredentialRegistry} 登记，供撤销广播与周期校验反查。</p>
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
        HttpServletRequest servletRequest = request.getServletRequest();
        String token = servletRequest.getParameter("token");
        if (token == null || token.isBlank()
            || AccessTokenClaimsReader.resolveLoginIdQuietly(token) == null
            || accessSessionValidator.isInvalid(token)) {
            throw new BusinessException("登录已过期，请重新登录");
        }
        String clientId = tokenCodec.fingerprint(token);
        credentialRegistry.register(clientId, token);
        return clientId;
    }
}
