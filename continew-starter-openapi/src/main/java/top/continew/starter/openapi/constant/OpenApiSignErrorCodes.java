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

package top.continew.starter.openapi.constant;

/**
 * 开放接口验签错误码
 *
 * <p>覆盖验签链路全部失败出口，code 供接入方定位失败环节，msg 为固定文案（不拼接异常文本），
 * httpStatus 为响应的 HTTP 状态码。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public enum OpenApiSignErrorCodes {

    /**
     * 签名头缺失、格式错、重复头、长度或字符集违规、query 含旧协议公共参数
     */
    SIGN_HEADER_INVALID("E400", "签名头缺失或格式不合法", 400),

    /**
     * 请求体超过大小上限
     */
    BODY_TOO_LARGE("E413", "请求体超过允许的大小上限", 413),

    /**
     * multipart 请求不受本协议支持
     */
    MULTIPART_UNSUPPORTED("E415", "签名接口不支持 multipart 请求", 415),

    /**
     * form 表单请求不受本协议支持（容器会重新编码表单，摘要契约无法成立）
     */
    FORM_UNSUPPORTED("E415", "签名接口不支持 form 表单请求，请使用 JSON 等原文可复现的请求体", 415),

    /**
     * 签名过期（timestamp 超出允许窗口）
     */
    SIGN_EXPIRED("E401", "签名已过期，请校准系统时钟后重新签名", 401),

    /**
     * nonce 重放
     */
    NONCE_REPLAY("E409", "nonce 已被使用，请勿重复提交", 409),

    /**
     * 凭证无效（accessKey 不存在或密钥未配置）
     */
    CREDENTIAL_INVALID("E401", "凭证无效，accessKey 不存在或密钥未配置", 401),

    /**
     * 应用已禁用
     */
    APP_DISABLED("E403", "应用已被禁用", 403),

    /**
     * 应用已过期
     */
    APP_EXPIRED("E403", "应用已过期", 403),

    /**
     * 请求体摘要与摘要头不一致
     */
    BODY_DIGEST_MISMATCH("E403", "请求体摘要校验失败，请确认签名与传输的内容一致", 403),

    /**
     * 签名不匹配
     */
    SIGNATURE_MISMATCH("E401", "签名不匹配，请检查签名算法与密钥", 401),

    /**
     * 应用无权访问该接口（接口不在应用-接口清单）
     */
    APP_NO_PERMISSION("E403", "应用无权访问该接口", 403);

    private final String code;

    private final String msg;

    private final int httpStatus;

    OpenApiSignErrorCodes(String code, String msg, int httpStatus) {
        this.code = code;
        this.msg = msg;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
