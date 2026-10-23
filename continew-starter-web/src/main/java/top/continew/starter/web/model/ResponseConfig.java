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

package top.continew.starter.web.model;

/**
 * 响应默认状态配置
 * <p>
 * {@link R} 由控制器静态创建，无法直接注入配置属性，因此由 {@link DefaultGlobalResponseFactory}
 * 在创建时写入默认值。未启动 Spring 上下文时使用内置默认值（0/ok/1/error）。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
final class ResponseConfig {

    private static volatile String successCode = "0";

    private static volatile String successMsg = "ok";

    private static volatile String errorCode = "1";

    private static volatile String errorMsg = "error";

    private ResponseConfig() {
    }

    static void configure(String successCode, String successMsg, String errorCode,
        String errorMsg) {
        ResponseConfig.successCode = successCode;
        ResponseConfig.successMsg = successMsg;
        ResponseConfig.errorCode = errorCode;
        ResponseConfig.errorMsg = errorMsg;
    }

    static String getSuccessCode() {
        return successCode;
    }

    static String getSuccessMsg() {
        return successMsg;
    }

    static String getErrorCode() {
        return errorCode;
    }

    static String getErrorMsg() {
        return errorMsg;
    }
}
