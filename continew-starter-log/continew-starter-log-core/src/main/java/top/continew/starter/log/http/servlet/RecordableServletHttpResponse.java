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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import top.continew.starter.core.wrapper.RepeatReadResponseWrapper;
import top.continew.starter.log.http.RecordableHttpResponse;
import top.continew.starter.web.util.ServletUtils;

import java.util.Map;

/**
 * 可记录的 HTTP 响应信息适配器
 *
 * @author Andy Wilkinson（Spring Boot Actuator）
 * @author Charles7c
 */
public final class RecordableServletHttpResponse implements RecordableHttpResponse {

    private final HttpServletResponse response;

    private final int status;

    public RecordableServletHttpResponse(HttpServletResponse response, int status) {
        this.response = response;
        this.status = status;
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
        if (response instanceof RepeatReadResponseWrapper wrapper && !wrapper.isStreamingResponse()) {
            String body = wrapper.getResponseContent();
            return JSONUtil.isTypeJSON(body) ? body : null;
        }
        return null;
    }

    @Override
    public Map<String, Object> getParam() {
        String body = this.getBody();
        return CharSequenceUtil.isNotBlank(body) && JSONUtil.isTypeJSON(body) ? JSONUtil.toBean(body, Map.class) : null;
    }
}
