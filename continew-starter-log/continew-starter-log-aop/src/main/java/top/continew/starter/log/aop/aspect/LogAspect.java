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

package top.continew.starter.log.aop.aspect;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.log.aop.annotation.Log;
import top.continew.starter.log.aop.autoconfigure.LogProperties;
import top.continew.starter.log.core.dao.LogDao;
import top.continew.starter.log.core.enums.Include;
import top.continew.starter.log.core.http.recordable.impl.RecordableServletHttpRequest;
import top.continew.starter.log.core.http.recordable.impl.RecordableServletHttpResponse;
import top.continew.starter.log.core.model.LogRecord;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 日志切面
 *
 * @author echo
 * @date 2024/12/06 09:58
 **/
@Aspect
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);
    private final LogDao logDao;
    private final LogProperties logProperties;
    private final TransmittableThreadLocal<Instant> timeTtl = new TransmittableThreadLocal<>();
    private final TransmittableThreadLocal<LogRecord.Started> logTtl = new TransmittableThreadLocal<>();

    public LogAspect(LogDao logDao, LogProperties logProperties) {
        this.logDao = logDao;
        this.logProperties = logProperties;
    }

    /**
     * 切点 - 匹配日志注解 {@link Log}
     */
    @Pointcut(value = "@annotation(top.continew.starter.log.aop.annotation.Log)")
    public void pointcutService() {
    }

    /**
     * 处理请求前执行
     */
    @Before(value = "pointcutService()")
    public void doBefore() {
        Instant startTime = Instant.now();
        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            LogRecord.Started startedLogRecord = LogRecord.start(startTime, new RecordableServletHttpRequest(request));
            logTtl.set(startedLogRecord);
        }
    }

    /**
     * 处理请求后执行 - 正常返回
     *
     * @param joinPoint 切点
     */
    @After(value = "pointcutService()")
    public void afterAdvice(JoinPoint joinPoint) {
        handleAfterCompletion(joinPoint, null);
    }

    /**
     * 处理请求后执行 - 异常情况
     *
     * @param joinPoint 切点
     * @param ex        异常
     */
    @AfterThrowing(pointcut = "pointcutService()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Exception ex) {
        handleAfterCompletion(joinPoint, ex);
    }

    /**
     * 处理请求完成后的逻辑
     *
     * @param joinPoint 切点
     * @param ex        异常
     * @param result    返回结果
     */
    private void handleAfterCompletion(JoinPoint joinPoint, Exception ex) {
        try {
            Instant endTime = Instant.now();
            ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletResponse response = attributes.getResponse();
            // 处理日志记录
            LogRecord.Started startedLogRecord = logTtl.get();
            if (startedLogRecord == null) {
                return;
            }

            // 获取方法和类注解信息
            MethodSignature signature = (MethodSignature)joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?> targetClass = joinPoint.getTarget().getClass();

            Log methodLog = method.getAnnotation(Log.class);
            Log classLog = targetClass.getAnnotation(Log.class);

            // 获取日志包含信息
            Set<Include> includeSet = getIncludes(methodLog, classLog);

            // 完成日志记录
            LogRecord finishedLogRecord = startedLogRecord
                .finish(endTime, new RecordableServletHttpResponse(response, response.getStatus()), includeSet);
            // 记录异常
            if (ex != null) {
                finishedLogRecord.getResponse().setStatus(1);
                finishedLogRecord.setErrorMsg(StrUtil.sub(ex.getMessage(), 0, 2000));
            }

            // 记录日志描述
            if (includeSet.contains(Include.DESCRIPTION)) {
                logDescription(finishedLogRecord, methodLog);
            }

            // 记录所属模块
            if (includeSet.contains(Include.MODULE)) {
                logModule(finishedLogRecord, methodLog, classLog);
            }
            logDao.add(finishedLogRecord);
        } catch (Exception e) {
            log.error("Logging http log occurred an error: {}.", e.getMessage(), e);
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
        // 如果方法注解存在日志描述
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
