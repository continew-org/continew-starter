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

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 多标签页 WebSocket 会话 DAO 的并发登记与摘取语义测试。
 *
 * @author Charles7c
 * @since 2.17.0
 */
class ConcurrentWebSocketSessionDaoTest {

    @Test
    void shouldKeepOtherTabsWhenOneConnectionCloses() {
        ConcurrentWebSocketSessionDao dao =
            new ConcurrentWebSocketSessionDao(new WebSocketCredentialRegistry());
        WebSocketSession closed = this.session("conn-1", false);
        WebSocketSession alive = this.session("conn-2", true);

        dao.add("client-id", closed);
        dao.add("client-id", alive);
        List<WebSocketSession> registered = List.copyOf(dao.listByKey("client-id"));
        assertEquals(2, registered.size());
        assertTrue(registered.containsAll(List.of(closed, alive)));
        assertEquals(Set.of("client-id"), dao.listAllSessionIds());

        // Starter 关闭回调只携带 Key：只移除已关闭的连接，保留存活的标签页。
        dao.delete("client-id");
        assertEquals(List.of(alive), dao.listByKey("client-id"));
        assertEquals(alive, dao.get("client-id"));
    }

    @Test
    void shouldExtractAllSessionsAndKeepLaterAdditionsDiscoverable() {
        ConcurrentWebSocketSessionDao dao =
            new ConcurrentWebSocketSessionDao(new WebSocketCredentialRegistry());
        WebSocketSession first = this.session("conn-1", true);
        WebSocketSession second = this.session("conn-2", true);
        dao.add("client-id", first);
        dao.add("client-id", second);

        // 摘取返回完整集合，索引同时清空；摘取后新增的连接进入全新登记，仍可发现。
        List<WebSocketSession> extracted = List.copyOf(dao.removeAll("client-id"));
        assertEquals(2, extracted.size());
        assertTrue(extracted.containsAll(List.of(first, second)));
        assertEquals(List.of(), dao.listByKey("client-id"));
        assertNull(dao.get("client-id"));

        WebSocketSession later = this.session("conn-3", true);
        dao.add("client-id", later);
        assertEquals(List.of(later), dao.listByKey("client-id"));
    }

    @Test
    void shouldKeepOpenConnectionDiscoverableWhenAddRacesDelete() throws Exception {
        ConcurrentWebSocketSessionDao dao =
            new ConcurrentWebSocketSessionDao(new WebSocketCredentialRegistry());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 300; round++) {
                WebSocketSession closing = this.session("closing-" + round, false);
                WebSocketSession opening = this.session("opening-" + round, true);
                dao.add("client-id", closing);

                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<?> adder = executor.submit(() -> {
                    barrier.await();
                    dao.add("client-id", opening);
                    return null;
                });
                Future<?> deleter = executor.submit(() -> {
                    barrier.await();
                    dao.delete("client-id");
                    return null;
                });
                adder.get(5, TimeUnit.SECONDS);
                deleter.get(5, TimeUnit.SECONDS);

                // 不变量：无论新增与清理以何种顺序交错，仍然打开的连接必须可被
                // 索引发现；否则撤销广播与周期校验永远无法命中该连接。
                assertTrue(dao.listByKey("client-id").contains(opening), "第 " + round
                    + " 轮：并发新增/清理后存活连接脱离索引");

                // 清理本轮登记，避免影响下一轮断言。
                when(opening.isOpen()).thenReturn(false);
                dao.delete("client-id");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldResolveRawTokenToClientIdOnGet() {
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        ConcurrentWebSocketSessionDao dao = new ConcurrentWebSocketSessionDao(registry);
        WebSocketSession alive = this.session("conn-1", true);

        dao.add("token-fingerprint", alive);
        registry.register("token-fingerprint", "raw-access-token");

        // 业务侧以原始令牌推送（WebSocketUtils.sendMessage(rawToken, ...)）：经登记反查指纹命中连接
        assertEquals(alive, dao.get("raw-access-token"));
        // 指纹直查与未登记令牌的原有语义不变
        assertEquals(alive, dao.get("token-fingerprint"));
        assertNull(dao.get("unknown-token"));
    }

    @Test
    void shouldResolveRawTokenToClientIdOnListByKey() {
        WebSocketCredentialRegistry registry = new WebSocketCredentialRegistry();
        ConcurrentWebSocketSessionDao dao = new ConcurrentWebSocketSessionDao(registry);
        WebSocketSession tab1 = this.session("conn-1", true);
        WebSocketSession tab2 = this.session("conn-2", true);

        dao.add("token-fingerprint", tab1);
        dao.add("token-fingerprint", tab2);
        registry.register("token-fingerprint", "raw-access-token");

        // sendMessageToAll 走 listByKey：原始令牌反查后须返回同一指纹下的全部标签页连接
        List<WebSocketSession> all = List.copyOf(dao.listByKey("raw-access-token"));
        assertEquals(2, all.size());
        assertTrue(all.containsAll(List.of(tab1, tab2)));
        // 指纹直查语义不变
        assertEquals(2, dao.listByKey("token-fingerprint").size());
    }

    private WebSocketSession session(String id, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(open);
        return session;
    }
}
