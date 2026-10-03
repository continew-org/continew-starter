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

package top.continew.starter.messaging.websocket.dao;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * WebSocket 会话 DAO
 *
 * @author Charles7c
 * @since 2.1.0
 */
public interface WebSocketSessionDao {

    /**
     * 添加会话
     *
     * @param key     会话 Key
     * @param session 会话信息
     */
    void add(String key, WebSocketSession session);

    /**
     * 删除会话
     *
     * @param key 会话 Key
     */
    void delete(String key);

    /**
     * 获取会话
     *
     * @param key 会话 Key
     * @return 会话信息
     */
    WebSocketSession get(String key);

    /**
     * 获取所有会话
     *
     * @return 所有会话
     * @since 2.12.1
     */
    Collection<WebSocketSession> listAll();

    /**
     * 获取所有会话 ID
     *
     * @return 所有会话 ID
     * @since 2.12.1
     */
    Set<String> listAllSessionIds();

    /**
     * 获取指定 Key 下的全部会话。
     *
     * <p>单连接实现返回该 Key 下至多一条会话；支持多标签页的实现（如
     * auth-refresh-token 模块的 {@code ConcurrentWebSocketSessionDao}）返回该 Key 下的
     * 全部存活连接，供撤销与全量推送使用。</p>
     *
     * @param key 会话 Key
     * @return 该 Key 下的全部会话；不存在时返回空集合
     * @since 2.17.0
     */
    default Collection<WebSocketSession> listByKey(String key) {
        WebSocketSession session = this.get(key);
        return session == null ? List.of() : List.of(session);
    }
}
