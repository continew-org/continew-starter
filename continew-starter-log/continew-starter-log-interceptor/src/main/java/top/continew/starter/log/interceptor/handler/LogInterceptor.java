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

package top.continew.starter.log.interceptor.handler;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.continew.starter.log.core.http.recordable.impl.RecordableServletHttpRequest;
import top.continew.starter.log.core.http.recordable.impl.RecordableServletHttpResponse;
import top.continew.starter.log.interceptor.annotation.Log;
import top.continew.starter.log.core.dao.LogDao;
import top.continew.starter.log.core.enums.Include;
import top.continew.starter.log.core.model.LogRecord;
import top.continew.starter.log.interceptor.autoconfigure.LogProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 日志拦截器
 *
 * @author Charles7c
 * @since 1.1.0
 */
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LogInterceptor.class);
    private final LogDao logDao;
    private final LogProperties logProperties;
    private final TransmittableThreadLocal<Instant> timeTtl = new TransmittableThreadLocal<>();
    private final TransmittableThreadLocal<LogRecord.Started> logTtl = new TransmittableThreadLocal<>();

    public LogInterceptor(LogDao logDao, LogProperties logProperties) {
        this.logDao = logDao;
        this.logProperties = logProperties;
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
        if (this.isRequestRecord(handler, request)) {
            LogRecord.Started startedLogRecord = LogRecord.start(startTime, new RecordableServletHttpRequest(request));
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
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            Log methodLog = handlerMethod.getMethodAnnotation(Log.class);
            Log classLog = handlerMethod.getBeanType().getDeclaredAnnotation(Log.class);
            Set<Include> includeSet = this.getIncludes(methodLog, classLog);
            LogRecord finishedLogRecord = startedLogRecord
                .finish(endTime, new RecordableServletHttpResponse(response, response.getStatus()), includeSet);
            // 记录日志描述
            if (includeSet.contains(Include.DESCRIPTION)) {
                this.logDescription(finishedLogRecord, methodLog, handlerMethod);
            }
            // 记录所属模块
            if (includeSet.contains(Include.MODULE)) {
                this.logModule(finishedLogRecord, methodLog, classLog, handlerMethod);
            }
            logDao.add(finishedLogRecord);
        } catch (Exception ex) {
            log.error("Logging http log occurred an error: {}.", ex.getMessage(), ex);
        } finally {
            timeTtl.remove();
            logTtl.remove();
        }
    }

    /**
     * 获取日志包含信息
     *
     * @param methodLog 方法级 Log 注解
     * @param classLog  类级 Log 注解
     * @return 日志包含信息
     */
    private Set<Include> getIncludes(Log methodLog, Log classLog) {
        Set<Include> includeSet = new HashSet<>(logProperties.getIncludes());
        if (null != classLog) {
            this.processInclude(includeSet, classLog);
        }
        if (null != methodLog) {
            this.processInclude(includeSet, methodLog);
        }
        return includeSet;
    }

    /**
     * 处理日志包含信息
     *
     * @param includes      日志包含信息
     * @param logAnnotation Log 注解
     */
    private void processInclude(Set<Include> includes, Log logAnnotation) {
        Include[] includeArr = logAnnotation.includes();
        if (includeArr.length > 0) {
            includes.addAll(Set.of(includeArr));
        }
        Include[] excludeArr = logAnnotation.excludes();
        if (excludeArr.length > 0) {
            includes.removeAll(Set.of(excludeArr));
        }
    }

    /**
     * 记录描述
     *
     * @param logRecord     日志信息
     * @param methodLog     方法级 Log 注解
     * @param handlerMethod 处理器方法
     */
    private void logDescription(LogRecord logRecord, Log methodLog, HandlerMethod handlerMethod) {
        // 例如：@Log("新增部门") -> 新增部门
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.value())) {
            logRecord.setDescription(methodLog.value());
            return;
        }
        // 例如：@Operation(summary="新增部门") -> 新增部门
        Operation methodOperation = handlerMethod.getMethodAnnotation(Operation.class);
        if (null != methodOperation) {
            logRecord.setDescription(CharSequenceUtil.blankToDefault(methodOperation.summary(), "请在该接口方法上指定日志描述"));
        }
    }

    /**
     * 记录模块
     *
     * @param logRecord     日志信息
     * @param methodLog     方法级 Log 注解
     * @param classLog      类级 Log 注解
     * @param handlerMethod 处理器方法
     */
    private void logModule(LogRecord logRecord, Log methodLog, Log classLog, HandlerMethod handlerMethod) {
        // 例如：@Log(module = "部门管理") -> 部门管理
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.module())) {
            logRecord.setModule(methodLog.module());
            return;
        }
        if (null != classLog && CharSequenceUtil.isNotBlank(classLog.module())) {
            logRecord.setModule(classLog.module());
            return;
        }
        // 例如：@Tag(name = "部门管理") -> 部门管理
        Tag classTag = handlerMethod.getBeanType().getDeclaredAnnotation(Tag.class);
        if (null != classTag) {
            String name = classTag.name();
            logRecord.setModule(CharSequenceUtil.blankToDefault(name, "请在该接口类上指定所属模块"));
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
