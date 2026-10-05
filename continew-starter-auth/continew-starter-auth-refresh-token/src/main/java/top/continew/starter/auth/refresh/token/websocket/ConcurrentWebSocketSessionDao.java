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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持多标签页的 WebSocket 会话 DAO 内存实现。
 *
 * <p>以 {@code Key → (连接 ID → 连接)} 两级登记：同一 Access Token 的多个标签页
 * 连接互不覆盖。Starter 的 {@code afterConnectionClosed}/{@code handleTransportError}
 * 关闭回调只携带 Key，因此 {@link #delete(String)} 采用「只移除已关闭连接」的语义，
 * 保留同一 Key 下仍存活的其它标签页连接。</p>
 *
 * <p>DAO 索引键为客户端标识（Access Token 指纹）。{@link #get(String)} 额外兼容
 * 直接传入原始 Access Token：查不到索引时经凭证登记反查指纹再取，业务侧既有
 * 「以 {@code StpUtil} 取令牌即推送」的调用无需改造。反查仅在登记所在进程内有效，
 * 多副本部署下跨实例推送不在覆盖范围。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public class ConcurrentWebSocketSessionDao implements MultiWebSocketSessionDao {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ConcurrentWebSocketSessionDao.class);

    private final Map<String, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    private final WebSocketCredentialRegistry credentialRegistry;

    public ConcurrentWebSocketSessionDao(WebSocketCredentialRegistry credentialRegistry) {
        this.credentialRegistry = credentialRegistry;
    }

    @Override
    public void add(String key, WebSocketSession session) {
        // 新增与清理共用外层 compute 串行化：单独的 computeIfAbsent + put 会让并发
        // 清理线程在两步之间移除外层索引，新连接随后落入脱离管理的孤儿内层 Map。
        sessions.compute(key, (k, byId) -> {
            Map<String, WebSocketSession> inner = byId == null
                ? new ConcurrentHashMap<>()
                : byId;
            inner.put(session.getId(), session);
            return inner;
        });
    }

    @Override
    public void delete(String key) {
        sessions.compute(key, (k, byId) -> {
            if (byId == null) {
                return null;
            }
            byId.values().removeIf(session -> !session.isOpen());
            return byId.isEmpty() ? null : byId;
        });
    }

    @Override
    public WebSocketSession get(String key) {
        Map<String, WebSocketSession> byId = sessions.get(this.resolveKey(key));
        if (byId == null || byId.isEmpty()) {
            return null;
        }
        // 推送是单接收方语义（WebSocketUtils.sendMessage），同一 Key 下取任意一条存活连接。
        // 多标签页场景如需全量推送，请调用 WebSocketUtils.sendMessageToAll(key, message)。
        WebSocketSession latest = null;
        for (WebSocketSession session : byId.values()) {
            if (session.isOpen()) {
                latest = session;
            }
        }
        return latest;
    }

    @Override
    public Collection<WebSocketSession> listAll() {
        return sessions.values().stream().flatMap(byId -> byId.values().stream()).toList();
    }

    @Override
    public Set<String> listAllSessionIds() {
        return sessions.keySet();
    }

    @Override
    public Collection<WebSocketSession> listByKey(String key) {
        Map<String, WebSocketSession> byId = sessions.get(this.resolveKey(key));
        return byId == null ? List.of() : List.copyOf(byId.values());
    }

    @Override
    public Collection<WebSocketSession> removeAll(String key) {
        // 整组原子摘取：返回摘取时刻的连接集合，摘取后新增的连接进入全新登记。
        Map<String, WebSocketSession> removed = sessions.remove(key);
        return removed == null ? List.of() : List.copyOf(removed.values());
    }

    /**
     * 把调用方传入的键翻译为 DAO 实际索引键：索引键为客户端标识（Access Token 指纹），
     * 兼容直接传入原始 Access Token 时经凭证登记反查指纹。
     *
     * @param key 客户端标识（指纹）或原始 Access Token
     * @return DAO 索引键
     */
    private String resolveKey(String key) {
        if (credentialRegistry == null || sessions.containsKey(key)) {
            return key;
        }
        String clientId = credentialRegistry.findClientIdByToken(key);
        if (clientId == null) {
            LOGGER.debug(
                "WebSocket 连接查找：入参既非已登记指纹，反查原始令牌亦未命中登记（可能令牌已轮换、连接已断开或推送发往了其它实例）");
            return key;
        }
        return clientId;
    }
}
