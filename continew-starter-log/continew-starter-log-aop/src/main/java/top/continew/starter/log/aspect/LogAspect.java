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

import cn.hutool.core.annotation.AnnotationUtil;
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
import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.LogHandler;
import top.continew.starter.log.model.LogProperties;
import top.continew.starter.log.model.LogRecord;
import top.continew.starter.web.util.SpringWebUtils;

import java.lang.reflect.Method;
import java.time.Instant;

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
    private final LogProperties logProperties;
    private final LogHandler logHandler;
    private final LogDao logDao;

    public LogAspect(LogProperties logProperties, LogHandler logHandler, LogDao logDao) {
        this.logProperties = logProperties;
        this.logHandler = logHandler;
        this.logDao = logDao;
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
        // 指定规则不记录
        HttpServletRequest request = SpringWebUtils.getRequest();
        Method targetMethod = this.getMethod(joinPoint);
        Class<?> targetClass = joinPoint.getTarget().getClass();
        if (!isRequestRecord(targetMethod, targetClass)) {
            return joinPoint.proceed();
        }
        String errorMsg = null;
        // 开始记录
        LogRecord.Started startedLogRecord = logHandler.start(startTime, request);
        try {
            // 执行目标方法
            return joinPoint.proceed();
        } catch (Exception e) {
            errorMsg = CharSequenceUtil.sub(e.getMessage(), 0, 2000);
            throw e;
        } finally {
            try {
                Instant endTime = Instant.now();
                HttpServletResponse response = SpringWebUtils.getResponse();
                LogRecord logRecord = logHandler.finish(startedLogRecord, endTime, response, logProperties
                    .getIncludes(), targetMethod, targetClass);
                // 记录异常信息
                if (errorMsg != null) {
                    logRecord.setErrorMsg(errorMsg);
                }
                logDao.add(logRecord);
            } catch (Exception e) {
                log.error("Logging http log occurred an error: {}.", e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * 是否要记录日志
     *
     * @param targetMethod 目标方法
     * @param targetClass  目标类
     * @return true：需要记录；false：不需要记录
     */
    private boolean isRequestRecord(Method targetMethod, Class<?> targetClass) {
        // 非 Web 环境不记录
        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getResponse() == null) {
            return false;
        }
        // 如果接口匹配排除列表，不记录日志
        if (logProperties.isMatch(attributes.getRequest().getRequestURI())) {
            return false;
        }
        // 如果接口方法或类上有 @Log 注解，且要求忽略该接口，则不记录日志
        Log methodLog = AnnotationUtil.getAnnotation(targetMethod, Log.class);
        if (null != methodLog && methodLog.ignore()) {
            return false;
        }
        Log classLog = AnnotationUtil.getAnnotation(targetClass, Log.class);
        return null == classLog || !classLog.ignore();
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
}
