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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.http.RecordableHttpResponse;

import java.time.Instant;

/**
 * {@link AccessLogContext} 访问日志上下文测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AccessLogContextTest {

    /**
     * 构建者应完整装配各字段且与原对象同源
     */
    @Test
    @DisplayName("构建者装配各字段且同源")
    void builderShouldAssembleAllFields() {
        Instant startTime = Instant.parse("2026-01-01T00:00:00Z");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        RecordableHttpResponse response = mock(RecordableHttpResponse.class);
        LogProperties properties = new LogProperties();

        AccessLogContext context = AccessLogContext.builder()
            .startTime(startTime)
            .endTime(startTime)
            .request(request)
            .response(response)
            .properties(properties)
            .build();

        assertThat(context.getStartTime()).isEqualTo(startTime);
        assertThat(context.getRequest()).isSameAs(request);
        assertThat(context.getResponse()).isSameAs(response);
        assertThat(context.getProperties()).isSameAs(properties);
    }

    /**
     * 未设置结束时间时结束时间保持为 null
     */
    @Test
    @DisplayName("未设置结束时间时保持 null")
    void endTimeShouldBeNullUntilSet() {
        AccessLogContext context = AccessLogContext.builder()
            .startTime(Instant.parse("2026-01-01T00:00:00Z"))
            .properties(new LogProperties())
            .build();
        assertThat(context.getEndTime()).isNull();
    }

    /**
     * 结束时间可在请求结束后回填
     */
    @Test
    @DisplayName("结束时间可回填")
    void endTimeShouldBeSettableAfterBuild() {
        AccessLogContext context = AccessLogContext.builder().build();
        Instant endTime = Instant.parse("2026-01-01T00:00:05Z");
        context.setEndTime(endTime);
        assertThat(context.getEndTime()).isEqualTo(endTime);
    }
}
