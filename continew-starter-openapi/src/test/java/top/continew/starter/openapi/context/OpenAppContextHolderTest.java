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

package top.continew.starter.openapi.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 开放应用上下文 ThreadLocal 生命周期测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class OpenAppContextHolderTest {

    @AfterEach
    void cleanUp() {
        OpenAppContextHolder.remove();
    }

    /**
     * set 后 get 可见，remove 后为 null
     */
    @Test
    void contextLifecycleShouldFollowSetGetRemove() {
        assertThat(OpenAppContextHolder.getContext()).isNull();
        OpenAppContext context = new OpenAppContext(1L, "ak", "GET:/open/api/demo", "request-1");
        OpenAppContextHolder.setContext(context);
        assertThat(OpenAppContextHolder.getContext()).isSameAs(context);
        OpenAppContextHolder.remove();
        assertThat(OpenAppContextHolder.getContext()).isNull();
    }

    /**
     * 并发 100 线程各自 set/get，上下文互不串值
     */
    @Test
    void concurrentThreadsShouldNotLeakContext() throws Exception {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            long appId = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                OpenAppContextHolder.setContext(
                    new OpenAppContext(appId, "ak-" + appId, "GET:/open", "req-" + appId));
                OpenAppContext got = OpenAppContextHolder.getContext();
                boolean isolated = got != null && got.getAppId() == appId;
                OpenAppContextHolder.remove();
                return isolated && OpenAppContextHolder.getContext() == null;
            }));
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<Boolean> future : futures) {
            assertThat(future.get(5, TimeUnit.SECONDS)).isTrue();
        }
        executor.shutdown();
    }

    /**
     * 上下文字段仅含开放应用身份语义，不含登录用户字段
     */
    @Test
    void contextShouldCarryOnlyOpenAppFields() {
        OpenAppContext context =
            new OpenAppContext(1L, "ak-1", "POST:/open/api/order/create", "req-1");
        assertThat(context.getAppId()).isEqualTo(1L);
        assertThat(context.getAccessKey()).isEqualTo("ak-1");
        assertThat(context.getAuthorizedApi()).isEqualTo("POST:/open/api/order/create");
        assertThat(context.getRequestId()).isEqualTo("req-1");
    }
}
