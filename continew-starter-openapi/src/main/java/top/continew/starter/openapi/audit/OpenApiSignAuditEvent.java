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

package top.continew.starter.openapi.audit;

import java.time.Instant;

/**
 * 开放接口验签审计事件
 *
 * <p>按应用粒度记录验签结果。密钥不进入本对象，accessKey 以脱敏形式
 * （前 6 位 + **** + 后 4 位）携带；请求体原文不进入本对象。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class OpenApiSignAuditEvent {

    /**
     * 事件类型：验签通过
     */
    public static final String SIGN_SUCCESS = "SIGN_SUCCESS";

    /**
     * 事件类型：验签拒绝
     */
    public static final String SIGN_DENIED = "SIGN_DENIED";

    /**
     * 事件类型：授权拒绝
     */
    public static final String APP_DENIED = "APP_DENIED";

    /**
     * 事件类型（取 SIGN_SUCCESS、SIGN_DENIED、APP_DENIED 之一）
     */
    private final String eventType;

    /**
     * 应用 ID（头解析失败等无法识别应用时为 null）
     */
    private final Long appId;

    /**
     * 脱敏后的 accessKey（前 6 位 + **** + 后 4 位）
     */
    private final String accessKey;

    /**
     * HTTP 方法
     */
    private final String method;

    /**
     * 请求路径（不含 context-path）
     */
    private final String path;

    /**
     * 失败错误码（成功时为 null）
     */
    private final String errorCode;

    /**
     * 请求关联 ID
     */
    private final String requestId;

    /**
     * 链路追踪 ID（链路上下文缺失时为 null）
     */
    private final String traceId;

    /**
     * 事件发生时间
     */
    private final Instant occurredAt;

    /**
     * 客户端 IP
     */
    private final String clientIp;

    /**
     * 私有全参构造：外部统一经由 {@link #builder()} 组装，避免多参构造器误传同型字段
     *
     * @param eventType  事件类型（取 SIGN_SUCCESS、SIGN_DENIED、APP_DENIED 之一）
     * @param appId      应用 ID（未识别时为 null）
     * @param accessKey  脱敏后的 accessKey
     * @param method     HTTP 方法
     * @param path       请求路径（不含 context-path）
     * @param errorCode  失败错误码（成功时为 null）
     * @param requestId  请求关联 ID
     * @param traceId    链路追踪 ID（可为 null）
     * @param occurredAt 事件发生时间
     * @param clientIp   客户端 IP
     */
    private OpenApiSignAuditEvent(String eventType, Long appId, String accessKey, String method,
        String path, String errorCode, String requestId, String traceId, Instant occurredAt,
        String clientIp) {
        this.eventType = eventType;
        this.appId = appId;
        this.accessKey = accessKey;
        this.method = method;
        this.path = path;
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.traceId = traceId;
        this.occurredAt = occurredAt;
        this.clientIp = clientIp;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getAppId() {
        return appId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getClientIp() {
        return clientIp;
    }

    /**
     * accessKey 脱敏：保留前 6 位与后 4 位，中间以 **** 掩盖
     *
     * <p>长度不足以保留前后的密文整体掩盖为固定掩码，避免短值被局部展示后猜测。</p>
     *
     * @param accessKey 原始 accessKey
     * @return 脱敏后的 accessKey
     */
    public static String maskAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isEmpty()) {
            return "";
        }
        int maskKeepLength = 10;
        if (accessKey.length() < maskKeepLength) {
            return "****";
        }
        return accessKey.substring(0, 6) + "****" + accessKey.substring(accessKey.length() - 4);
    }

    /**
     * 创建审计事件构建器
     *
     * @return 审计事件构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 审计事件构建器（字段与事件一一对应，链式组装后 {@link #build()} 出不可变事件）
     */
    public static class Builder {

        private String eventType;

        private Long appId;

        private String accessKey;

        private String method;

        private String path;

        private String errorCode;

        private String requestId;

        private String traceId;

        private Instant occurredAt;

        private String clientIp;

        /**
         * 设置事件类型
         *
         * @param eventType 事件类型（取 SIGN_SUCCESS、SIGN_DENIED、APP_DENIED 之一）
         * @return 当前构建器
         */
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        /**
         * 设置应用 ID
         *
         * @param appId 应用 ID（未识别时为 null）
         * @return 当前构建器
         */
        public Builder appId(Long appId) {
            this.appId = appId;
            return this;
        }

        /**
         * 设置脱敏后的 accessKey
         *
         * @param accessKey 脱敏后的 accessKey
         * @return 当前构建器
         */
        public Builder accessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        /**
         * 设置 HTTP 方法
         *
         * @param method HTTP 方法
         * @return 当前构建器
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * 设置请求路径
         *
         * @param path 请求路径（不含 context-path）
         * @return 当前构建器
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * 设置失败错误码
         *
         * @param errorCode 失败错误码（成功时为 null）
         * @return 当前构建器
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * 设置请求关联 ID
         *
         * @param requestId 请求关联 ID
         * @return 当前构建器
         */
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * 设置链路追踪 ID
         *
         * @param traceId 链路追踪 ID（可为 null）
         * @return 当前构建器
         */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * 设置事件发生时间
         *
         * @param occurredAt 事件发生时间
         * @return 当前构建器
         */
        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        /**
         * 设置客户端 IP
         *
         * @param clientIp 客户端 IP
         * @return 当前构建器
         */
        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        /**
         * 构建审计事件
         *
         * @return 审计事件
         */
        public OpenApiSignAuditEvent build() {
            return new OpenApiSignAuditEvent(eventType, appId, accessKey, method, path, errorCode,
                requestId, traceId, occurredAt, clientIp);
        }
    }
}
