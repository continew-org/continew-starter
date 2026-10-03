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

package top.continew.starter.auth.refresh.token.exception;

import org.springframework.http.HttpStatus;
import top.continew.starter.core.exception.BusinessException;

import java.io.Serial;

/**
 * Refresh Token 协议异常。
 *
 * <p>认证终止、来源拒绝和限流必须使用稳定且可区分的业务状态码，客户端才能只在
 * Refresh Token 确定失效时清理登录态。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public class RefreshTokenException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    private RefreshTokenException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 获取对应的 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public HttpStatus getStatus() {
        return this.status;
    }

    /**
     * 创建 401 Unauthorized 异常。
     *
     * @param message 异常信息
     * @return Refresh Token 协议异常
     */
    public static RefreshTokenException unauthorized(String message) {
        return new RefreshTokenException(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * 创建 403 Forbidden 异常。
     *
     * @param message 异常信息
     * @return Refresh Token 协议异常
     */
    public static RefreshTokenException forbidden(String message) {
        return new RefreshTokenException(HttpStatus.FORBIDDEN, message);
    }

    /**
     * 创建 429 Too Many Requests 异常。
     *
     * @param message 异常信息
     * @return Refresh Token 协议异常
     */
    public static RefreshTokenException tooManyRequests(String message) {
        return new RefreshTokenException(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    /**
     * 创建 500 Internal Server Error 异常。
     *
     * @param message 异常信息
     * @return Refresh Token 协议异常
     */
    public static RefreshTokenException internalServerError(String message) {
        return new RefreshTokenException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
