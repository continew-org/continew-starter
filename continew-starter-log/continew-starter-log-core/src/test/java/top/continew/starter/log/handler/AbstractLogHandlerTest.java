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

package top.continew.starter.log.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.http.RecordableHttpResponse;
import top.continew.starter.log.model.AccessLogContext;
import top.continew.starter.log.model.AccessLogProperties;
import top.continew.starter.log.model.LogProperties;
import top.continew.starter.log.model.LogRecord;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link AbstractLogHandler} 日志处理器基类测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AbstractLogHandlerTest {

    private final AbstractLogHandler handler = new TestLogHandler();

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUpLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractLogHandler.class);
        logAppender.setContext(logger.getLoggerContext());
        logAppender.start();
        logAppender.list.clear();
        logger.addAppender(logAppender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDownLogAppender() {
        // 结束可能残留的访问日志上下文，避免 ThreadLocal 状态在用例间串扰；无残留时为空操作
        handler.accessLogFinish(AccessLogContext.builder()
            .endTime(Instant.now())
            .response(mock(RecordableHttpResponse.class))
            .properties(new LogProperties())
            .build());
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractLogHandler.class);
        logger.detachAppender(logAppender);
    }

    /**
     * 是否记录日志应按隐藏与忽略注解逐级判定
     *
     * @param targetClass 目标类
     * @param methodName  目标方法名
     * @param expected    预期是否记录
     */
    @ParameterizedTest(name = "[{index}] {0}.{1} 是否记录应为 {2}")
    @MethodSource("isRecordCases")
    @DisplayName("是否记录日志按隐藏与忽略注解判定")
    void isRecordShouldFollowHiddenAndIgnoreAnnotations(Class<?> targetClass, String methodName,
        boolean expected) throws Exception {
        Method method = targetClass.getDeclaredMethod(methodName);
        assertThat(handler.isRecord(method, targetClass)).isEqualTo(expected);
    }

    static Stream<Arguments> isRecordCases() {
        return Stream.of(Arguments.of(PlainController.class, "list", true), Arguments
            .of(VisibleOperationController.class, "remove", true),
            Arguments
                .of(HiddenOperationController.class, "remove", false),
            Arguments
                .of(HiddenMethodController.class, "export", false),
            Arguments
                .of(HiddenClassController.class, "list", false),
            Arguments
                .of(IgnoredMethodController.class, "list", false),
            Arguments
                .of(IgnoredClassController.class, "list", false));
    }

    /**
     * start 与 finish 应基于 Servlet 请求/响应记录时间戳、耗时与方法
     */
    @Test
    @DisplayName("start/finish 基于 Servlet 对象记录耗时")
    void startAndFinishShouldTrackDurationFromServletObjects() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T00:00:05Z");
        LogRecord.Started started = handler.start(start, new MockHttpServletRequest("GET",
            "/api/users"));
        LogRecord logRecord = handler.finish(started, end, new MockHttpServletResponse(), Set
            .of(Include.REQUEST_HEADERS));
        assertThat(logRecord.getTimestamp()).isEqualTo(start);
        assertThat(logRecord.getTimeTaken()).isEqualTo(Duration.ofSeconds(5));
        assertThat(logRecord.getRequest().getMethod()).isEqualTo("GET");
        assertThat(logRecord.getResponse().getStatus()).isEqualTo(200);
    }

    /**
     * 结束记录时应按包含项写入方法级注解中的描述与模块
     */
    @Test
    @DisplayName("结束记录写入方法级注解的描述与模块")
    void finishShouldFillDescriptionAndModuleFromMethodAnnotation() throws Exception {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        LogRecord.Started started = handler.start(start, new MockHttpServletRequest("GET",
            "/api/users"));
        LogRecord logRecord = handler.finish(started, start.plusSeconds(1),
            new MockHttpServletResponse(), Set
                .of(Include.DESCRIPTION, Include.MODULE),
            FullController.class
                .getDeclaredMethod("create"),
            FullController.class);
        assertThat(logRecord.getDescription()).isEqualTo("新增部门");
        assertThat(logRecord.getModule()).isEqualTo("方法级模块");
    }

    /**
     * 未包含描述与模块项时结束记录不应回填对应字段
     */
    @Test
    @DisplayName("未包含描述与模块项时不回填")
    void finishShouldSkipDescriptionAndModuleWhenNotIncluded() throws Exception {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        LogRecord.Started started = handler.start(start, new MockHttpServletRequest("GET",
            "/api/users"));
        LogRecord logRecord = handler.finish(started, start.plusSeconds(1),
            new MockHttpServletResponse(), Set
                .of(Include.REQUEST_HEADERS),
            FullController.class
                .getDeclaredMethod("create"),
            FullController.class);
        assertThat(logRecord.getDescription()).isNull();
        assertThat(logRecord.getModule()).isNull();
    }

    /**
     * 描述取值优先级：方法 @Log(value) 高于 @Operation(summary)
     */
    @Test
    @DisplayName("描述优先取方法 @Log(value)")
    void logDescriptionShouldPreferLogValueOverOperationSummary() throws Exception {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        handler.logDescription(logRecord, DescribedByLogController.class
            .getDeclaredMethod("create"));
        assertThat(logRecord.getDescription()).isEqualTo("新增部门");
    }

    /**
     * 方法 @Log(value) 为空白时描述回退取 @Operation(summary)
     */
    @Test
    @DisplayName("@Log(value) 空白时回退 @Operation(summary)")
    void logDescriptionShouldFallbackToOperationSummary() throws Exception {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        handler.logDescription(logRecord, DescribedByOperationController.class
            .getDeclaredMethod("create"));
        assertThat(logRecord.getDescription()).isEqualTo("空描述回退");
    }

    /**
     * 无任何描述注解时描述为引导文案
     */
    @Test
    @DisplayName("无描述注解时为引导文案")
    void logDescriptionShouldUseHintWhenNoAnnotationPresent() throws Exception {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        handler.logDescription(logRecord, UndescribedController.class.getDeclaredMethod("create"));
        assertThat(logRecord.getDescription())
            .isEqualTo("请在该接口方法上添加 @top.continew.starter.log.annotation.Log(value) 来指定日志描述");
    }

    /**
     * 模块取值优先级：方法级注解高于类级注解
     */
    @Test
    @DisplayName("模块优先取方法级注解")
    void logModuleShouldPreferMethodAnnotationOverClassAnnotation() throws Exception {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        handler.logModule(logRecord, ModuleFromMethodController.class
            .getDeclaredMethod("create"), ModuleFromMethodController.class);
        assertThat(logRecord.getModule()).isEqualTo("方法级模块");
    }

    /**
     * 方法无模块注解时回退取类级 @Log(module)
     */
    @Test
    @DisplayName("方法无注解时回退类级 @Log(module)")
    void logModuleShouldFallbackToClassAnnotation() throws Exception {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        handler.logModule(logRecord, ModuleFromClassController.class
            .getDeclaredMethod("create"), ModuleFromClassController.class);
        assertThat(logRecord.getModule()).isEqualTo("类级模块");
    }

    /**
     * 类级无 @Log(module) 时回退取 @Tag(name)
     */
    @Test
    @DisplayName("类级无 @Log(module) 时回退 @Tag(name)")
    void logModuleShouldFallbackToClassTag() throws Exception {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        handler.logModule(logRecord, ModuleFromTagController.class
            .getDeclaredMethod("create"), ModuleFromTagController.class);
        assertThat(logRecord.getModule()).isEqualTo("标签模块");
    }

    /**
     * 包含信息按类级注解先合并、方法级注解后合并
     */
    @Test
    @DisplayName("包含信息按类级再方法级合并")
    void getIncludesShouldMergeClassThenMethodAnnotation() throws Exception {
        Set<Include> includes = handler.getIncludes(Include.defaultIncludes(),
            IncludeClassController.class.getDeclaredMethod("create"), IncludeClassController.class);
        assertThat(includes).containsExactlyInAnyOrder(Include.REQUEST_PARAM,
            Include.RESPONSE_PARAM, Include.REQUEST_BODY, Include.IP_ADDRESS);
    }

    /**
     * 方法无注解时仅应用类级包含信息
     */
    @Test
    @DisplayName("方法无注解时仅应用类级包含信息")
    void getIncludesShouldApplyClassAnnotationOnlyWithoutMethodAnnotation() throws Exception {
        Set<Include> includes = handler.getIncludes(Include.defaultIncludes(),
            IncludeClassController.class.getDeclaredMethod("list"), IncludeClassController.class);
        assertThat(includes).containsExactlyInAnyOrder(Include.RESPONSE_HEADERS,
            Include.REQUEST_PARAM, Include.RESPONSE_PARAM, Include.REQUEST_BODY);
    }

    /**
     * 无任何注解时包含信息与默认集一致
     */
    @Test
    @DisplayName("无注解时包含信息与默认集一致")
    void getIncludesShouldKeepBaseWithoutAnnotations() throws Exception {
        Set<Include> includes = handler.getIncludes(Include.defaultIncludes(), PlainController.class
            .getDeclaredMethod("list"), PlainController.class);
        assertThat(includes).containsExactlyInAnyOrderElementsOf(Include.defaultIncludes());
    }

    /**
     * 访问日志开启时应输出开始事件
     */
    @Test
    @DisplayName("访问日志开启时输出开始事件")
    void accessLogStartShouldLogStartEventWhenEnabled() {
        LogProperties properties = new LogProperties();
        properties.getAccessLog().setEnabled(true);
        handler.accessLogStart(startContext("GET", "/api/users", properties));
        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getFormattedMessage())
            .isEqualTo("[Start] [GET] /api/users");
    }

    /**
     * 开启参数打印时开始事件应携带参数
     */
    @Test
    @DisplayName("开启参数打印时开始事件携带参数")
    void accessLogStartShouldLogParamsWhenPrintRequestParamEnabled() {
        LogProperties properties = new LogProperties();
        AccessLogProperties accessLog = properties.getAccessLog();
        accessLog.setEnabled(true);
        accessLog.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getPath()).thenReturn("/api/users");
        when(request.getParams()).thenReturn("{\"id\":1}");
        handler.accessLogStart(AccessLogContext.builder()
            .startTime(Instant.parse("2026-01-01T00:00:00Z"))
            .request(request)
            .properties(properties)
            .build());
        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getFormattedMessage())
            .isEqualTo("[Start] [GET] /api/users param: {\"id\":1}");
    }

    /**
     * 访问日志关闭时不输出事件且不残留上下文
     */
    @Test
    @DisplayName("访问日志关闭时不输出事件")
    void accessLogStartShouldLogNothingWhenDisabled() {
        handler.accessLogStart(startContext("GET", "/api/users", new LogProperties()));
        assertThat(logAppender.list).isEmpty();
    }

    /**
     * 命中放行路径（内置错误页）时不输出事件
     */
    @Test
    @DisplayName("命中放行路径时不输出事件")
    void accessLogStartShouldLogNothingForExcludedPath() {
        LogProperties properties = new LogProperties();
        properties.getAccessLog().setEnabled(true);
        handler.accessLogStart(startContext("GET", "/error", properties));
        assertThat(logAppender.list).isEmpty();
    }

    /**
     * 结束访问日志时输出结束事件并清理线程上下文
     */
    @Test
    @DisplayName("结束访问日志输出结束事件并清理上下文")
    void accessLogFinishShouldLogEndEventAndClearContext() {
        LogProperties properties = new LogProperties();
        properties.getAccessLog().setEnabled(true);
        handler.accessLogStart(startContext("GET", "/api/users", properties));

        RecordableHttpResponse response = mock(RecordableHttpResponse.class);
        when(response.getStatus()).thenReturn(200);
        AccessLogContext finishContext = AccessLogContext.builder()
            .endTime(Instant.parse("2026-01-01T00:00:05Z"))
            .response(response)
            .properties(properties)
            .build();
        handler.accessLogFinish(finishContext);

        assertThat(logAppender.list).hasSize(2);
        assertThat(logAppender.list.get(1).getFormattedMessage())
            .isEqualTo("[End] [GET] /api/users 200 5000ms");

        // 线程上下文已清理，重复结束不产生新事件
        handler.accessLogFinish(finishContext);
        assertThat(logAppender.list).hasSize(2);
    }

    /**
     * 未先开始记录时结束访问日志应直接跳过
     */
    @Test
    @DisplayName("未先开始记录时结束访问日志跳过")
    void accessLogFinishShouldSkipWithoutStartedContext() {
        LogProperties properties = new LogProperties();
        properties.getAccessLog().setEnabled(true);
        RecordableHttpResponse response = mock(RecordableHttpResponse.class);
        AccessLogContext finishContext = AccessLogContext.builder()
            .endTime(Instant.parse("2026-01-01T00:00:05Z"))
            .response(response)
            .properties(properties)
            .build();
        handler.accessLogFinish(finishContext);
        assertThat(logAppender.list).isEmpty();
    }

    private AccessLogContext startContext(String method, String path, LogProperties properties) {
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getPath()).thenReturn(path);
        return AccessLogContext.builder()
            .startTime(Instant.parse("2026-01-01T00:00:00Z"))
            .request(request)
            .properties(properties)
            .build();
    }

    /**
     * 供测试实例化的空实现
     */
    static class TestLogHandler extends AbstractLogHandler {
    }

    static class PlainController {

        public String list() {
            return "ok";
        }
    }

    static class VisibleOperationController {

        @Operation(summary = "可见接口", hidden = false)
        public String remove() {
            return "ok";
        }
    }

    static class HiddenOperationController {

        @Operation(summary = "隐藏接口", hidden = true)
        public String remove() {
            return "ok";
        }
    }

    static class HiddenMethodController {

        @Hidden
        public String export() {
            return "ok";
        }
    }

    @Hidden
    static class HiddenClassController {

        public String list() {
            return "ok";
        }
    }

    static class IgnoredMethodController {

        @Log(ignore = true)
        public String list() {
            return "ok";
        }
    }

    @Log(ignore = true)
    static class IgnoredClassController {

        public String list() {
            return "ok";
        }
    }

    static class DescribedByLogController {

        @Log("新增部门")
        @Operation(summary = "注解摘要")
        public String create() {
            return "ok";
        }
    }

    static class DescribedByOperationController {

        @Log
        @Operation(summary = "空描述回退")
        public String create() {
            return "ok";
        }
    }

    static class UndescribedController {

        public String create() {
            return "ok";
        }
    }

    @Tag(name = "标签模块")
    static class ModuleFromTagController {

        public String create() {
            return "ok";
        }
    }

    @Log(module = "类级模块")
    static class ModuleFromClassController {

        public String create() {
            return "ok";
        }
    }

    @Log(module = "类级模块")
    static class ModuleFromMethodController {

        @Log(module = "方法级模块")
        public String create() {
            return "ok";
        }
    }

    @Log(includes = {Include.REQUEST_BODY}, excludes = {Include.REQUEST_HEADERS})
    static class IncludeClassController {

        @Log(includes = {Include.IP_ADDRESS}, excludes = {Include.RESPONSE_HEADERS})
        public String create() {
            return "ok";
        }

        public String list() {
            return "ok";
        }
    }

    static class FullController {

        @Log(value = "新增部门", module = "方法级模块")
        public String create() {
            return "ok";
        }
    }
}
