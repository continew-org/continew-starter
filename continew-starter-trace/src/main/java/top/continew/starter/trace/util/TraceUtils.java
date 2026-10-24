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

package top.continew.starter.trace.util;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;

/**
 * 链路追踪工具类
 *
 * @author Charles7c
 * @since 3.0.0
 */
public final class TraceUtils {

    /**
     * 链路 ID 的 MDC 键名（Micrometer Tracing 默认关联字段）
     */
    public static final String MDC_TRACE_ID = "traceId";

    /**
     * Span ID 的 MDC 键名（Micrometer Tracing 默认关联字段）
     */
    public static final String MDC_SPAN_ID = "spanId";

    private TraceUtils() {
    }

    /**
     * 获取当前链路 ID
     *
     * @return 链路 ID；当前线程无链路上下文时返回 null
     */
    public static String getTraceId() {
        return MDC.get(MDC_TRACE_ID);
    }

    /**
     * 获取当前链路 ID
     *
     * @param tracer Tracer
     * @return 链路 ID；当前上下文无 Span 时返回 null
     */
    public static String getTraceId(Tracer tracer) {
        Span span = tracer.currentSpan();
        return span == null ? null : span.context().traceId();
    }

    /**
     * 获取当前 Span ID
     *
     * @return Span ID；当前线程无链路上下文时返回 null
     */
    public static String getSpanId() {
        return MDC.get(MDC_SPAN_ID);
    }
}
