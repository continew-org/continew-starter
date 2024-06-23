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

package top.continew.starter.messaging.websocket.util;

import cn.hutool.extra.spring.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import java.io.IOException;

/**
 * WebSocket 工具类
 *
 * @author WeiRan
 * @author Charles7c
 * @since 2.1.0
 */
public class WebSocketUtils {

    private static final Logger log = LoggerFactory.getLogger(WebSocketUtils.class);
    private static final WebSocketSessionDao SESSION_DAO = SpringUtil.getBean(WebSocketSessionDao.class);

    private WebSocketUtils() {
    }

    /**
     * 发送消息
     *
     * @param clientId 客户端 ID
     * @param message  消息内容
     */
    public static void sendMessage(String clientId, String message) {
        WebSocketSession session = SESSION_DAO.get(clientId);
        sendMessage(session, message);
    }

    /**
     * 发送消息
     *
     * @param session 会话
     * @param message 消息内容
     */
    public static void sendMessage(WebSocketSession session, String message) {
        sendMessage(session, new TextMessage(message));
    }

    /**
     * 发送消息
     *
     * @param session 会话
     * @param message 消息内容
     */
    public static void sendMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket session closed.");
            return;
        }
        try {
            session.sendMessage(message);
        } catch (IOException e) {
            log.error("WebSocket send message failed. sessionId: {}.", session.getId(), e);
        }
    }
}
