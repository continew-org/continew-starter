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

import cn.hutool.core.text.CharSequenceUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.log.autoconfigure.LogProperties;
import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.http.recordable.impl.RecordableServletHttpRequest;
import top.continew.starter.log.http.recordable.impl.RecordableServletHttpResponse;
import top.continew.starter.log.model.LogRecord;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 日志切面
 *
 * @author echo
 * @author Charles7c
 * @since 2.8.0
 */
@Aspect
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);
    private final LogDao logDao;
    private final LogProperties logProperties;

    public LogAspect(LogDao logDao, LogProperties logProperties) {
        this.logDao = logDao;
        this.logProperties = logProperties;
    }

    /**
     * 切点 - 匹配日志注解 {@link Log}
     */
    @Pointcut("@annotation(top.continew.starter.log.annotation.Log)")
    public void pointcut() {
    }

    /**
     * 记录日志
     *
     * @param joinPoint 切点
     * @return 返回结果
     * @throws Throwable 异常
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant startTime = Instant.now();
        // 非 Web 环境不记录
        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getResponse() == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        String errorMsg = null;
        // 开始记录
        LogRecord.Started startedLogRecord = LogRecord.start(startTime, new RecordableServletHttpRequest(request));
        try {
            // 执行目标方法
            return joinPoint.proceed();
        } catch (Exception e) {
            errorMsg = CharSequenceUtil.sub(e.getMessage(), 0, 2000);
            throw e;
        } finally {
            // 结束记录
            this.logFinish(startedLogRecord, errorMsg, response, joinPoint);
        }
    }

    /**
     * 结束记录日志
     *
     * @param startedLogRecord 日志记录器
     * @param errorMsg         异常信息
     * @param response         响应对象
     * @param joinPoint        切点
     */
    private void logFinish(LogRecord.Started startedLogRecord,
                           String errorMsg,
                           HttpServletResponse response,
                           ProceedingJoinPoint joinPoint) {
        try {
            Instant endTime = Instant.now();
            Method method = this.getMethod(joinPoint);
            Class<?> targetClass = joinPoint.getTarget().getClass();
            Log methodLog = method.getAnnotation(Log.class);
            Log classLog = targetClass.getAnnotation(Log.class);
            Set<Include> includeSet = this.getIncludes(methodLog, classLog);
            LogRecord finishedLogRecord = startedLogRecord
                .finish(endTime, new RecordableServletHttpResponse(response, response.getStatus()), includeSet);
            // 记录异常信息
            if (errorMsg != null) {
                finishedLogRecord.setErrorMsg(errorMsg);
            }
            // 记录日志描述
            if (includeSet.contains(Include.DESCRIPTION)) {
                this.logDescription(finishedLogRecord, methodLog);
            }
            // 记录所属模块
            if (includeSet.contains(Include.MODULE)) {
                this.logModule(finishedLogRecord, methodLog, classLog);
            }
            logDao.add(finishedLogRecord);
        } catch (Exception e) {
            log.error("Logging http log occurred an error: {}.", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取方法
     *
     * @param joinPoint 切点
     * @return 方法
     */
    private Method getMethod(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        return signature.getMethod();
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
            processInclude(includeSet, classLog);
        }
        if (null != methodLog) {
            processInclude(includeSet, methodLog);
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
     * @param logRecord 日志信息
     * @param methodLog 方法级 Log 注解
     */
    private void logDescription(LogRecord logRecord, Log methodLog) {
        // 例如：@Log("新增部门") -> 新增部门
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.value())) {
            logRecord.setDescription(methodLog.value());
        } else {
            logRecord.setDescription("请在该接口方法上指定日志描述");
        }
    }

    /**
     * 记录模块
     *
     * @param logRecord 日志信息
     * @param methodLog 方法级 Log 注解
     * @param classLog  类级 Log 注解
     */
    private void logModule(LogRecord logRecord, Log methodLog, Log classLog) {
        // 例如：@Log(module = "部门管理") -> 部门管理
        // 优先使用方法注解的模块
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.module())) {
            logRecord.setModule(methodLog.module());
            return;
        }
        // 其次使用类注解的模块
        if (null != classLog) {
            logRecord.setModule(CharSequenceUtil.blankToDefault(classLog.module(), "请在该接口类上指定所属模块"));
        }
    }
}
