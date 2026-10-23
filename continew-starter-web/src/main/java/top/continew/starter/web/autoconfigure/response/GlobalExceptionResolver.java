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

package top.continew.starter.web.autoconfigure.response;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import top.continew.starter.web.annotation.ExceptionAliasFor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 全局异常响应解析器
 * <p>
 * 统一处理例外异常放行、异常别名与 HTTP 状态码解析，供各类异常处理器复用。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class GlobalExceptionResolver {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GlobalResponseProperties properties;

    private final ExceptionAliasRegister exceptionAliasRegister;

    private final BeforeControllerAdviceProcess beforeControllerAdviceProcess;

    /**
     * 构造方法
     *
     * @param properties                    全局响应配置属性
     * @param exceptionAliasRegister        异常别名注册器
     * @param beforeControllerAdviceProcess 异常处理前回调
     */
    public GlobalExceptionResolver(GlobalResponseProperties properties,
        ExceptionAliasRegister exceptionAliasRegister,
        BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
        this.properties = properties;
        this.exceptionAliasRegister = exceptionAliasRegister;
        this.beforeControllerAdviceProcess = beforeControllerAdviceProcess;
    }

    /**
     * 异常处理前回调，并在命中例外配置时原样抛出
     * <p>
     * 原样抛出后由 {@code ExceptionHandlerExceptionResolver} 记录一条 WARN 并返回 {@code null}，
     * 后续的 {@code ResponseStatusExceptionResolver} / {@code DefaultHandlerExceptionResolver}
     * 继续处理，从而保留框架默认语义（如 400/405）。这是有意的放行方式，WARN 属预期输出。
     * </p>
     *
     * @param request 当前请求
     * @param e       当前异常
     * @throws Exception 异常命中例外配置时原样抛出
     */
    public void beforeHandle(HttpServletRequest request, Exception e) throws Exception {
        before(request, e);
        if (isExcluded(e)) {
            throw e;
        }
    }

    /**
     * 异常处理前回调
     *
     * @param request 当前请求
     * @param e       当前异常
     */
    void before(HttpServletRequest request, Exception e) {
        beforeControllerAdviceProcess.call(request, null, null, e);
    }

    /**
     * 是否为例外异常
     *
     * @param e 当前异常
     * @return 是否为例外异常
     */
    boolean isExcluded(Exception e) {
        Set<Class<?>> excludeExceptionTypes = properties.getExcludeExceptionTypes();
        if (!CollectionUtils.isEmpty(excludeExceptionTypes)) {
            Class<? extends Exception> exceptionClass = e.getClass();
            boolean typeMatched = excludeExceptionTypes.contains(exceptionClass)
                || excludeExceptionTypes.stream()
                    .anyMatch(item -> item.isAssignableFrom(exceptionClass));
            if (typeMatched) {
                return true;
            }
        }
        List<String> excludeExceptionPackages = properties.getExcludeExceptionPackages();
        if (CollectionUtils.isEmpty(excludeExceptionPackages)) {
            return false;
        }
        String packageName = e.getClass().getPackageName();
        return excludeExceptionPackages.stream()
            .filter(Objects::nonNull)
            .anyMatch(item -> PATH_MATCHER.match(item, packageName));
    }

    /**
     * 解析响应码与响应信息
     *
     * @param e 当前异常
     * @return 响应码与响应信息
     */
    ResponseInfo resolveResponseInfo(Exception e) {
        ExceptionAliasConfig aliasConfig = getAliasConfig(e.getClass());
        if (aliasConfig != null) {
            return new ResponseInfo(defaultIfBlank(aliasConfig.getCode(), properties
                .getDefaultErrorCode()), defaultIfBlank(aliasConfig.getMsg(),
                    properties
                        .getDefaultErrorMsg()));
        }
        ExceptionAliasFor exceptionAliasFor = getExceptionAliasFor(e.getClass());
        if (exceptionAliasFor != null) {
            return new ResponseInfo(defaultIfBlank(exceptionAliasFor.code(), properties
                .getDefaultErrorCode()), defaultIfBlank(exceptionAliasFor.msg(),
                    properties
                        .getDefaultErrorMsg()));
        }
        String msg = properties.isOriginExceptionUsingDetailMessage() && e.getMessage() != null
            ? e.getMessage()
            : properties.getDefaultErrorMsg();
        return new ResponseInfo(properties.getDefaultErrorCode(), msg);
    }

    /**
     * 解析异常对应的 HTTP 状态码
     *
     * @param e 当前异常
     * @return HTTP 状态码
     */
    HttpStatusCode resolveHttpStatus(Exception e) {
        ResponseStatus responseStatus = AnnotatedElementUtils
            .findMergedAnnotation(e.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.code();
        }
        ExceptionAliasConfig aliasConfig = getAliasConfig(e.getClass());
        if (aliasConfig != null && aliasConfig.getHttpStatusCode() != null) {
            return HttpStatusCode.valueOf(aliasConfig.getHttpStatusCode());
        }
        ExceptionAliasFor exceptionAliasFor = getExceptionAliasFor(e.getClass());
        if (exceptionAliasFor != null && exceptionAliasFor.httpStatusCode() > 0) {
            return HttpStatusCode.valueOf(exceptionAliasFor.httpStatusCode());
        }
        return defaultErrorHttpStatus();
    }

    /**
     * 解析参数校验异常对应的 HTTP 状态码
     *
     * @return HTTP 状态码
     */
    HttpStatusCode resolveValidationHttpStatus() {
        Integer statusCode = properties.getDefaultHttpStatusCodeOnValidationError();
        return statusCode != null ? HttpStatusCode.valueOf(statusCode) : defaultErrorHttpStatus();
    }

    private HttpStatusCode defaultErrorHttpStatus() {
        Integer statusCode = properties.getDefaultHttpStatusCodeOnError();
        return HttpStatusCode.valueOf(statusCode != null ? statusCode : HttpStatus.OK.value());
    }

    /**
     * 获取参数校验失败响应码
     *
     * @return 响应码
     */
    String resolveValidationCode() {
        return StringUtils.hasText(properties.getDefaultValidateErrorCode())
            ? properties.getDefaultValidateErrorCode()
            : properties.getDefaultErrorCode();
    }

    private ExceptionAliasConfig getAliasConfig(Class<? extends Throwable> throwableClass) {
        Map<Class<?>, ExceptionAliasConfig> aliasConfigMap = properties
            .getExceptionAliasConfigMap();
        if (CollectionUtils.isEmpty(aliasConfigMap)) {
            return null;
        }
        for (Class<?> type = throwableClass; type != null; type = type.getSuperclass()) {
            ExceptionAliasConfig aliasConfig = aliasConfigMap.get(type);
            if (aliasConfig != null) {
                return aliasConfig;
            }
        }
        return null;
    }

    private ExceptionAliasFor getExceptionAliasFor(Class<? extends Throwable> throwableClass) {
        for (Class<?> type = throwableClass; Throwable.class
            .isAssignableFrom(type); type = type.getSuperclass()) {
            ExceptionAliasFor exceptionAliasFor = exceptionAliasRegister
                .getExceptionAliasFor(type.asSubclass(Throwable.class));
            if (exceptionAliasFor != null) {
                return exceptionAliasFor;
            }
        }
        return null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    /**
     * 响应状态
     *
     * @param code 响应码
     * @param msg  响应信息
     */
    record ResponseInfo(String code, String msg) {
    }
}
