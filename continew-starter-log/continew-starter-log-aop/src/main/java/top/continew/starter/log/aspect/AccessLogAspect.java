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
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.log.handler.LogHandler;
import top.continew.starter.log.http.servlet.RecordableServletHttpRequest;
import top.continew.starter.log.http.servlet.RecordableServletHttpResponse;
import top.continew.starter.log.model.AccessLogContext;
import top.continew.starter.log.model.LogProperties;

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
    private final LogHandler logHandler;

    public AccessLogAspect(LogProperties logProperties, LogHandler logHandler) {
        this.logProperties = logProperties;
        this.logHandler = logHandler;
    }

    /**
     * 切点 - 匹配所有控制器层的 GET 请求方法
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void pointcut() {
    }

    /**
     * 切点 - 匹配所有控制器层的 GET 请求方法
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void pointcutGet() {
    }

    /**
     * 切点 - 匹配所有控制器层的 POST 请求方法
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void pointcutPost() {
    }

    /**
     * 切点 - 匹配所有控制器层的 PUT 请求方法
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.PutMapping)")
    public void pointcutPut() {
    }

    /**
     * 切点 - 匹配所有控制器层的 DELETE 请求方法
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void pointcutDelete() {
    }

    /**
     * 切点 - 匹配所有控制器层的 PATCH 请求方法
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public void pointcutPatch() {
    }

    /**
     * 打印访问日志
     *
     * @param joinPoint 切点
     * @return 返回结果
     * @throws Throwable 异常
     */
    @Around("pointcut() || pointcutGet() || pointcutPost() || pointcutPut() || pointcutDelete() || pointcutPatch()")
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
            logHandler.processAccessLogStartReq(AccessLogContext.builder()
                .startTime(startTime)
                .request(new RecordableServletHttpRequest(request))
                .properties(logProperties)
                .build());
            return joinPoint.proceed();
        } finally {
            Instant endTime = Instant.now();
            logHandler.processAccessLogEndReq(AccessLogContext.builder()
                .endTime(endTime)
                .response(new RecordableServletHttpResponse(response, response.getStatus()))
                .build());
        }
    }
}
