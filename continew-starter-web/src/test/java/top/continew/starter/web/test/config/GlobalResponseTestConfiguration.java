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

package top.continew.starter.web.test.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.continew.starter.web.autoconfigure.response.ExceptionAliasRegister;
import top.continew.starter.web.model.R;
import top.continew.starter.web.test.exception.AliasedException;

/**
 * 全局响应测试配置
 *
 * @author Charles7c
 * @since 3.0.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class GlobalResponseTestConfiguration {

    /**
     * 注册异常别名
     *
     * @return 异常别名注册器
     */
    @Bean
    @Primary
    public ExceptionAliasRegister testExceptionAliasRegister() {
        return new ExceptionAliasRegister().doRegisterExceptionAlias(AliasedException.class);
    }

    /**
     * 业务方自定义异常处理器
     *
     * @return 自定义异常处理器
     */
    @Bean
    public CustomExceptionHandler customExceptionHandler() {
        return new CustomExceptionHandler();
    }

    @RestControllerAdvice
    @Order(0)
    static class CustomExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public R handleIllegalArgumentException(IllegalArgumentException e) {
            return R.fail("custom-code", e.getMessage());
        }
    }
}
