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

package top.charles7c.continew.starter.log.common.model;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import top.charles7c.continew.starter.core.util.ExceptionUtils;
import top.charles7c.continew.starter.core.util.IpUtils;
import top.charles7c.continew.starter.core.util.ServletUtils;
import top.charles7c.continew.starter.log.common.enums.Include;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 请求信息
 *
 * @author Charles7c
 * @since 1.1.0
 */
@Data
public class LogRequest {

    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求 URI
     */
    private URI uri;

    /**
     * IP
     */
    private String ip;

    /**
     * 请求头
     */
    private Map<String, List<String>> headers;

    /**
     * 请求体（JSON 字符串）
     */
    private String body;

    /**
     * 请求参数
     */
    private Map<String, Object> param;

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
        this.uri = request.getUri();
        this.ip = request.getIp();
        this.headers = (includes.contains(Include.REQUEST_HEADERS)) ? request.getHeaders() : null;
        if (includes.contains(Include.REQUEST_BODY)) {
            this.body = request.getBody();
        } else if (includes.contains(Include.REQUEST_PARAM)) {
            this.param = request.getParam();
        }
        this.address = (includes.contains(Include.IP_ADDRESS)) ? IpUtils.getAddress(this.ip) : null;
        String userAgentString = ExceptionUtils.exToNull(() -> this.headers.get(HttpHeaders.USER_AGENT).get(0));
        this.browser = (includes.contains(Include.BROWSER)) ? ServletUtils.getBrowser(userAgentString) : null;
        this.os = (includes.contains(Include.OS)) ? ServletUtils.getOs(userAgentString) : null;
    }
}