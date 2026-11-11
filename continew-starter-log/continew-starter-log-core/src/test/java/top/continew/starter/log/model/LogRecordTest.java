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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.http.RecordableHttpResponse;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * {@link LogRecord} 及其请求/响应投影模型测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LogRecordTest {

    /**
     * Chrome 桌面端 User-Agent 样例
     */
    private static final String CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * start 与 finish 应记录起止时间戳并计算耗时
     */
    @Test
    @DisplayName("start/finish 记录时间戳与耗时")
    void startAndFinishShouldTrackTimestampAndDuration() {
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        RecordableHttpResponse response = mock(RecordableHttpResponse.class);
        when(response.getStatus()).thenReturn(200);

        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T00:00:05Z");
        LogRecord logRecord = LogRecord.start(start, request)
            .finish(end, response, Include.defaultIncludes());

        assertThat(logRecord.getTimestamp()).isEqualTo(start);
        assertThat(logRecord.getTimeTaken()).isEqualTo(Duration.ofSeconds(5));
        assertThat(logRecord.getResponse().getStatus()).isEqualTo(200);
    }

    /**
     * 默认包含信息下应记录请求头与请求参数
     */
    @Test
    @DisplayName("默认包含信息下记录请求头与请求参数")
    void requestShouldCarryHeadersAndParamsWithDefaultIncludes() {
        RecordableHttpRequest request = mockRequest();
        LogRequest logRequest = new LogRequest(request, Include.defaultIncludes());
        assertThat(logRequest.getMethod()).isEqualTo("GET");
        assertThat(logRequest.getUrl()).isEqualTo(URI.create("http://localhost/api/users"));
        assertThat(logRequest.getIp()).isEqualTo("127.0.0.1");
        assertThat(logRequest.getHeaders()).containsEntry("User-Agent", CHROME_UA);
        assertThat(logRequest.getParams()).isEqualTo("{\"id\":1}");
        assertThat(logRequest.getBody()).isNull();
    }

    /**
     * 空包含集下请求头、请求体、请求参数均不记录
     */
    @Test
    @DisplayName("空包含集下请求信息不记录")
    void requestShouldNotCarryAnythingWithEmptyIncludes() {
        LogRequest logRequest = new LogRequest(mockRequest(), Set.of());
        assertThat(logRequest.getHeaders()).isNull();
        assertThat(logRequest.getBody()).isNull();
        assertThat(logRequest.getParams()).isNull();
        assertThat(logRequest.getAddress()).isNull();
        assertThat(logRequest.getBrowser()).isNull();
        assertThat(logRequest.getOs()).isNull();
    }

    /**
     * 包含请求体时记录请求体且不记录请求参数
     */
    @Test
    @DisplayName("包含请求体时记录体且参数置空")
    void bodyShouldBeRecordedAndParamsSkippedWhenBodyIncluded() {
        LogRequest logRequest = new LogRequest(mockRequest(), Set.of(Include.REQUEST_BODY));
        assertThat(logRequest.getBody()).isEqualTo("{\"id\":1}");
        assertThat(logRequest.getParams()).isNull();
    }

    /**
     * 内网 IP 且包含归属地时解析为内网标记
     */
    @Test
    @DisplayName("内网 IP 解析为内网标记")
    void addressShouldBeIntranetMarkForInnerIp() {
        LogRequest logRequest = new LogRequest(mockRequest(), Set.of(Include.IP_ADDRESS));
        assertThat(logRequest.getAddress()).isEqualTo("内网IP");
    }

    /**
     * 外网 IP 无归属地解析器（ip2region Bean 缺失）时归属地为 null 而非抛异常
     */
    @Test
    @DisplayName("外网 IP 无解析器时归属地为 null")
    void addressShouldBeNullWhenPublicIpCannotBeResolved() {
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getIp()).thenReturn("8.8.8.8");
        // SpringUtil 持有 JVM 级静态上下文，显式固定 Bean 缺失路径，避免依赖测试执行顺序
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(Ip2regionSearcher.class))
                .thenThrow(new NoSuchBeanDefinitionException(Ip2regionSearcher.class));
            LogRequest logRequest = new LogRequest(request, Set.of(Include.IP_ADDRESS));
            assertThat(logRequest.getAddress()).isNull();
            // 确认异常确实产生于解析器查找，而非地址解析被整体跳过
            springUtil.verify(() -> SpringUtil.getBean(Ip2regionSearcher.class));
        }
    }

    /**
     * 包含浏览器与操作系统时从 User-Agent 解析
     */
    @Test
    @DisplayName("浏览器与操作系统从 User-Agent 解析")
    void browserAndOsShouldBeParsedFromUserAgent() {
        LogRequest logRequest = new LogRequest(mockRequest(), Set
            .of(Include.REQUEST_HEADERS, Include.BROWSER, Include.OS));
        assertThat(logRequest.getBrowser()).contains("Chrome");
        assertThat(logRequest.getOs()).isNotBlank();
    }

    /**
     * 锁定当前行为：浏览器解析依赖请求头包含项，未包含请求头时即使包含浏览器项也为 null
     */
    @Test
    @DisplayName("未含请求头时浏览器解析跳过（锁定当前行为）")
    void browserShouldBeNullWhenHeadersNotIncluded() {
        LogRequest logRequest = new LogRequest(mockRequest(), Set.of(Include.BROWSER));
        assertThat(logRequest.getBrowser()).isNull();
    }

    /**
     * 响应体包含时记录响应体且不记录响应参数
     */
    @Test
    @DisplayName("包含响应体时记录体且参数置空")
    void responseBodyShouldBeRecordedAndParamsSkippedWhenBodyIncluded() {
        LogResponse logResponse = new LogResponse(mockResponse(), Set.of(Include.RESPONSE_BODY));
        assertThat(logResponse.getStatus()).isEqualTo(200);
        assertThat(logResponse.getBody()).isEqualTo("{\"ok\":true}");
        assertThat(logResponse.getParams()).isNull();
    }

    /**
     * 状态码始终记录，响应头/体/参数按包含项决定
     */
    @Test
    @DisplayName("空包含集下响应仅记录状态码")
    void responseShouldCarryStatusOnlyWithEmptyIncludes() {
        LogResponse logResponse = new LogResponse(mockResponse(), Set.of());
        assertThat(logResponse.getStatus()).isEqualTo(200);
        assertThat(logResponse.getHeaders()).isNull();
        assertThat(logResponse.getBody()).isNull();
        assertThat(logResponse.getParams()).isNull();
    }

    /**
     * 日志的描述、模块与错误信息可读写
     */
    @Test
    @DisplayName("描述/模块/错误信息可读写")
    void descriptionModuleAndErrorMsgShouldBeWritable() {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        logRecord.setDescription("新增用户");
        logRecord.setModule("用户管理");
        logRecord.setErrorMsg("非法参数");
        assertThat(logRecord.getDescription()).isEqualTo("新增用户");
        assertThat(logRecord.getModule()).isEqualTo("用户管理");
        assertThat(logRecord.getErrorMsg()).isEqualTo("非法参数");
    }

    private RecordableHttpRequest mockRequest() {
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getUrl()).thenReturn(URI.create("http://localhost/api/users"));
        when(request.getIp()).thenReturn("127.0.0.1");
        when(request.getHeaders()).thenReturn(Map.of("User-Agent", CHROME_UA));
        when(request.getBody()).thenReturn("{\"id\":1}");
        when(request.getParams()).thenReturn("{\"id\":1}");
        return request;
    }

    private RecordableHttpResponse mockResponse() {
        RecordableHttpResponse response = mock(RecordableHttpResponse.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getHeaders()).thenReturn(Map.of("Content-Type", "application/json"));
        when(response.getBody()).thenReturn("{\"ok\":true}");
        when(response.getParams()).thenReturn("{\"ok\":true}");
        return response;
    }
}
