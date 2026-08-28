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

package top.continew.starter.core.constant;

import org.springframework.core.Ordered;

/**
 * 过滤器和拦截器相关顺序常量
 *
 * @author Charles7c
 * @since 2.13.3
 */
public class OrderedConstants {

    /**
     * 过滤器顺序
     */
    public static final class Filter {

        /**
         * API 加密过滤器顺序
         */
        public static final int API_ENCRYPT_FILTER = Ordered.HIGHEST_PRECEDENCE;

        /**
         * 链路追踪过滤器顺序
         */
        public static final int TRACE_FILTER = Ordered.HIGHEST_PRECEDENCE + 100;

        /**
         * XSS 过滤器顺序
         */
        public static final int XSS_FILTER = Ordered.HIGHEST_PRECEDENCE + 200;

        /**
         * 日志过滤器顺序
         */
        public static final int LOG_FILTER = Ordered.LOWEST_PRECEDENCE - 100;

        private Filter() {
        }
    }

    /**
     * 拦截器顺序
     */
    public static final class Interceptor {

        /**
         * 租户拦截器顺序
         */
        public static final int TENANT_INTERCEPTOR = Ordered.HIGHEST_PRECEDENCE + 100;

        /**
         * 认证拦截器顺序
         */
        public static final int AUTH_INTERCEPTOR = Ordered.HIGHEST_PRECEDENCE + 200;

        /**
         * 日志拦截器顺序
         */
        public static final int LOG_INTERCEPTOR = Ordered.LOWEST_PRECEDENCE - 100;

        private Interceptor() {
        }
    }

    private OrderedConstants() {
    }
}
