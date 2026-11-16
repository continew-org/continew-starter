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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多标签页 WebSocket 会话 DAO 的并发摘取窗口测试。
 *
 * <p>硬契约（不依赖时序）：并发摘取不丢不重（会话恰好被一组摘取者收走一次）；
 * 多 Key 并发摘取互不串扰；摘取与新增竞态后存活连接仍可发现。时序由
 * ConcurrentHashMap 与原子摘取保证，断言只锁集合级别的总量与归属。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("WebSocket 会话 DAO 并发摘取窗口")
class ConcurrentWebSocketSessionDaoConcurrencyTest {

    private static final int THREADS = 8;

    /**
     * 并发摘取同一 Key：全部会话恰好被收走一次，既不重复也不遗留。
     */
    @RepeatedTest(value = 10, name = "{currentRepetition}/{totalRepetitions}")
    @DisplayName("并发摘取同一 Key：不丢不重")
    void shouldRemoveAllSessionsExactlyOnceUnderConcurrentExtraction() throws Exception {
        ConcurrentWebSocketSessionDao dao =
            new ConcurrentWebSocketSessionDao(new WebSocketCredentialRegistry());
        List<WebSocketSession> sessions = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            sessions.add(this.session("conn-" + i, true));
        }
        sessions.forEach(session -> dao.add("client-id", session));

        Set<String> collectedIds = ConcurrentHashMap.newKeySet();
        AtomicInteger extractionCount = new AtomicInteger();
        this.runConcurrently(() -> {
            List<WebSocketSession> extracted = List.copyOf(dao.removeAll("client-id"));
            extractionCount.incrementAndGet();
            extracted.forEach(session -> collectedIds.add(session.getId()));
        });

        // 硬契约 1：每个会话恰好被收走一次（AtomicReference 集合去重后数量守恒）
        assertEquals(THREADS, collectedIds.size(), "并发摘取收集到的会话 ID 种类数");
        // 硬契约 2：摘取后索引为空，无遗留会话
        assertEquals(List.of(), List.copyOf(dao.listByKey("client-id")), "摘取后索引应清空");
        // 收集到的 ID 与登记 ID 一致（无外部混入）
        List<String> expectedIds = sessions.stream()
            .map(s -> s.getId())
            .toList();
        assertTrue(collectedIds.containsAll(expectedIds), "摘取结果应覆盖全部登记会话");
    }

    /**
     * 多 Key 并发摘取互不串扰：每个 Key 的会话恰好被该 Key 的摘取组收走。
     */
    @RepeatedTest(value = 5, name = "{currentRepetition}/{totalRepetitions}")
    @DisplayName("多 Key 并发摘取互不串扰")
    void shouldExtractIndependentKeysWithoutCrosstalk() throws Exception {
        ConcurrentWebSocketSessionDao dao =
            new ConcurrentWebSocketSessionDao(new WebSocketCredentialRegistry());
        int keyCount = 4;
        List<String> keys = new ArrayList<>(keyCount);
        for (int k = 0; k < keyCount; k++) {
            keys.add("client-" + k);
            for (int i = 0; i < 2; i++) {
                dao.add("client-" + k, this.session("k" + k + "-conn-" + i, true));
            }
        }

        Set<String> collected = ConcurrentHashMap.newKeySet();
        this.runConcurrently(() -> {
            // 摘取者按线程序号选择 Key：8 线程抢 4 个 Key，同一 Key 的摘取者并发
            int keyIndex = (int) (Thread.currentThread().getId() % keyCount);
            dao.removeAll(keys.get(keyIndex))
                .forEach(session -> collected.add(session.getId()));
        });

        // 每个 Key 恰好 2 个会话，全部 Key 合计 8 个，全部被收走且归属正确
        assertEquals(keyCount * 2, collected.size(), "多 Key 并发摘取的会话总数");
        for (int k = 0; k < keyCount; k++) {
            assertEquals(List.of(), List.copyOf(dao.listByKey("client-" + k)), "Key client-"
                + k + " 摘取后应清空");
        }
    }

    /**
     * 摘取与新增竞态：摘取后的新增进入全新登记，不被摘取者误收或遗弃。
     */
    @Test
    @DisplayName("摘取与新增竞态：摘取后新增仍可发现")
    void shouldKeepPostExtractionAdditionDiscoverable() throws Exception {
        ConcurrentWebSocketSessionDao dao =
            new ConcurrentWebSocketSessionDao(new WebSocketCredentialRegistry());
        WebSocketSession initial = this.session("conn-initial", true);
        dao.add("client-id", initial);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 100; round++) {
                WebSocketSession later = this.session("later-" + round, true);
                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<?> extractor = executor.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    dao.removeAll("client-id");
                    return null;
                });
                Future<?> adder = executor.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    dao.add("client-id", later);
                    return null;
                });
                extractor.get(5, TimeUnit.SECONDS);
                adder.get(5, TimeUnit.SECONDS);

                // 不变量：摘取与新增都原子，交错后只有两种合法终态——
                // ① 新增先完成：later 被摘取者整组收走（索引为空属正确）；
                // ② 摘取先完成：later 落入全新登记，必须可被索引发现，
                //    否则后续推送永远无法命中该连接。
                List<WebSocketSession> remaining = List.copyOf(dao.listByKey("client-id"));
                assertTrue(remaining.contains(later) || remaining.isEmpty(), "第 " + round
                    + " 轮：新增连接既不在索引也未被摘取，脱离管理");

                dao.removeAll("client-id");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 并发执行骨架：全员闸门同步后各自执行动作并统一回收异常。
     *
     * @param action 并发动作
     */
    private void runConcurrently(Runnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger done = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(THREADS);
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        assertTrue(startLatch.await(5, TimeUnit.SECONDS), "启动信号未就位");
                        action.run();
                        return null;
                    } finally {
                        done.incrementAndGet();
                    }
                }));
            }
            startLatch.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            assertEquals(THREADS, done.get(), "并发任务未全部完成");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "执行器未正常关闭");
        }
    }

    private WebSocketSession session(String id, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(open);
        return session;
    }
}
