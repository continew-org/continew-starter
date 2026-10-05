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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 客户端标识到原始 Access Token 的进程内登记。
 *
 * <p>握手时以 Access Token 的服务端指纹作为客户端 ID（可安全进入日志与 DAO 索引），
 * 本登记负责在撤销广播与周期校验时反查出原始凭证。仅在进程内存活，随连接关闭由
 * 周期校验统一清理，不落任何外部存储。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
public class WebSocketCredentialRegistry {

    private final Map<String, String> rawTokens = new ConcurrentHashMap<>();

    /**
     * 原始凭证到客户端标识的反向索引，反查降为 O(1)。
     */
    private final Map<String, String> tokenToClientId = new ConcurrentHashMap<>();

    /**
     * 登记握手建立的客户端标识与原始凭证映射。
     *
     * @param clientId 客户端标识（Access Token 指纹）
     * @param rawToken 原始 Access Token
     */
    public void register(String clientId, String rawToken) {
        rawTokens.put(clientId, rawToken);
        tokenToClientId.put(rawToken, clientId);
    }

    /**
     * 反查客户端标识对应的原始凭证。
     *
     * @param clientId 客户端标识（Access Token 指纹）
     * @return 原始 Access Token；未登记返回 {@code null}
     */
    public String resolve(String clientId) {
        return clientId == null ? null : rawTokens.get(clientId);
    }

    /**
     * 由原始 Access Token 反查客户端标识。
     *
     * <p>供业务侧仍以原始令牌调用 {@code WebSocketUtils.sendMessage} 的既有调用点
     * 兼容使用：DAO 以客户端标识（指纹）为索引，需要先翻译回指纹才能命中连接。</p>
     *
     * @param rawToken 原始 Access Token
     * @return 客户端标识（Access Token 指纹）；未登记返回 {@code null}
     */
    public String findClientIdByToken(String rawToken) {
        return rawToken == null ? null : tokenToClientId.get(rawToken);
    }

    /**
     * 仅保留仍在线的客户端标识，清理已断开连接的登记。
     *
     * @param onlineClientIds 当前仍登记在 DAO 索引中的客户端标识
     */
    public void retainAll(Set<String> onlineClientIds) {
        List<String> staleClientIds = new ArrayList<>();
        for (String clientId : rawTokens.keySet()) {
            if (!onlineClientIds.contains(clientId)) {
                staleClientIds.add(clientId);
            }
        }
        for (String clientId : staleClientIds) {
            String rawToken = rawTokens.remove(clientId);
            if (rawToken != null) {
                tokenToClientId.remove(rawToken);
            }
        }
    }
}
