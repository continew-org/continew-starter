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

import org.springframework.beans.factory.InitializingBean;
import top.continew.starter.web.autoconfigure.response.GlobalResponseProperties;

/**
 * 响应默认状态初始化器
 * <p>
 * 在容器启动时把配置默认值写入 {@link R} 的静态状态，避免依赖默认响应工厂的创建时机；
 * 使用自定义 {@code GlobalResponseFactory} 时同样生效。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class ResponseConfigInitializer implements InitializingBean {

    private final GlobalResponseProperties properties;

    /**
     * 构造方法
     *
     * @param properties 全局响应配置属性
     */
    public ResponseConfigInitializer(GlobalResponseProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        ResponseConfig.configure(properties.getDefaultSuccessCode(), properties
            .getDefaultSuccessMsg(), properties.getDefaultErrorCode(),
            properties.getDefaultErrorMsg());
    }
}
