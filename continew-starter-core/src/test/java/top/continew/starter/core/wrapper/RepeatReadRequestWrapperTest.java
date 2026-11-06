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

package top.continew.starter.core.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读包装器有界构造测试：超限即抛，未超限时行为与单参构造一致
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RepeatReadRequestWrapperTest {

    /**
     * 请求体累计超过上限时有界构造立即抛出超限异常（IOException 子类），不读完整个流
     */
    @Test
    void boundedConstructorShouldThrowWhenBodyExceedsMaxBytes() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setContentType("application/json");
        request.setContent(new byte[100]);
        assertThatThrownBy(() -> new RepeatReadRequestWrapper(request, 50))
            .isInstanceOf(RequestBodySizeExceededException.class)
            .isInstanceOf(IOException.class);
    }

    /**
     * 请求体未超限时有界构造与单参构造行为一致：缓存同一份字节且流可重复读取
     */
    @Test
    void boundedConstructorShouldBehaveSameAsUnboundedWithinLimit() throws IOException {
        byte[] body = "{\"amount\":100,\"currency\":\"CNY\"}".getBytes(StandardCharsets.UTF_8);
        RepeatReadRequestWrapper bounded = wrap(body, 1024L);
        RepeatReadRequestWrapper unbounded = wrap(body, null);

        assertThat(bounded.getCachedContent().toByteArray()).isEqualTo(unbounded.getCachedContent()
            .toByteArray());
        assertThat(bounded.getContentAsString()).isEqualTo(unbounded.getContentAsString());
        assertThat(bounded.getInputStream().readAllBytes()).isEqualTo(body);
    }

    /**
     * 以指定上限构造包装器（null 表示走单参无界构造）
     *
     * @param body     请求体字节
     * @param maxBytes 上限（字节），null 表示无界
     * @return 包装后的请求对象
     */
    private RepeatReadRequestWrapper wrap(byte[] body, Long maxBytes) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setContentType("application/json");
        request.setContent(body);
        return maxBytes == null
            ? new RepeatReadRequestWrapper(request)
            : new RepeatReadRequestWrapper(request, maxBytes);
    }
}
