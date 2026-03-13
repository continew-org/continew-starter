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

import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.http.RecordableHttpResponse;

import java.time.Instant;

/**
 * 访问日志上下文
 *
 * @author echo
 * @since 2.10.0
 */
public class AccessLogContext {

    /**
     * 开始时间
     */
    private final Instant startTime;

    /**
     * 结束时间
     */
    private Instant endTime;

    /**
     * 请求信息
     */
    private final RecordableHttpRequest request;

    /**
     * 响应信息
     */
    private final RecordableHttpResponse response;

    /**
     * 配置信息
     */
    private final LogProperties properties;

    private AccessLogContext(Builder builder) {
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.request = builder.request;
        this.response = builder.response;
        this.properties = builder.properties;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public RecordableHttpRequest getRequest() {
        return request;
    }

    public RecordableHttpResponse getResponse() {
        return response;
    }

    public LogProperties getProperties() {
        return properties;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 访问日志上下文构建者
     */
    public static class Builder {

        private Instant startTime;
        private Instant endTime;
        private RecordableHttpRequest request;
        private RecordableHttpResponse response;
        private LogProperties properties;

        private Builder() {
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder request(RecordableHttpRequest request) {
            this.request = request;
            return this;
        }

        public Builder response(RecordableHttpResponse response) {
            this.response = response;
            return this;
        }

        public Builder properties(LogProperties properties) {
            this.properties = properties;
            return this;
        }

        public AccessLogContext build() {
            return new AccessLogContext(this);
        }
    }
}
