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

import com.alibaba.ttl.TransmittableThreadLocal;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.log.autoconfigure.LogProperties;
import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.handler.LogHandler;
import top.continew.starter.log.model.LogRecord;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

/**
 * 日志拦截器
 *
 * @author Charles7c
 * @since 1.1.0
 */
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LogInterceptor.class);
    private final LogProperties logProperties;
    private final LogHandler logHandler;
    private final LogDao logDao;
    private final TransmittableThreadLocal<Instant> timeTtl = new TransmittableThreadLocal<>();
    private final TransmittableThreadLocal<LogRecord.Started> logTtl = new TransmittableThreadLocal<>();

    public LogInterceptor(LogProperties logProperties, LogHandler logHandler, LogDao logDao) {
        this.logProperties = logProperties;
        this.logHandler = logHandler;
        this.logDao = logDao;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Instant startTime = Instant.now();
        if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
            log.info("[{}] {}", request.getMethod(), request.getRequestURI());
            timeTtl.set(startTime);
        }
        // 开始日志记录
        if (this.isRequestRecord(handler, request)) {
            LogRecord.Started startedLogRecord = logHandler.start(startTime, request);
            logTtl.set(startedLogRecord);
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception e) {
        try {
            Instant endTime = Instant.now();
            if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
                Duration timeTaken = Duration.between(timeTtl.get(), endTime);
                log.info("[{}] {} {} {}ms", request.getMethod(), request.getRequestURI(), response
                    .getStatus(), timeTaken.toMillis());
            }
            LogRecord.Started startedLogRecord = logTtl.get();
            if (null == startedLogRecord) {
                return;
            }
            // 结束日志记录
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            Method targetMethod = handlerMethod.getMethod();
            Class<?> targetClass = handlerMethod.getBeanType();
            LogRecord logRecord = logHandler.finish(startedLogRecord, endTime, response, logProperties
                .getIncludes(), targetMethod, targetClass);
            logDao.add(logRecord);
        } catch (Exception ex) {
            log.error("Logging http log occurred an error: {}.", ex.getMessage(), ex);
            throw ex;
        } finally {
            timeTtl.remove();
            logTtl.remove();
        }
    }

    /**
     * 是否要记录日志
     *
     * @param handler 处理器
     * @return true：需要记录；false：不需要记录
     */
    private boolean isRequestRecord(Object handler, HttpServletRequest request) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        // 如果接口匹配排除列表，不记录日志
        if (logProperties.isMatch(request.getRequestURI())) {
            return false;
        }
        // 如果接口被隐藏，不记录日志
        Operation methodOperation = handlerMethod.getMethodAnnotation(Operation.class);
        if (null != methodOperation && methodOperation.hidden()) {
            return false;
        }
        Hidden methodHidden = handlerMethod.getMethodAnnotation(Hidden.class);
        if (null != methodHidden) {
            return false;
        }
        Class<?> handlerBeanType = handlerMethod.getBeanType();
        if (null != handlerBeanType.getDeclaredAnnotation(Hidden.class)) {
            return false;
        }
        // 如果接口方法或类上有 @Log 注解，且要求忽略该接口，则不记录日志
        Log methodLog = handlerMethod.getMethodAnnotation(Log.class);
        if (null != methodLog && methodLog.ignore()) {
            return false;
        }
        Log classLog = handlerBeanType.getDeclaredAnnotation(Log.class);
        return null == classLog || !classLog.ignore();
    }
}
