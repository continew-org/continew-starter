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

package top.charles7c.continew.starter.log.httptracepro.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.charles7c.continew.starter.log.common.annotation.Log;
import top.charles7c.continew.starter.log.common.dao.LogDao;
import top.charles7c.continew.starter.log.common.enums.Include;
import top.charles7c.continew.starter.log.common.model.LogRecord;
import top.charles7c.continew.starter.log.common.model.LogResponse;
import top.charles7c.continew.starter.log.httptracepro.autoconfigure.LogProperties;

import java.time.Clock;
import java.util.Set;

/**
 * 日志拦截器
 *
 * @author Charles7c
 * @since 1.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class LogInterceptor implements HandlerInterceptor {

    private final LogDao logDao;
    private final LogProperties logProperties;
    private final TransmittableThreadLocal<LogRecord.Started> timestampTtl = new TransmittableThreadLocal<>();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Clock timestamp = Clock.systemUTC();
        if (this.isRequestRecord(handler, request)) {
            if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
                log.info("[{}] {}", request.getMethod(), request.getRequestURI());
            }
            LogRecord.Started startedLogRecord = LogRecord.start(timestamp, new RecordableServletHttpRequest(request));
            timestampTtl.set(startedLogRecord);
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception e) {
        LogRecord.Started startedLogRecord = timestampTtl.get();
        if (null == startedLogRecord) {
            return;
        }
        timestampTtl.remove();
        Set<Include> includeSet = logProperties.getInclude();
        try {
            LogRecord finishedLogRecord = startedLogRecord.finish(new RecordableServletHttpResponse(response, response
                .getStatus()), includeSet);
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            // 记录日志描述
            if (includeSet.contains(Include.DESCRIPTION)) {
                this.logDescription(finishedLogRecord, handlerMethod);
            }
            // 记录所属模块
            if (includeSet.contains(Include.MODULE)) {
                this.logModule(finishedLogRecord, handlerMethod);
            }
            if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
                LogResponse logResponse = finishedLogRecord.getResponse();
                log.info("[{}] {} {} {}ms", request.getMethod(), request.getRequestURI(), logResponse
                    .getStatus(), finishedLogRecord.getTimeTaken().toMillis());
            }
            logDao.add(finishedLogRecord);
        } catch (Exception ex) {
            log.error("Logging http log occurred an error: {}.", ex.getMessage(), ex);
        }
    }

    /**
     * 记录描述
     *
     * @param logRecord     日志信息
     * @param handlerMethod 处理器方法
     */
    private void logDescription(LogRecord logRecord, HandlerMethod handlerMethod) {
        // 例如：@Log("新增部门") -> 新增部门
        Log methodLog = handlerMethod.getMethodAnnotation(Log.class);
        if (null != methodLog && StrUtil.isNotBlank(methodLog.value())) {
            logRecord.setDescription(methodLog.value());
            return;
        }
        // 例如：@Operation(summary="新增部门") -> 新增部门
        Operation methodOperation = handlerMethod.getMethodAnnotation(Operation.class);
        if (null != methodOperation) {
            logRecord.setDescription(StrUtil.blankToDefault(methodOperation.summary(), "请在该接口方法上指定日志描述"));
        }
    }

    /**
     * 记录模块
     *
     * @param logRecord     日志信息
     * @param handlerMethod 处理器方法
     */
    private void logModule(LogRecord logRecord, HandlerMethod handlerMethod) {
        // 例如：@Log(module = "部门管理") -> 部门管理
        Log methodLog = handlerMethod.getMethodAnnotation(Log.class);
        if (null != methodLog && StrUtil.isNotBlank(methodLog.module())) {
            logRecord.setModule(methodLog.module());
            return;
        }
        Log classLog = handlerMethod.getBeanType().getDeclaredAnnotation(Log.class);
        if (null != classLog && StrUtil.isNotBlank(classLog.module())) {
            logRecord.setModule(classLog.module());
            return;
        }
        // 例如：@Tag(name = "部门管理") -> 部门管理
        Tag classTag = handlerMethod.getBeanType().getDeclaredAnnotation(Tag.class);
        if (null != classTag) {
            String name = classTag.name();
            logRecord.setModule(StrUtil.blankToDefault(name, "请在该接口类上指定所属模块"));
        }
    }

    /**
     * 是否要记录日志
     *
     * @param handler 处理器
     * @param request 请求对象
     * @return true：需要记录；false：不需要记录
     */
    private boolean isRequestRecord(Object handler, HttpServletRequest request) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        // 不拦截 /error
        ServerProperties serverProperties = SpringUtil.getBean(ServerProperties.class);
        if (request.getRequestURI().equals(serverProperties.getError().getPath())) {
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
