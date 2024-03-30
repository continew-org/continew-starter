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

package top.charles7c.continew.starter.web.autoconfigure.xss;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * 针对XssServletRequest进行过滤的包装类
 *
 * @author whh
 * @since 1.0.0
 */
public class XssServletRequestWrapper extends HttpServletRequestWrapper {

    private String body = "";
    String[] method = {"POST", "PATCH", "PUT"};

    public XssServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        if (StrUtil.containsAny(request.getMethod().toUpperCase(), method)) {
            body = IoUtil.getReader(request.getReader()).readLine();
            if (StrUtil.isNotEmpty(body)) {
                body = cleanHtmlTag(body);
            }
        }

    }

    @Override
    public BufferedReader getReader() {
        return IoUtil.toBuffered(new StringReader(body));
    }

    @Override
    public ServletInputStream getInputStream() {
        return getServletInputStream(body);
    }

    @Override
    public String getQueryString() {
        return cleanHtmlTag(super.getQueryString());
    }

    @Override
    public String getParameter(String name) {
        return cleanHtmlTag(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (ArrayUtil.isEmpty(values)) {
            return values;
        }
        int length = values.length;
        String[] escapeValues = new String[length];
        for (int i = 0; i < length; i++) {
            escapeValues[i] = cleanHtmlTag(values[i]);
        }
        return escapeValues;
    }

    /**
     * 清除文本中所有HTML标签，但是不删除标签内的内容
     *
     * @param content 文本内容
     * @return 处理过后的文本
     */
    private String cleanHtmlTag(String content) {
        if (StrUtil.isEmpty(content)) {
            return content;
        }
        return HtmlUtil.cleanHtmlTag(content);
    }

    static ServletInputStream getServletInputStream(String body) {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        return new ServletInputStream() {
            public int read() {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

        };
    }
}
