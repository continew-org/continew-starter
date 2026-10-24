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

package top.continew.starter.trace.filter;

import cn.hutool.core.text.CharSequenceUtil;
import io.micrometer.tracing.Tracer;
import top.continew.starter.trace.autoconfigure.TraceProperties;
import top.continew.starter.trace.util.TraceUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 链路追踪响应过滤器
 *
 * <p>
 * 将当前请求的链路 ID 写入响应头，便于前端获取整条链路的标识。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class TraceResponseFilter implements Filter {

    private final Tracer tracer;

    private final TraceProperties traceProperties;

    public TraceResponseFilter(Tracer tracer, TraceProperties traceProperties) {
        this.tracer = tracer;
        this.traceProperties = traceProperties;
    }

    @Override
    public void doFilter(ServletRequest request,
        ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest
            && response instanceof HttpServletResponse httpServletResponse) {
            String traceIdName = traceProperties.getTraceIdName();
            String traceId = TraceUtils.getTraceId(tracer);
            if (CharSequenceUtil.isNotBlank(traceIdName) && CharSequenceUtil.isNotBlank(traceId)) {
                httpServletResponse.addHeader(traceIdName, traceId);
            }
        }
        chain.doFilter(request, response);
    }
}
