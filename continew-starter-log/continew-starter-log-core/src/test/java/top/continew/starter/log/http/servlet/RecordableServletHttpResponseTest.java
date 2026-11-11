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

package top.continew.starter.log.http.servlet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.core.wrapper.RepeatReadResponseWrapper;

/**
 * {@link RecordableServletHttpResponse} 响应信息适配器测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RecordableServletHttpResponseTest {

    /**
     * 状态码应取构造时刻的值
     */
    @Test
    @DisplayName("状态码取构造时刻快照")
    void statusShouldCaptureValueAtConstructionTime() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(response);
        assertThat(recordable.getStatus()).isEqualTo(200);
    }

    /**
     * 锁定当前行为：构造后底层状态码再变化不影响已记录的状态码
     */
    @Test
    @DisplayName("构造后状态码变化不影响记录值（锁定当前行为）")
    void statusShouldNotTrackLaterStatusChanges() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(response);
        response.setStatus(500);
        assertThat(recordable.getStatus()).isEqualTo(200);
    }

    /**
     * 响应头应从底层响应收集
     */
    @Test
    @DisplayName("收集底层响应头")
    void shouldCollectHeadersFromUnderlyingResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("X-Resp", "ok");
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(response);
        assertThat(recordable.getHeaders()).containsEntry("X-Resp", "ok");
    }

    /**
     * 未包装的响应体不可读取，返回 null
     */
    @Test
    @DisplayName("未包装响应体读取返回 null")
    void bodyShouldBeNullWithoutRepeatReadWrapper() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(response);
        assertThat(recordable.getBody()).isNull();
    }

    /**
     * 包装后的 JSON 响应体应可读取
     */
    @Test
    @DisplayName("包装后可读取 JSON 响应体")
    void bodyShouldReturnWrappedJsonContent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RepeatReadResponseWrapper wrapper = new RepeatReadResponseWrapper(response);
        wrapper.getWriter().write("{\"ok\":true}");
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(wrapper);
        assertThat(recordable.getBody()).isEqualTo("{\"ok\":true}");
    }

    /**
     * 包装后的非 JSON 响应体不记录，返回 null
     */
    @Test
    @DisplayName("非 JSON 响应体不记录")
    void bodyShouldBeNullForNonJsonContent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RepeatReadResponseWrapper wrapper = new RepeatReadResponseWrapper(response);
        wrapper.getWriter().write("plain text");
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(wrapper);
        assertThat(recordable.getBody()).isNull();
    }

    /**
     * SSE 流式响应不做缓存记录，返回 null
     */
    @Test
    @DisplayName("SSE 流式响应体不记录")
    void bodyShouldBeNullForStreamingResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/event-stream");
        RepeatReadResponseWrapper wrapper = new RepeatReadResponseWrapper(response);
        assertThat(wrapper.isStreamingResponse()).isTrue();
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(wrapper);
        assertThat(recordable.getBody()).isNull();
    }

    /**
     * 响应参数与响应体取同一内容
     */
    @Test
    @DisplayName("响应参数与响应体同值")
    void paramsShouldMirrorBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RepeatReadResponseWrapper wrapper = new RepeatReadResponseWrapper(response);
        wrapper.getWriter().write("{\"ok\":true}");
        RecordableServletHttpResponse recordable = new RecordableServletHttpResponse(wrapper);
        assertThat(recordable.getParams()).isEqualTo(recordable.getBody());
    }
}
