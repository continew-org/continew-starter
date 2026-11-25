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

package top.continew.starter.messaging.websocket.core;

import org.springframework.web.socket.server.HandshakeHandler;
import top.continew.starter.messaging.websocket.autoconfigure.WebSocketProperties;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

/**
 * 消息端点的默认处理器与握手策略工厂。
 *
 * @author Charles7c
 * @since 3.1.0
 */
public interface WebSocketEndpointFactory {

    /**
     * 创建默认消息处理器，使用方已注册 DefaultWebSocketHandler 时不调用。
     *
     * @param properties WebSocket 配置属性
     * @param sessionDao WebSocket 会话 DAO
     * @return 消息处理器
     */
    DefaultWebSocketHandler createHandler(WebSocketProperties properties,
        WebSocketSessionDao sessionDao);

    /**
     * 创建在握手拦截器放行后执行的端点握手策略。
     *
     * @return 握手策略
     */
    HandshakeHandler createHandshakeHandler();
}
