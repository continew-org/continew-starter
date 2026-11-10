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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import top.continew.starter.core.wrapper.RepeatReadRequestWrapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * {@link RecordableServletHttpRequest} 请求信息适配器测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class RecordableServletHttpRequestTest {

    /**
     * 请求方式与路径应透传底层请求
     */
    @Test
    @DisplayName("透传请求方式与路径")
    void shouldExposeMethodAndPathFromUnderlyingRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getMethod()).isEqualTo("POST");
        assertThat(recordable.getPath()).isEqualTo("/api/users");
    }

    /**
     * 无查询字符串时 URL 不含问号部分
     */
    @Test
    @DisplayName("无查询字符串时 URL 不含问号")
    void urlShouldExcludeQueryPartWhenQueryStringAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getUrl()).isEqualTo(URI.create("http://localhost/api/users"));
    }

    /**
     * 空串查询字符串按无查询处理
     */
    @Test
    @DisplayName("空串查询按无查询处理（锁定当前行为）")
    void urlShouldTreatBlankQueryStringAsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setQueryString("");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getUrl().getRawQuery()).isNull();
    }

    /**
     * 查询字符串按预期拼接：合法原样、URI 非法字符编码后拼接、非 ASCII 原样保留（锁定当前行为）
     *
     * @param queryString      底层请求的查询字符串
     * @param expectedRawQuery  预期拼接后的 URL 查询部分
     */
    @ParameterizedTest(name = "[{index}] 查询串「{0}」拼接为「{1}」")
    @CsvSource({
        "'a=1&b=2', 'a=1&b=2'",
        "'name=zhang san', 'name=zhang%20san'",
        "'name=张三', 'name=张三'"})
    void urlShouldAppendQueryStringAsExpected(String queryString, String expectedRawQuery) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setQueryString(queryString);
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getUrl().getRawQuery()).isEqualTo(expectedRawQuery);
    }

    /**
     * 请求头应从底层请求收集
     */
    @Test
    @DisplayName("收集底层请求头")
    void shouldCollectHeadersFromUnderlyingRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("X-Test", "v1");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getHeaders()).containsEntry("X-Test", "v1");
    }

    /**
     * 请求 IP 默认取远程地址，存在代理头时取代理头
     */
    @Test
    @DisplayName("IP 取远程地址且代理头优先")
    void ipShouldFallbackToRemoteAddrAndPreferProxyHeader() {
        MockHttpServletRequest plainRequest = new MockHttpServletRequest("GET", "/api/users");
        RecordableServletHttpRequest plainRecordable =
            new RecordableServletHttpRequest(plainRequest);
        assertThat(plainRecordable.getIp()).isEqualTo("127.0.0.1");

        MockHttpServletRequest proxiedRequest = new MockHttpServletRequest("GET", "/api/users");
        proxiedRequest.addHeader("X-Forwarded-For", "203.0.113.1");
        RecordableServletHttpRequest proxiedRecordable =
            new RecordableServletHttpRequest(proxiedRequest);
        assertThat(proxiedRecordable.getIp()).isEqualTo("203.0.113.1");
    }

    /**
     * 未包装的请求体不可读取，返回 null
     */
    @Test
    @DisplayName("未包装请求体读取返回 null")
    void bodyShouldBeNullWithoutRepeatReadWrapper() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContentType("application/json");
        request.setContent("{\"id\":1}".getBytes(StandardCharsets.UTF_8));
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getBody()).isNull();
    }

    /**
     * 包装后的 JSON 请求体应可读取
     */
    @Test
    @DisplayName("包装后可读取 JSON 请求体")
    void bodyShouldReturnWrappedJsonContent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContentType("application/json");
        request.setContent("{\"id\":1}".getBytes(StandardCharsets.UTF_8));
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(
            new RepeatReadRequestWrapper(request));
        assertThat(recordable.getBody()).isEqualTo("{\"id\":1}");
    }

    /**
     * 包装后的非 JSON 请求体不记录，返回 null
     */
    @Test
    @DisplayName("非 JSON 请求体不记录")
    void bodyShouldBeNullForNonJsonContent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContentType("text/plain");
        request.setContent("plain text".getBytes(StandardCharsets.UTF_8));
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(
            new RepeatReadRequestWrapper(request));
        assertThat(recordable.getBody()).isNull();
    }

    /**
     * 文件上传请求体不记录，返回 null
     */
    @Test
    @DisplayName("文件上传请求体不记录")
    void bodyShouldBeNullForMultipartRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
        request.setContentType("multipart/form-data; boundary=----boundary");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(
            new RepeatReadRequestWrapper(request));
        assertThat(recordable.getBody()).isNull();
    }

    /**
     * 无请求参数时请求参数序列化为空对象 JSON
     */
    @Test
    @DisplayName("无请求参数序列化为空对象")
    void paramsShouldBeEmptyJsonObjectWithoutParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getParams()).isEqualTo("{}");
    }

    /**
     * 请求参数序列化为 JSON 对象字符串
     */
    @Test
    @DisplayName("请求参数序列化为 JSON 对象")
    void paramsShouldSerializeParameterMapAsJson() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addParameter("page", "1");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(request);
        assertThat(recordable.getParams()).isEqualTo("{\"page\":\"1\"}");
    }

    /**
     * 含 JSON 请求体时请求参数取请求体而非表单参数
     */
    @Test
    @DisplayName("JSON 请求体优先于表单参数")
    void paramsShouldPreferJsonBodyOverFormParameters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContentType("application/json");
        request.setContent("{\"id\":1}".getBytes(StandardCharsets.UTF_8));
        request.addParameter("page", "1");
        RecordableServletHttpRequest recordable = new RecordableServletHttpRequest(
            new RepeatReadRequestWrapper(request));
        assertThat(recordable.getParams()).isEqualTo("{\"id\":1}");
    }
}
