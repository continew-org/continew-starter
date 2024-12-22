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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.log.autoconfigure.LogProperties;

import java.time.Duration;
import java.time.Instant;

/**
 * 访问日志切面
 *
 * @author echo
 * @author Charles7c
 * @since 2.8.0
 */
@Aspect
public class AccessLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AccessLogAspect.class);
    private final LogProperties logProperties;

    public AccessLogAspect(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    /**
     * 切点 - 匹配所有控制器层的方法
     */
    @Pointcut("execution(* *..controller.*.*(..)) || execution(* *..*Controller.*(..))")
    public void pointcut() {
    }

    /**
     * 打印访问日志
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
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        try {
            // 打印请求日志
            if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
                log.info("[{}] {}", request.getMethod(), request.getRequestURI());
            }
            return joinPoint.proceed();
        } finally {
            Instant endTime = Instant.now();
            if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
                Duration timeTaken = Duration.between(startTime, endTime);
                log.info("[{}] {} {} {}ms", request.getMethod(), request.getRequestURI(), response != null
                    ? response.getStatus()
                    : "N/A", timeTaken.toMillis());
            }
        }
    }
}
