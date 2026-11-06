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

package top.continew.starter.openapi.exception;

import top.continew.starter.openapi.constant.OpenApiSignErrorCodes;

/**
 * 开放接口验签异常
 *
 * <p>携带结构化错误码，验签链路任一步骤失败时抛出，由验签过滤器统一转换为失败响应。
 * 异常 message 使用错误码固定文案，不携带密钥、签名与请求体等敏感内容。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class OpenApiSignException extends RuntimeException {

    /**
     * 错误码
     */
    private final transient OpenApiSignErrorCodes errorCode;

    public OpenApiSignException(OpenApiSignErrorCodes errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public OpenApiSignErrorCodes getErrorCode() {
        return errorCode;
    }
}
