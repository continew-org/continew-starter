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

package top.continew.starter.security.xss.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import top.continew.starter.security.xss.autoconfigure.XssProperties;
import top.continew.starter.security.xss.enums.XssMode;

import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * XSS 请求包装器过滤行为测试
 *
 * <p>覆盖 {@link XssServletRequestWrapper} 的三条过滤入口：写方法请求体（POST/PATCH/PUT）、
 * 查询串与请求参数，并验证 CLEAN（删标签）与 ESCAPE（转实体）两种模式、字符编码回落、
 * 空白内容跳过，以及包装后输入流的读取状态。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class XssServletRequestWrapperTest {

    private static final Charset GBK = Charset.forName("GBK");

    /**
     * GET 请求不读取请求体，包装器内容保持为空。
     */
    @Test
    void shouldKeepBodyEmptyForGetRequest() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(this.readBody(wrapper)).isEmpty();
    }

    /**
     * POST 请求体在默认清理模式下移除 HTML 标签。
     */
    @Test
    void shouldCleanHtmlTagsInPostBody() throws IOException {
        assertThat(this.filteredBody("POST", "<p>hello</p>", new XssProperties()))
            .isEqualTo("hello");
    }

    /**
     * PATCH 与 PUT 请求体同样进入过滤流程。
     */
    @Test
    void shouldCleanHtmlTagsInPatchAndPutBody() throws IOException {
        assertThat(this.filteredBody("PATCH", "<p>patch</p>", new XssProperties()))
            .isEqualTo("patch");
        assertThat(this.filteredBody("PUT", "<p>put</p>", new XssProperties())).isEqualTo("put");
    }

    /**
     * 空白请求体直接跳过过滤，原样保留。
     */
    @Test
    void shouldKeepBlankBodyAsIs() throws IOException {
        assertThat(this.filteredBody("POST", "   ", new XssProperties())).isEqualTo("   ");
    }

    /**
     * 转义模式下标签被 HTML 实体转义而非删除。
     */
    @Test
    void shouldEscapeHtmlTagsInBodyWhenEscapeMode() throws IOException {
        XssProperties properties = new XssProperties();
        properties.setMode(XssMode.ESCAPE);
        assertThat(this.filteredBody("POST", "<b>bold</b>", properties))
            .isEqualTo("&lt;b&gt;bold&lt;/b&gt;");
    }

    /**
     * 转义模式下不含 HTML 标记的请求体原样保留。
     */
    @Test
    void shouldKeepPlainBodyWhenEscapeMode() throws IOException {
        XssProperties properties = new XssProperties();
        properties.setMode(XssMode.ESCAPE);
        assertThat(this.filteredBody("POST", "plain", properties)).isEqualTo("plain");
    }

    /**
     * 未显式声明字符编码时按 UTF-8 读取；声明 GBK 时按声明编码读取。
     */
    @Test
    void shouldReadBodyWithDeclaredCharacterEncoding() throws IOException {
        assertThat(this.filteredBody("POST", "中文内容", new XssProperties())).isEqualTo("中文内容");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setCharacterEncoding("GBK");
        request.setContent("中文内容".getBytes(GBK));
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(this.readBody(wrapper)).isEqualTo("中文内容");
    }

    /**
     * 查询串在清理模式下移除 HTML 标签。
     */
    @Test
    void shouldCleanHtmlTagsInQueryString() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("q=<script>alert(1)</script>");
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(wrapper.getQueryString()).isEqualTo("q=alert(1)");
    }

    /**
     * 无查询串时返回 null，不做过滤。
     */
    @Test
    void shouldReturnNullQueryStringWhenAbsent() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(wrapper.getQueryString()).isNull();
    }

    /**
     * 单值参数在清理模式下移除 HTML 标签。
     */
    @Test
    void shouldCleanHtmlTagsInParameter() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("name", "<p>tom</p>");
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(wrapper.getParameter("name")).isEqualTo("tom");
    }

    /**
     * 不存在的参数返回 null，不做过滤。
     */
    @Test
    void shouldReturnNullParameterWhenAbsent() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(wrapper.getParameter("missing")).isNull();
    }

    /**
     * 多值参数逐个过滤且顺序保持。
     */
    @Test
    void shouldCleanEachParameterValue() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("hobby", "<p>a</p>", "<p>b</p>");
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(wrapper.getParameterValues("hobby")).containsExactly("a", "b");
    }

    /**
     * 不存在的参数取值数组原样返回。
     */
    @Test
    void shouldReturnOriginalValuesWhenParameterAbsent() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        XssServletRequestWrapper wrapper =
            new XssServletRequestWrapper(request, new XssProperties());
        assertThat(wrapper.getParameterValues("missing")).isNull();
    }

    /**
     * 包装后的输入流可完整读出过滤后的请求体，读完即处于 finished 状态。
     */
    @Test
    void shouldExposeFilteredBodyThroughInputStream() throws IOException {
        XssServletRequestWrapper wrapper =
            this.wrapperOf("POST", "<p>body</p>", new XssProperties());
        ServletInputStream inputStream = wrapper.getInputStream();
        assertThat(inputStream.isFinished()).isFalse();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        inputStream.transferTo(output);
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("body");
        assertThat(inputStream.isFinished()).isTrue();
    }

    /**
     * 非异步模式下 isReady 恒为 false，setReadListener 为空操作。
     */
    @Test
    void shouldStayNotReadyAndIgnoreReadListener() throws IOException {
        XssServletRequestWrapper wrapper = this.wrapperOf("POST", "x", new XssProperties());
        ServletInputStream inputStream = wrapper.getInputStream();
        assertThat(inputStream.isReady()).isFalse();
        assertThatCode(() -> inputStream.setReadListener(null)).doesNotThrowAnyException();
    }

    /**
     * 构造指定方法与请求体的包装器并返回过滤后的请求体文本。
     *
     * @param method     请求方法
     * @param content    原始请求体
     * @param properties 配置属性
     * @return 过滤后的请求体
     */
    private String filteredBody(String method, String content, XssProperties properties)
        throws IOException {
        return this.readBody(this.wrapperOf(method, content, properties));
    }

    /**
     * 构造指定方法与请求体的包装器。
     *
     * @param method     请求方法
     * @param content    原始请求体
     * @param properties 配置属性
     * @return 请求包装器
     */
    private XssServletRequestWrapper wrapperOf(String method, String content,
        XssProperties properties)
        throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setContent(content.getBytes(StandardCharsets.UTF_8));
        return new XssServletRequestWrapper(request, properties);
    }

    /**
     * 读取包装器当前请求体的文本内容。
     *
     * @param wrapper 请求包装器
     * @return 请求体文本
     */
    private String readBody(XssServletRequestWrapper wrapper) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        wrapper.getInputStream().transferTo(output);
        return output.toString(StandardCharsets.UTF_8);
    }
}
