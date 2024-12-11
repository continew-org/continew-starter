package top.continew.starter.log.aop.aspect;

import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.log.aop.autoconfigure.LogProperties;

import java.time.Duration;
import java.time.Instant;

/**
 * 控制台 输出日志切面
 *
 * @author echo
 * @date 2024/12/06 10:33
 **/
@Aspect
public class ConsoleLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ConsoleLogAspect.class);
    private final LogProperties logProperties;
    private final TransmittableThreadLocal<Instant> timeTtl = new TransmittableThreadLocal<>();

    public ConsoleLogAspect(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    /**
     * 切点 - 匹配所有控制器层的方法
     */
    @Pointcut("execution(* *..controller.*.*(..)) || execution(* *..*Controller.*(..))")
    public void controllerLayer() {
    }

    /**
     * 处理请求前执行
     */
    @Before(value = "controllerLayer()")
    public void doBefore() {
        // 打印请求日志
        if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
            Instant startTime = Instant.now();
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.info("[{}] {}", request.getMethod(), request.getRequestURI());
            }
            timeTtl.set(startTime);
        }
    }

    /**
     * 处理请求后执行
     */
    @After(value = "controllerLayer()")
    public void afterAdvice() {
        // 打印请求耗时
        if (Boolean.TRUE.equals(logProperties.getIsPrint())) {
            Instant endTime = Instant.now();
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            HttpServletResponse response = attributes.getResponse();
            Duration timeTaken = Duration.between(timeTtl.get(), endTime);
            log.info("[{}] {} {} {}ms", request.getMethod(), request.getRequestURI(),
                    response != null ? response.getStatus() : "N/A",
                    timeTaken.toMillis());
        }
    }
}
