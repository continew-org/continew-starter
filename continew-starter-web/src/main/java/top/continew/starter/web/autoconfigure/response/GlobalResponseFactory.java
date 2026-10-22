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

import org.jspecify.annotations.Nullable;

/**
 * 全局响应工厂
 * <p>
 * 负责创建成功与失败响应对象，并提供识别、读取响应码、改写响应信息的契约。默认实现返回
 * {@code top.continew.starter.web.model.R}，需要自定义响应结构时声明一个本接口的 Bean 即可替换，
 * 无需修改 Starter 代码。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public interface GlobalResponseFactory {

    /**
     * 创建成功响应
     *
     * @param data 响应数据，可为 {@code null}
     * @return 响应对象
     */
    Object newSuccess(@Nullable Object data);

    /**
     * 创建失败响应
     *
     * @param code 响应码
     * @param msg  响应信息
     * @return 响应对象
     */
    Object newFailure(String code, String msg);

    /**
     * 是否为响应对象
     *
     * @param body 响应体
     * @return 是否为响应对象
     */
    boolean isResponse(Object body);

    /**
     * 获取响应码
     *
     * @param body 响应对象
     * @return 响应码，无法获取时返回 {@code null}
     */
    @Nullable
    String getCode(Object body);

    /**
     * 改写响应信息
     *
     * @param body    响应对象
     * @param message 新的响应信息
     */
    void setMessage(Object body, String message);
}
