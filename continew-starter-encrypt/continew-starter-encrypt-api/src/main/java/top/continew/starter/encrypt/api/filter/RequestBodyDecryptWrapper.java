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

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.MediaType;
import top.continew.starter.encrypt.util.EncryptUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 请求体解密包装类
 *
 * @author lishuyan
 * @since 2.14.0
 */
public class RequestBodyDecryptWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public RequestBodyDecryptWrapper(HttpServletRequest request,
        String privateKey,
        String secretKeyHeader) throws IOException {
        super(request);
        this.body = getDecryptContent(request, privateKey, secretKeyHeader);
    }

    /**
     * 获取解密后的请求体
     *
     * @param request         请求对象
     * @param privateKey      RSA私钥
     * @param secretKeyHeader 密钥头
     * @return 解密后的请求体
     * @throws IOException /
     */
    public byte[] getDecryptContent(HttpServletRequest request,
        String privateKey,
        String secretKeyHeader) throws IOException {
        // 通过 请求头 获取 AES 密钥，密钥内容经过 RSA 加密
        String secretKeyByRsa = request.getHeader(secretKeyHeader);
        // 通过 RSA 解密，获取 AES 密钥，密钥内容经过 Base64 编码
        String secretKeyByBase64 = EncryptUtils.decryptByRsa(secretKeyByRsa, privateKey);
        // 通过 Base64 解码，获取 AES 密钥
        String aesSecretKey = EncryptUtils.decodeByBase64(secretKeyByBase64);
        request.setCharacterEncoding(CharsetUtil.UTF_8);
        byte[] readBytes = IoUtil.readBytes(request.getInputStream(), false);
        String requestBody = new String(readBytes, StandardCharsets.UTF_8);
        // 通过 AES 密钥，解密 请求体
        return EncryptUtils.decryptByAes(requestBody, aesSecretKey)
            .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream stream = new ByteArrayInputStream(body);
        return new ServletInputStream() {

            @Override
            public int read() {
                return stream.read();
            }

            @Override
            public int available() {
                return body.length;
            }

            @Override
            public boolean isFinished() {
                return false;
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
