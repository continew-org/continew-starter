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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 默认异常处理前回调实现
 *
 * @author Charles7c
 * @since 2.6.0
 */
public class DefaultBeforeControllerAdviceProcess implements BeforeControllerAdviceProcess {

    private static final Logger LOGGER = LoggerFactory
        .getLogger(DefaultBeforeControllerAdviceProcess.class);

    private final GlobalResponseProperties properties;

    /**
     * 构造方法
     *
     * @param properties 全局响应配置属性
     */
    public DefaultBeforeControllerAdviceProcess(GlobalResponseProperties properties) {
        this.properties = properties;
    }

    @Override
    public void call(HttpServletRequest request,
        @Nullable HttpServletResponse response,
        @Nullable Object handler,
        Exception e) {
        if (properties.isPrintExceptionInGlobalAdvice()) {
            LOGGER.error("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        }
    }
}
