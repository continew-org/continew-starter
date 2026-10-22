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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 响应信息
 *
 * @param <T> 响应数据类型
 * @author Charles7c
 * @since 1.0.0
 */
@Schema(description = "响应信息")
public class R<T> {

    /**
     * 状态码
     */
    @Schema(description = "状态码", example = "0")
    private String code;

    /**
     * 状态信息
     */
    @Schema(description = "状态信息", example = "ok")
    private String msg;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private T data;

    public R() {
    }

    /**
     * 构造方法
     *
     * @param code 状态码
     * @param msg  状态信息
     */
    public R(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 构造方法
     *
     * @param code 状态码
     * @param msg  状态信息
     * @param data 响应数据
     */
    public R(String code, String msg, T data) {
        this(code, msg);
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Objects.equals(ResponseConfig.getSuccessCode(), code);
    }

    /**
     * 时间戳
     *
     * @return 当前时间戳
     */
    public Long getTimestamp() {
        return System.currentTimeMillis();
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * 操作成功
     *
     * @return 响应信息
     */
    public static R ok() {
        return new R(ResponseConfig.getSuccessCode(), ResponseConfig.getSuccessMsg());
    }

    /**
     * 操作成功
     *
     * @param data 响应数据
     * @return 响应信息
     */
    public static R ok(Object data) {
        return new R(ResponseConfig.getSuccessCode(), ResponseConfig.getSuccessMsg(), data);
    }

    /**
     * 操作成功
     *
     * @param msg  状态信息
     * @param data 响应数据
     * @return 响应信息
     */
    public static R ok(String msg, Object data) {
        R r = ok(data);
        r.setMsg(msg);
        return r;
    }

    /**
     * 操作失败
     *
     * @return 响应信息
     */
    public static R fail() {
        return new R(ResponseConfig.getErrorCode(), ResponseConfig.getErrorMsg());
    }

    /**
     * 操作失败
     *
     * @param code 状态码
     * @param msg  状态信息
     * @return 响应信息
     */
    public static R fail(String code, String msg) {
        return new R(code, msg);
    }
}
