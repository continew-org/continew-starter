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

package top.continew.starter.log.model;

import cn.hutool.core.text.CharSequenceUtil;
import org.springframework.http.HttpHeaders;
import top.continew.starter.core.util.ExceptionUtils;
import top.continew.starter.core.util.IpUtils;
import top.continew.starter.core.util.ServletUtils;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.http.RecordableHttpRequest;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * 请求信息
 *
 * @author Charles7c
 * @since 1.1.0
 */
public class LogRequest {

    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求 URL
     */
    private URI url;

    /**
     * IP
     */
    private String ip;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 请求体（JSON 字符串）
     */
    private String body;

    /**
     * 请求参数
     */
    private String params;

    /**
     * IP 归属地
     */
    private String address;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    public LogRequest(RecordableHttpRequest request, Set<Include> includes) {
        this.method = request.getMethod();
        this.url = request.getUrl();
        this.ip = request.getIp();
        boolean includeHeaders = includes.contains(Include.REQUEST_HEADERS);
        this.headers = includeHeaders ? request.getHeaders() : null;
        if (includes.contains(Include.REQUEST_BODY)) {
            this.body = request.getBody();
        } else if (includes.contains(Include.REQUEST_PARAM)) {
            this.params = request.getParams();
        }
        this.address = (includes.contains(Include.IP_ADDRESS))
            ? ExceptionUtils.exToNull(() -> IpUtils.getIpv4Address(this.ip))
            : null;
        boolean includeBrowser = includes.contains(Include.BROWSER);
        boolean includeOs = includes.contains(Include.OS);
        if (!includeBrowser && !includeOs) {
            return;
        }
        String userAgent = null;
        if (includeHeaders) {
            userAgent = findRecordedUserAgent();
        } else if (request instanceof RecordableHttpRequest.WithUserAgent userAgentRequest) {
            userAgent = userAgentRequest.readUserAgent();
        }
        if (CharSequenceUtil.isNotBlank(userAgent)) {
            this.browser = includeBrowser ? ServletUtils.getBrowser(userAgent) : null;
            this.os = includeOs ? ServletUtils.getOs(userAgent) : null;
        }
    }

    private String findRecordedUserAgent() {
        if (this.headers == null) {
            return null;
        }
        return this.headers.entrySet()
            .stream()
            .filter(header -> HttpHeaders.USER_AGENT.equalsIgnoreCase(header.getKey()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(null);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
