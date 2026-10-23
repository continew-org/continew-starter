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

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 参数校验异常处理器
 * <p>
 * 处理 {@code @Validated} 标注的类或方法上的约束校验异常。仅当类路径存在 Bean Validation API
 * 时由自动配置注册。本处理器为 {@code @Order(190)}，业务方的 {@code @RestControllerAdvice}
 * 需声明小于 190 的 {@code @Order} 才会优先处理。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
@RestControllerAdvice
@Order(190)
public class ConstraintViolationExceptionHandler {

    private final GlobalResponseProperties properties;

    private final GlobalResponseFactory responseFactory;

    private final GlobalExceptionResolver exceptionResolver;

    /**
     * 构造方法
     *
     * @param properties        全局响应配置属性
     * @param responseFactory   全局响应工厂
     * @param exceptionResolver 全局异常响应解析器
     */
    public ConstraintViolationExceptionHandler(GlobalResponseProperties properties,
        GlobalResponseFactory responseFactory,
        GlobalExceptionResolver exceptionResolver) {
        this.properties = properties;
        this.responseFactory = responseFactory;
        this.exceptionResolver = exceptionResolver;
    }

    /**
     * 处理约束校验异常
     *
     * @param e       约束校验异常
     * @param request 当前请求
     * @return 统一错误响应
     * @throws Exception 异常命中例外配置时原样抛出
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @SuppressWarnings("java:S112")
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e,
        HttpServletRequest request) throws Exception {
        exceptionResolver.beforeHandle(request, e);
        String msg = e.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getMessage)
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(";"));
        if (!StringUtils.hasText(msg)) {
            msg = properties.getDefaultErrorMsg();
        }
        return ResponseEntity.status(exceptionResolver.resolveValidationHttpStatus())
            .body(responseFactory.newFailure(exceptionResolver.resolveValidationCode(), msg));
    }
}
