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

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.WebUtils;
import top.continew.starter.core.util.ServletUtils;
import top.continew.starter.core.wrapper.RepeatReadResponseWrapper;
import top.continew.starter.log.http.RecordableHttpResponse;

import java.util.Map;

/**
 * 可记录的 HTTP 响应信息适配器
 *
 * @author Andy Wilkinson（Spring Boot Actuator）
 * @author Charles7c
 * @author echo
 * @since 1.1.0
 */
public final class RecordableServletHttpResponse implements RecordableHttpResponse {

    private final HttpServletResponse response;
    private final int status;

    public RecordableServletHttpResponse(HttpServletResponse response) {
        this.response = response;
        this.status = response.getStatus();
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public Map<String, String> getHeaders() {
        return ServletUtils.getHeaderMap(response);
    }

    @Override
    public String getBody() {
        try {
            RepeatReadResponseWrapper wrappedResponse = WebUtils
                .getNativeResponse(response, RepeatReadResponseWrapper.class);
            if (wrappedResponse == null || wrappedResponse.isStreamingResponse()) {
                return null;
            }
            String body = wrappedResponse.getResponseContent();
            return JSONUtil.isTypeJSON(body) ? body : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getParams() {
        return this.getBody();
    }
}
