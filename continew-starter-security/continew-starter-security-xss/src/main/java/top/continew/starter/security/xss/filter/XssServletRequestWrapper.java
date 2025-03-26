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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.EscapeUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.Method;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.security.xss.autoconfigure.XssProperties;
import top.continew.starter.security.xss.enums.XssMode;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * 针对 XssServletRequest 进行过滤的包装类
 *
 * @author whhya
 * @since 2.0.0
 */
public class XssServletRequestWrapper extends HttpServletRequestWrapper {

    private final XssProperties xssProperties;

    private String body = "";

    public XssServletRequestWrapper(HttpServletRequest request, XssProperties xssProperties) throws IOException {
        super(request);
        this.xssProperties = xssProperties;
        if (CharSequenceUtil.equalsAnyIgnoreCase(request.getMethod().toUpperCase(), Method.POST.name(), Method.PATCH
            .name(), Method.PUT.name())) {
            body = IoUtil.getReader(request.getReader()).readLine();
            if (CharSequenceUtil.isBlank(body)) {
                return;
            }
            body = this.handleTag(body);
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
        return this.handleTag(super.getQueryString());
    }

    @Override
    public String getParameter(String name) {
        return this.handleTag(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (ArrayUtil.isEmpty(values)) {
            return values;
        }
        int length = values.length;
        String[] resultValues = new String[length];
        for (int i = 0; i < length; i++) {
            resultValues[i] = this.handleTag(values[i]);
        }
        return resultValues;
    }

    /**
     * 对文本内容进行 XSS 处理
     *
     * @param content 文本内容
     * @return 返回处理过后内容
     */
    private String handleTag(String content) {
        if (CharSequenceUtil.isBlank(content)) {
            return content;
        }
        XssMode mode = xssProperties.getMode();
        // 转义
        if (XssMode.ESCAPE.equals(mode)) {
            List<String> reStr = ReUtil.findAllGroup0(HtmlUtil.RE_HTML_MARK, content);
            if (CollUtil.isEmpty(reStr)) {
                return content;
            }
            for (String s : reStr) {
                content = content.replace(s, EscapeUtil.escapeHtml4(s)
                    .replace(StringConstants.BACKSLASH, StringConstants.EMPTY));
            }
            return content;
        }
        // 清理
        return HtmlUtil.cleanHtmlTag(content);
    }

    static ServletInputStream getServletInputStream(String body) {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        return new ServletInputStream() {
            @Override
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
                // 设置监听器
            }
        };
    }

}
