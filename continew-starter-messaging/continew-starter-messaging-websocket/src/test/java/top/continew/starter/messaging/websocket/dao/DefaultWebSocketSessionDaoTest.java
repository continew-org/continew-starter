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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

/**
 * {@link DefaultWebSocketSessionDao} 内存会话存储语义测试
 *
 * <p>底层 {@code SESSION_MAP} 为类级静态共享，注册/注销的语义断言全部使用唯一 Key 隔离，
 * 全量视图断言按包含判定而非整体相等，避免与其他测试共享状态相互污染。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("DefaultWebSocketSessionDao 内存会话存储语义")
class DefaultWebSocketSessionDaoTest {

    private final DefaultWebSocketSessionDao sessionDao = new DefaultWebSocketSessionDao();

    /**
     * 生成用例唯一会话 Key，规避静态共享存储的跨用例干扰。
     *
     * @return 用例唯一会话 Key
     */
    private static String uniqueKey() {
        return "dao-test-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("add 后应能按 Key 取回同一会话")
    void shouldReturnSameSessionAfterAdd() {
        String key = uniqueKey();
        WebSocketSession session = mock(WebSocketSession.class);

        sessionDao.add(key, session);

        assertSame(session, sessionDao.get(key));
    }

    @Test
    @DisplayName("delete 后应取不到会话，删除未知 Key 不抛异常")
    void shouldRemoveSessionAndTolerateUnknownKey() {
        String key = uniqueKey();
        WebSocketSession session = mock(WebSocketSession.class);
        sessionDao.add(key, session);

        sessionDao.delete(key);

        assertNull(sessionDao.get(key));
        assertDoesNotThrow(() -> sessionDao.delete(key));
    }

    @Test
    @DisplayName("同一 Key 重复 add 应覆盖为先注册的会话")
    void shouldOverrideSessionUnderSameKey() {
        String key = uniqueKey();
        WebSocketSession first = mock(WebSocketSession.class);
        WebSocketSession second = mock(WebSocketSession.class);

        sessionDao.add(key, first);
        sessionDao.add(key, second);

        assertSame(second, sessionDao.get(key));
        assertEquals(1, sessionDao.listByKey(key).size());
        assertSame(second, sessionDao.listByKey(key).iterator().next());
    }

    @Test
    @DisplayName("全量视图应包含已注册的会话与 Key")
    void shouldContainRegisteredEntriesInFullViews() {
        String key = uniqueKey();
        WebSocketSession session = mock(WebSocketSession.class);
        sessionDao.add(key, session);

        assertTrue(sessionDao.listAll().contains(session));
        assertTrue(sessionDao.listAllSessionIds().contains(key));
    }

    @Test
    @DisplayName("listByKey 对未知 Key 返回空集合")
    void shouldReturnEmptyListByKeyWhenAbsent() {
        String key = uniqueKey();

        assertTrue(sessionDao.listByKey(key).isEmpty());
    }
}
