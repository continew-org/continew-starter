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

import org.springframework.web.socket.WebSocketSession;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;

import java.util.Collection;

/**
 * 支持同一客户端 Key 挂载多条连接的 WebSocket 会话 DAO。
 *
 * <p>浏览器多标签页共用同一 Access Token 时，Starter 默认 DAO 以 Key 为唯一维度、
 * 后建连接覆盖前者；本接口补充整组摘取能力，供撤销路径全量关闭。按 Key 枚举
 * （{@code listByKey}）已并入 {@link WebSocketSessionDao} 基类契约。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public interface MultiWebSocketSessionDao extends WebSocketSessionDao {

    /**
     * 原子摘取指定 Key 的全部连接登记并移除外层索引。
     *
     * <p>返回摘取时刻的完整连接集合；摘取之后新增的连接会进入全新登记，仍可被
     * 索引与撤销发现。撤销方只允许关闭摘取结果中的连接，不得再按旧快照删除索引。</p>
     *
     * @param key 客户端 Key（Access Token 指纹）
     * @return 摘取的连接集合（可能为空）
     */
    Collection<WebSocketSession> removeAll(String key);
}
