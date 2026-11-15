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

package top.continew.starter.log.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.handler.AopLogHandler;
import top.continew.starter.log.handler.LogHandler;
import top.continew.starter.log.http.servlet.RecordableServletHttpRequest;
import top.continew.starter.log.model.LogProperties;
import top.continew.starter.log.model.LogRecord;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;

/**
 * {@link LogAspect} 日志切面行为单元测试：非 Web 环境直通、Web 环境记录、异常截断与记录失败隔离。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LogAspectTest {

    private LogHandler logHandler;
    private LogDao logDao;
    private LogAspect aspect;

    @BeforeEach
    void setUp() {
        logHandler = mock(LogHandler.class);
        logDao = mock(LogDao.class);
        aspect = new LogAspect(new LogProperties(), logHandler, logDao);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 非 Web 环境（无 RequestAttributes）应直通目标方法且不触发任何日志记录。
     */
    @Test
    @DisplayName("非 Web 环境直通且不记录")
    void shouldProceedWithoutLoggingInNonWebEnvironment() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(new WebTarget(), "target");

        Object result = aspect.around(joinPoint);

        assertThat(result).isEqualTo("target");
        verify(logHandler, never()).start(any(), any());
        verify(logDao, never()).add(any());
    }

    /**
     * Web 环境下应记录日志：start/finish/add 按序触发并落库。
     */
    @Test
    @DisplayName("Web 环境记录日志并落库")
    void shouldLogAndPersistInWebEnvironment() throws Throwable {
        bindWebRequest();
        stubRecordableHandler();
        ProceedingJoinPoint joinPoint = mockJoinPoint(new WebTarget(), "target");

        Object result = aspect.around(joinPoint);

        assertThat(result).isEqualTo("target");
        verify(logHandler).start(any(), any());
        ArgumentCaptor<LogRecord> captor = ArgumentCaptor.forClass(LogRecord.class);
        verify(logDao).add(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    /**
     * 目标方法异常时应原样传播、记录错误信息（截断到 2000 字符内）并仍落库。
     */
    @Test
    @DisplayName("业务异常传播且记录错误信息")
    void shouldPropagateExceptionAndRecordErrorMsg() throws Throwable {
        bindWebRequest();
        stubRecordableHandler();
        String longMessage = "x".repeat(2500);
        ProceedingJoinPoint joinPoint = mockJoinPoint(new ThrowingTarget(), "boom", longMessage);

        assertThatThrownBy(() -> aspect.around(joinPoint)).isInstanceOf(IllegalStateException.class)
            .hasMessage(longMessage);

        ArgumentCaptor<LogRecord> captor = ArgumentCaptor.forClass(LogRecord.class);
        verify(logDao).add(captor.capture());
        assertThat(captor.getValue().getErrorMsg()).hasSize(2000);
    }

    /**
     * 日志记录链路自身失败不应影响业务返回（finally 内吞掉记录异常）。
     */
    @Test
    @DisplayName("记录失败不影响业务返回")
    void shouldSwallowLoggingFailureAndReturnBusinessResult() throws Throwable {
        bindWebRequest();
        when(logHandler.isRecord(any(), any())).thenReturn(true);
        MockHttpServletRequest request =
            (MockHttpServletRequest) ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();
        RecordableServletHttpRequest recordableRequest = new RecordableServletHttpRequest(request);
        when(logHandler.start(any(), any())).thenReturn(LogRecord.start(Instant.now(),
            recordableRequest));
        when(logHandler.finish(any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException(
                "记录失败"));
        ProceedingJoinPoint joinPoint = mockJoinPoint(new WebTarget(), "target");

        Object result = aspect.around(joinPoint);

        assertThat(result).isEqualTo("target");
    }

    /**
     * 排除 URI 命中时不记录（isRecord 返回 false，直通）。
     */
    @Test
    @DisplayName("排除 URI 直通")
    void shouldSkipWhenUriMatchesExcludePattern() throws Throwable {
        LogProperties properties = new LogProperties();
        properties.setExcludePatterns(java.util.List.of("/exclude/**"));
        aspect = new LogAspect(properties, logHandler, logDao);
        bindWebRequest("/exclude/api");
        ProceedingJoinPoint joinPoint = mockJoinPoint(new WebTarget(), "target");

        Object result = aspect.around(joinPoint);

        assertThat(result).isEqualTo("target");
        verify(logHandler, never()).isRecord(any(), any());
        verify(logDao, never()).add(any());
    }

    /**
     * 构造带真实 Method 签名的 ProceedingJoinPoint mock。
     *
     * @param target  目标对象
     * @param result  proceed 返回值
     * @param message 目标方法抛出的异常消息（null 表示正常返回）
     * @return 切点 mock
     */
    private ProceedingJoinPoint mockJoinPoint(Object target, String result, String... message)
        throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Method method = resolveTargetMethod(target);
        when(joinPoint.getSignature()).thenReturn(new MockMethodSignature(method));
        when(joinPoint.getTarget()).thenReturn(target);
        if (message.length > 0) {
            when(joinPoint.proceed()).thenThrow(new IllegalStateException(message[0]));
        } else {
            when(joinPoint.proceed()).thenReturn(result);
        }
        return joinPoint;
    }

    private Method resolveTargetMethod(Object target) throws NoSuchMethodException {
        return target.getClass().getMethod("target");
    }

    private void bindWebRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri != null ? uri : "/api/target");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    private void bindWebRequest() {
        bindWebRequest(null);
    }

    /**
     * stub 可记录链路：isRecord 放行，start 返回带真实请求的 Started，finish 委托真实
     * AopLogHandler（mock 未 stub 的 finish 返回 null，会使切面 setErrorMsg 处 NPE）。
     */
    private void stubRecordableHandler() {
        when(logHandler.isRecord(any(), any())).thenReturn(true);
        MockHttpServletRequest request =
            (MockHttpServletRequest) ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();
        RecordableServletHttpRequest recordableRequest = new RecordableServletHttpRequest(request);
        when(logHandler.start(any(), any())).thenReturn(LogRecord.start(Instant.now(),
            recordableRequest));
        AopLogHandler realHandler = new AopLogHandler();
        when(logHandler.finish(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            LogRecord.Started started = invocation.getArgument(0);
            Instant endTime = invocation.getArgument(1);
            HttpServletResponse response = invocation.getArgument(2);
            Set<Include> includes = invocation.getArgument(3);
            Method targetMethod = invocation.getArgument(4);
            Class<?> targetClass = invocation.getArgument(5);
            return realHandler.finish(started, endTime, response, includes, targetMethod,
                targetClass);
        });
    }

    /**
     * Mock MethodSignature（仅 getMethod 有真实语义）
     */
    static class MockMethodSignature implements org.aspectj.lang.reflect.MethodSignature {

        private final Method method;

        MockMethodSignature(Method method) {
            this.method = method;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Class[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            return new String[0];
        }

        @Override
        public Class[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public Class getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public String toString() {
            return method.toString();
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toString();
        }
    }

    /**
     * 正常返回目标
     */
    static class WebTarget {

        @Log("测试接口")
        public String target() {
            return "target";
        }
    }

    /**
     * 抛异常目标
     */
    static class ThrowingTarget {

        @Log("异常接口")
        public String target() {
            return "never";
        }
    }
}
