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

import org.springframework.web.socket.SubProtocolCapable;
import top.continew.starter.messaging.websocket.autoconfigure.WebSocketProperties;
import top.continew.starter.messaging.websocket.core.DefaultWebSocketHandler;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import java.util.List;

/**
 * 只协商固定协议名的 WebSocket 处理器，Bearer 凭证项不参与响应协商。
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class AuthWebSocketHandler extends DefaultWebSocketHandler implements SubProtocolCapable {

    private static final List<String> SUB_PROTOCOLS = List.of("continew.auth.v1");

    public AuthWebSocketHandler(WebSocketProperties properties, WebSocketSessionDao sessionDao) {
        super(properties, sessionDao);
    }

    @Override
    public List<String> getSubProtocols() {
        return SUB_PROTOCOLS;
    }
}
