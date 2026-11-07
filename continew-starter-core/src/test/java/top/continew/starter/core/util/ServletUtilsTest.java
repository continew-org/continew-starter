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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.core.exceptions.UtilException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Servlet 工具类测试
 *
 * <p>
 * 通过 Mockito 固化 writeJson 写出 JSON 时的字符编码设置时序（编码先于容器 {@code getWriter()} 设置）
 * 与 content-type 行为，锁定中文数据按 UTF-8 写出的契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ServletUtilsTest {

    /**
     * 构造接收写出内容的响应对象
     *
     * @param writerSink 捕获写出内容的 StringWriter
     * @return 响应对象
     * @throws IOException {@code getWriter()} 声明的受检异常
     */
    private static HttpServletResponse buildResponseWithWriter(StringWriter writerSink)
        throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(writerSink, true));
        return response;
    }

    @Test
    @DisplayName("两参 writeJson 默认设置 UTF-8 编码，且严格早于 getWriter")
    void shouldSetUtf8EncodingBeforeGetWriter() throws IOException {
        StringWriter writerSink = new StringWriter();
        HttpServletResponse response = buildResponseWithWriter(writerSink);
        InOrder inOrder = inOrder(response);
        ServletUtils.writeJson(response, "长时间未操作，请重新登录");
        inOrder.verify(response).setCharacterEncoding(StandardCharsets.UTF_8.name());
        inOrder.verify(response).getWriter();
        assertThat(writerSink.toString()).isEqualTo("长时间未操作，请重新登录");
    }

    @Test
    @DisplayName("写出 JSON 时 content-type 保持 application/json 不变")
    void shouldKeepJsonContentType() throws IOException {
        HttpServletResponse response = buildResponseWithWriter(new StringWriter());
        ServletUtils.writeJson(response, "长时间未操作，请重新登录");
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("三参 writeJson 按指定字符编码设置，数据原文写出")
    void shouldUseSpecifiedCharset() throws IOException {
        StringWriter writerSink = new StringWriter();
        HttpServletResponse response = buildResponseWithWriter(writerSink);
        InOrder inOrder = inOrder(response);
        ServletUtils.writeJson(response, "您当前没有访问该租户的权限", "GBK");
        inOrder.verify(response).setCharacterEncoding("GBK");
        inOrder.verify(response).getWriter();
        assertThat(writerSink.toString()).isEqualTo("您当前没有访问该租户的权限");
    }

    @Test
    @DisplayName("getWriter 抛出 IOException 时保持 Hutool 的 UtilException 包装行为")
    void shouldWrapIoExceptionAsUtilException() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new IOException("writer unavailable"));
        assertThatThrownBy(() -> ServletUtils.writeJson(response, "长时间未操作，请重新登录"))
            .isInstanceOf(UtilException.class)
            .hasCauseInstanceOf(IOException.class);
    }
}
