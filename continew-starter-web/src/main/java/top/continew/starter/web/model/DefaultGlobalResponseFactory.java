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

import org.jspecify.annotations.Nullable;
import top.continew.starter.web.autoconfigure.response.GlobalResponseFactory;
import top.continew.starter.web.autoconfigure.response.GlobalResponseProperties;

/**
 * 默认全局响应工厂
 * <p>
 * 返回 {@link R}，并在创建时把配置默认值写入 {@link R} 的静态状态。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class DefaultGlobalResponseFactory implements GlobalResponseFactory {

    /**
     * 构造方法
     *
     * @param properties 全局响应配置属性
     */
    public DefaultGlobalResponseFactory(GlobalResponseProperties properties) {
        ResponseConfig.configure(properties.getDefaultSuccessCode(), properties
            .getDefaultSuccessMsg(), properties.getDefaultErrorCode(),
            properties.getDefaultErrorMsg());
    }

    @Override
    public Object newSuccess(@Nullable Object data) {
        return data == null ? R.ok() : R.ok(data);
    }

    @Override
    public Object newFailure(String code, String msg) {
        return R.fail(code, msg);
    }

    @Override
    public boolean isResponse(Object body) {
        return body instanceof R;
    }

    @Override
    public String getCode(Object body) {
        return body instanceof R<?> r ? r.getCode() : null;
    }

    @Override
    public void setMessage(Object body, String message) {
        if (body instanceof R<?> r) {
            r.setMsg(message);
        }
    }
}
