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

package top.continew.starter.core.exception;

import top.continew.starter.core.response.CommonResCode;
import top.continew.starter.core.response.ResponseCode;

import java.io.Serial;

/**
 * 业务异常
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class BusinessException extends BaseException {

    private Long code;

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException() {
        super(CommonResCode.FAIL.getDescription());
        this.code = CommonResCode.FAIL.getCode();
    }

    public BusinessException(String msg) {
        super(msg);
        this.code = CommonResCode.FAIL.getCode();
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getDescription());
        this.code = responseCode.getCode();
    }

    public BusinessException(ResponseCode responseCode, Throwable throwable) {
        super(responseCode.getDescription(), throwable);
        this.code = responseCode.getCode();
    }

    public BusinessException(Long code, String msg) {
        super(msg);
        this.code = code;
    }

    public Long getCode() {
        return code;
    }

    public void setCode(Long code) {
        this.code = code;
    }
}
