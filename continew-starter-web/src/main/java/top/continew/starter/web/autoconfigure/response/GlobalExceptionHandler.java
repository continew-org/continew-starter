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

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.continew.starter.web.model.R;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 未匹配到业务方自定义异常处理器的异常由本处理器兜底。若异常命中例外配置，则原样抛出，
 * 继续交由框架或错误页处理。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
@RestControllerAdvice
@Order(200)
public class GlobalExceptionHandler {

    private final GlobalResponseProperties properties;

    private final GlobalExceptionResolver exceptionResolver;

    /**
     * 构造方法
     *
     * @param properties                    全局响应配置属性
     * @param exceptionAliasRegister        异常别名注册器
     * @param beforeControllerAdviceProcess 异常处理前回调
     */
    public GlobalExceptionHandler(GlobalResponseProperties properties,
        ExceptionAliasRegister exceptionAliasRegister,
        BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
        this.properties = properties;
        this.exceptionResolver = new GlobalExceptionResolver(properties, exceptionAliasRegister,
            beforeControllerAdviceProcess);
    }

    /**
     * 处理参数校验异常
     *
     * @param e       参数校验异常
     * @param request 当前请求
     * @return 统一错误响应
     * @throws Exception 异常命中例外配置时原样抛出
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R> handleBindException(BindException e,
        HttpServletRequest request) throws Exception {
        exceptionResolver.before(request, e);
        if (exceptionResolver.isExcluded(e)) {
            throw e;
        }
        String msg = e.getBindingResult()
            .getAllErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(";"));
        if (!StringUtils.hasText(msg)) {
            msg = properties.getDefaultErrorMsg();
        }
        return ResponseEntity.status(exceptionResolver.resolveValidationHttpStatus())
            .body(R.fail(exceptionResolver.resolveValidationCode(), msg));
    }

    /**
     * 处理未匹配的异常
     *
     * @param e       当前异常
     * @param request 当前请求
     * @return 统一错误响应
     * @throws Exception 异常命中例外配置时原样抛出
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R> handleException(Exception e,
        HttpServletRequest request) throws Exception {
        exceptionResolver.before(request, e);
        if (exceptionResolver.isExcluded(e)) {
            throw e;
        }
        GlobalExceptionResolver.ResponseInfo responseInfo = exceptionResolver
            .resolveResponseInfo(e);
        return ResponseEntity.status(exceptionResolver.resolveHttpStatus(e))
            .body(R.fail(responseInfo.code(), responseInfo.msg()));
    }
}
