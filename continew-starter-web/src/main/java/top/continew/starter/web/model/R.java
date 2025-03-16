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

import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.feiniaojin.gracefulresponse.api.ResponseStatusFactory;
import com.feiniaojin.gracefulresponse.data.Response;
import com.feiniaojin.gracefulresponse.data.ResponseStatus;
import com.feiniaojin.gracefulresponse.defaults.DefaultResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 响应信息
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Schema(description = "响应信息")
public class R<T> implements Response {

    private static final ResponseStatusFactory RESPONSE_STATUS_FACTORY = SpringUtil
        .getBean(ResponseStatusFactory.class);
    private static final ResponseStatus DEFAULT_STATUS_SUCCESS = RESPONSE_STATUS_FACTORY.defaultSuccess();
    private static final ResponseStatus DEFAULT_STATUS_ERROR = RESPONSE_STATUS_FACTORY.defaultError();

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
     * 是否成功
     */
    @Schema(description = "是否成功", example = "true")
    private boolean success;

    /**
     * 时间戳
     */
    @Schema(description = "时间戳", example = "1691453288000")
    private Long timestamp;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 状态信息
     */
    private ResponseStatus status = new DefaultResponseStatus();

    public R() {
    }

    public R(ResponseStatus status) {
        this.status = status;
    }

    public R(String code, String msg) {
        this.setCode(code);
        this.setMsg(msg);
    }

    public R(ResponseStatus status, T data) {
        this(status);
        this.setData(data);
    }

    public R(String code, String msg, T data) {
        this(code, msg);
        this.setData(data);
    }

    @Override
    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    @Override
    @JsonIgnore
    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public void setPayload(Object payload) {
        this.data = (T)payload;
    }

    @Override
    @JsonIgnore
    public Object getPayload() {
        return data;
    }

    public String getCode() {
        return status.getCode();
    }

    public void setCode(String code) {
        status.setCode(code);
    }

    public String getMsg() {
        return status.getMsg();
    }

    public void setMsg(String msg) {
        status.setMsg(msg);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return Objects.equals(DEFAULT_STATUS_SUCCESS.getCode(), status.getCode());
    }

    public Long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 操作成功
     *
     * @return R /
     */
    public static R ok() {
        return new R(DEFAULT_STATUS_SUCCESS);
    }

    /**
     * 操作成功
     *
     * @param data 响应数据
     * @return R /
     */
    public static R ok(Object data) {
        return new R(DEFAULT_STATUS_SUCCESS, data);
    }

    /**
     * 操作成功
     *
     * @param msg  业务状态信息
     * @param data 响应数据
     * @return R /
     */
    public static R ok(String msg, Object data) {
        R r = ok(data);
        r.setMsg(msg);
        return r;
    }

    /**
     * 操作失败
     *
     * @return R /
     */
    public static R fail() {
        return new R(DEFAULT_STATUS_ERROR);
    }

    /**
     * 操作失败
     *
     * @param code 业务状态码
     * @param msg  业务状态信息
     * @return R /
     */
    public static R fail(String code, String msg) {
        return new R(code, msg);
    }
}
