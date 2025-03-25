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

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.http.RecordableHttpResponse;
import top.continew.starter.log.http.servlet.RecordableServletHttpRequest;
import top.continew.starter.log.http.servlet.RecordableServletHttpResponse;
import top.continew.starter.log.model.AccessLogContext;
import top.continew.starter.log.model.AccessLogProperties;
import top.continew.starter.log.model.LogRecord;
import top.continew.starter.log.util.AccessLogUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 日志处理器基类
 *
 * @author Charles7c
 * @since 2.8.0
 */
public abstract class AbstractLogHandler implements LogHandler {

    private static final Logger log = LoggerFactory.getLogger(AbstractLogHandler.class);
    private final TransmittableThreadLocal<AccessLogContext> logContextThread = new TransmittableThreadLocal<>();

    @Override
    public LogRecord.Started start(Instant startTime, HttpServletRequest request) {
        return LogRecord.start(startTime, new RecordableServletHttpRequest(request));
    }

    @Override
    public LogRecord finish(LogRecord.Started started,
                            Instant endTime,
                            HttpServletResponse response,
                            Set<Include> includes,
                            Method targetMethod,
                            Class<?> targetClass) {
        Set<Include> includeSet = this.getIncludes(includes, targetMethod, targetClass);
        LogRecord logRecord = this.finish(started, endTime, response, includeSet);
        // 记录日志描述
        if (includeSet.contains(Include.DESCRIPTION)) {
            this.logDescription(logRecord, targetMethod);
        }
        // 记录所属模块
        if (includeSet.contains(Include.MODULE)) {
            this.logModule(logRecord, targetMethod, targetClass);
        }
        return logRecord;
    }

    @Override
    public LogRecord finish(LogRecord.Started started,
                            Instant endTime,
                            HttpServletResponse response,
                            Set<Include> includes) {
        return started.finish(endTime, new RecordableServletHttpResponse(response, response.getStatus()), includes);
    }

    /**
     * 记录日志描述
     *
     * @param logRecord    日志记录
     * @param targetMethod 目标方法
     */
    @Override
    public void logDescription(LogRecord logRecord, Method targetMethod) {
        logRecord.setDescription("请在该接口方法上添加 @top.continew.starter.log.annotation.Log(value) 来指定日志描述");
        Log methodLog = AnnotationUtil.getAnnotation(targetMethod, Log.class);
        // 例如：@Log("新增部门") -> 新增部门
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.value())) {
            logRecord.setDescription(methodLog.value());
            return;
        }
        // 例如：@Operation(summary="新增部门") -> 新增部门
        Operation methodOperation = AnnotationUtil.getAnnotation(targetMethod, Operation.class);
        if (null != methodOperation && CharSequenceUtil.isNotBlank(methodOperation.summary())) {
            logRecord.setDescription(methodOperation.summary());
        }
    }

    /**
     * 记录所属模块
     *
     * @param logRecord    日志记录
     * @param targetMethod 目标方法
     * @param targetClass  目标类
     */
    @Override
    public void logModule(LogRecord logRecord, Method targetMethod, Class<?> targetClass) {
        logRecord.setModule("请在该接口方法或类上添加 @top.continew.starter.log.annotation.Log(module) 来指定所属模块");
        Log methodLog = AnnotationUtil.getAnnotation(targetMethod, Log.class);
        // 例如：@Log(module = "部门管理") -> 部门管理
        // 方法级注解优先级高于类级注解
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.module())) {
            logRecord.setModule(methodLog.module());
            return;
        }
        Log classLog = AnnotationUtil.getAnnotation(targetClass, Log.class);
        if (null != classLog && CharSequenceUtil.isNotBlank(classLog.module())) {
            logRecord.setModule(classLog.module());
            return;
        }
        // 例如：@Tag(name = "部门管理") -> 部门管理
        Tag classTag = AnnotationUtil.getAnnotation(targetClass, Tag.class);
        if (null != classTag && CharSequenceUtil.isNotBlank(classTag.name())) {
            logRecord.setModule(classTag.name());
        }
    }

    @Override
    public Set<Include> getIncludes(Set<Include> includes, Method targetMethod, Class<?> targetClass) {
        Log classLog = AnnotationUtil.getAnnotation(targetClass, Log.class);
        Set<Include> includeSet = new HashSet<>(includes);
        if (null != classLog) {
            this.processInclude(includeSet, classLog);
        }
        // 方法级注解优先级高于类级注解
        Log methodLog = AnnotationUtil.getAnnotation(targetMethod, Log.class);
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

    @Override
    public void processAccessLogStartReq(AccessLogContext accessLogContext) {
        AccessLogProperties properties = accessLogContext.getProperties().getAccessLog();
        // 是否需要打印 规则: 是否打印开关 或 放行路径
        if (!properties.isPrint() || accessLogContext.getProperties()
            .getAccessLog()
            .isMatch(accessLogContext.getRequest().getPath())) {
            return;
        }
        // 构建上下文
        logContextThread.set(accessLogContext);
        RecordableHttpRequest request = accessLogContext.getRequest();
        String path = request.getPath();
        String param = AccessLogUtils.getParam(request, properties);
        log.info(param != null ? "[Start] [{}] {} {}" : "[Start] [{}] {}", request.getMethod(), path, param);
    }

    @Override
    public void processAccessLogEndReq(AccessLogContext accessLogContext) {
        AccessLogContext logContext = logContextThread.get();
        if (ObjectUtil.isNotEmpty(logContext)) {
            try {
                RecordableHttpRequest request = logContext.getRequest();
                RecordableHttpResponse response = accessLogContext.getResponse();
                Duration timeTaken = Duration.between(logContext.getStartTime(), accessLogContext.getEndTime());
                log.info("[End] [{}] {} {} {}ms", request.getMethod(), request.getPath(), response
                    .getStatus(), timeTaken.toMillis());
            } finally {
                logContextThread.remove();
            }
        }
    }
}
