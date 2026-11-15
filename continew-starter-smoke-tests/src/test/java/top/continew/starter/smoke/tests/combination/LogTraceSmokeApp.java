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

package top.continew.starter.smoke.tests.combination;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.handler.LogHandler;
import top.continew.starter.log.model.AccessLogContext;
import top.continew.starter.log.model.LogRecord;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * log + trace 组合冒烟启动类：日志切面与链路追踪共存。
 *
 * <p>消费方提供自定义 LogHandler（{@code ConditionalOnMissingBean} 让位），
 * 同时规避 LogAutoConfiguration 构造器注入自身 @Bean 的装配死锁。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootApplication
public class LogTraceSmokeApp {

    /**
     * 冒烟端点：供 trace 贯通用例发起真实请求。
     */
    @RestController
    static class SmokeEndpoint {

        /**
         * 正常响应。
         *
         * @return 固定载荷
         */
        @GetMapping("/smoke/trace")
        public Map<String, String> ok() {
            return Map.of("value", "trace");
        }
    }

    /**
     * 日志处理器空实现。
     *
     * @return 日志处理器
     */
    @Bean
    public LogHandler logHandler() {
        return new LogHandler() {

            @Override
            public boolean isRecord(Method targetMethod, Class<?> targetClass) {
                return false;
            }

            @Override
            public LogRecord.Started start(Instant startTime, HttpServletRequest request) {
                return null;
            }

            @Override
            public LogRecord finish(LogRecord.Started started, Instant endTime,
                HttpServletResponse response, Set<Include> includes) {
                return null;
            }

            @Override
            public LogRecord finish(LogRecord.Started started, Instant endTime,
                HttpServletResponse response, Set<Include> includes, Method targetMethod,
                Class<?> targetClass) {
                return null;
            }

            @Override
            public void logDescription(LogRecord logRecord, Method targetMethod) {
            }

            @Override
            public void logModule(LogRecord logRecord, Method targetMethod, Class<?> targetClass) {
            }

            @Override
            public void accessLogStart(AccessLogContext accessLogContext) {
            }

            @Override
            public void accessLogFinish(AccessLogContext accessLogContext) {
            }

            @Override
            public Set<Include> getIncludes(Set<Include> includes, Method targetMethod,
                Class<?> targetClass) {
                return includes;
            }
        };
    }
}
