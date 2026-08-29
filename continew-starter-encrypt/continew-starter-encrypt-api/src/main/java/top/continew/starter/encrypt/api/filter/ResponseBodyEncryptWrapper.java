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

package top.continew.starter.encrypt.api.filter;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.http.HttpHeaders;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.encrypt.util.EncryptUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 响应体加密包装类
 *
 * @author lishuyan
 * @since 2.14.0
 */
public class ResponseBodyEncryptWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream byteArrayOutputStream;
    private final ServletOutputStream servletOutputStream;
    private final PrintWriter printWriter;

    public ResponseBodyEncryptWrapper(HttpServletResponse response) throws IOException {
        super(response);
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.servletOutputStream = this.getOutputStream();
        this.printWriter = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream,
            StandardCharsets.UTF_8));
    }

    @Override
    public PrintWriter getWriter() {
        return printWriter;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (servletOutputStream != null) {
            servletOutputStream.flush();
        }
        if (printWriter != null) {
            printWriter.flush();
        }
    }

    @Override
    public void reset() {
        byteArrayOutputStream.reset();
    }

    public byte[] getResponseData() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toByteArray();
    }

    public String getContent() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }

    /**
     * 获取加密内容
     *
     * @param response        响应对象
     * @param publicKey       RSA公钥
     * @param secretKeyHeader 密钥头
     * @return 加密内容
     */
    public String getEncryptContent(HttpServletResponse response,
        String publicKey,
        String secretKeyHeader) throws IOException {
        // 生成 AES 密钥
        String aesSecretKey = RandomUtil.randomString(32);
        // Base64 编码
        String secretKeyByBase64 = EncryptUtils.encodeByBase64(aesSecretKey);
        // RSA 加密
        String secretKeyByRsa = EncryptUtils.encryptByRsa(secretKeyByBase64, publicKey);
        // 设置响应头
        response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, secretKeyHeader);
        response.setHeader(secretKeyHeader, secretKeyByRsa);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, StringConstants.ASTERISK);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, StringConstants.ASTERISK);
        response.setCharacterEncoding(CharsetUtil.UTF_8);
        // 通过 AES 密钥，对原始内容进行加密
        return EncryptUtils.encryptByAes(this.getContent(), aesSecretKey);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                byteArrayOutputStream.write(b, off, len);
            }
        };
    }

}
