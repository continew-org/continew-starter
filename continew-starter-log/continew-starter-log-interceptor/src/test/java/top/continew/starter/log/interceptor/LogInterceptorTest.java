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

package top.continew.starter.log.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.handler.LogHandler;
import top.continew.starter.log.model.LogProperties;
import top.continew.starter.log.model.LogRecord;

import java.time.Instant;

/**
 * {@link LogInterceptor} 日志拦截器行为单元测试：非 HandlerMethod 直通、记录落库与记录失败隔离。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LogInterceptorTest {

    private LogHandler logHandler;
    private LogDao logDao;
    private LogInterceptor interceptor;

    @BeforeEach
    void setUp() {
        logHandler = mock(LogHandler.class);
        logDao = mock(LogDao.class);
        interceptor = new LogInterceptor(new LogProperties(), logHandler, logDao);
    }

    /**
     * preHandle 应始终放行，并触发访问日志 start（无论是否记录业务日志）。
     */
    @Test
    @DisplayName("preHandle 放行并触发访问日志")
    void shouldProceedAndStartAccessLog() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethod();

        boolean proceed = interceptor.preHandle(request, response, handler);

        assertThat(proceed).isTrue();
        verify(logHandler).accessLogStart(any());
    }

    /**
     * HandlerMethod 且 isRecord 放行时应 start 业务日志并在 afterCompletion 落库。
     */
    @Test
    @DisplayName("记录业务日志并落库")
    void shouldRecordAndPersistLogForHandlerMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethod();
        when(logHandler.isRecord(any(), any())).thenReturn(true);
        when(logHandler.start(any(), any())).thenReturn(LogRecord.start(Instant.now(), null));

        interceptor.preHandle(request, response, handler);
        interceptor.afterCompletion(request, response, handler, null);

        verify(logHandler).start(any(), any());
        verify(logHandler).accessLogFinish(any());
        verify(logDao).add(any());
    }

    /**
     * 非 HandlerMethod（如静态资源处理器）不应记录业务日志，afterCompletion 直接返回。
     */
    @Test
    @DisplayName("非 HandlerMethod 不记录业务日志")
    void shouldSkipRecordingForNonHandlerMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Object handler = new Object();

        interceptor.preHandle(request, response, handler);
        interceptor.afterCompletion(request, response, handler, null);

        verify(logHandler, never()).start(any(), any());
        verify(logDao, never()).add(any());
    }

    /**
     * afterCompletion 在未经过 preHandle（或未记录）时应直接返回且不落库。
     */
    @Test
    @DisplayName("afterCompletion 无记录时不落库")
    void shouldSkipPersistWhenNoStartedRecord() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethod();

        interceptor.afterCompletion(request, response, handler, null);

        verify(logDao, never()).add(any());
    }

    /**
     * afterCompletion 内日志记录失败应被吞掉，不向容器传播（保证请求正常完成）。
     */
    @Test
    @DisplayName("记录失败不影响请求完成")
    void shouldSwallowLoggingFailureInAfterCompletion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethod();
        when(logHandler.isRecord(any(), any())).thenReturn(true);
        when(logHandler.start(any(), any())).thenReturn(LogRecord.start(Instant.now(), null));
        when(logHandler.finish(any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException(
                "记录失败"));

        interceptor.preHandle(request, response, handler);

        org.assertj.core.api.Assertions.assertThatCode(() -> interceptor.afterCompletion(request,
            response, handler, null)).doesNotThrowAnyException();
    }

    private HandlerMethod handlerMethod() throws IllegalStateException {
        try {
            return new HandlerMethod(new TargetController(), TargetController.class
                .getMethod("list"));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 目标控制器（携带 @Log 注解的方法）
     */
    static class TargetController {

        @top.continew.starter.log.annotation.Log("列表查询")
        public String list() {
            return "list";
        }
    }
}
