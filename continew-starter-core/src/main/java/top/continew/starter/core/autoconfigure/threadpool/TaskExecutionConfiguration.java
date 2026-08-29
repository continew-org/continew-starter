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

package top.continew.starter.core.autoconfigure.threadpool;

import cn.hutool.core.util.ArrayUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.exception.BaseException;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskExecutor}.
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.task.execution.extension",
    name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class TaskExecutionConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutionConfiguration.class);

    @Value("${spring.task.execution.pool.core-size:#{T(java.lang.Runtime).getRuntime().availableProcessors() + 1}}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:#{T(java.lang.Runtime).getRuntime().availableProcessors() * 2}}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:#{T(java.lang.Runtime).getRuntime().availableProcessors() * 200}}")
    private int queueCapacity;

    @Bean
    public ThreadPoolTaskExecutorCustomizer threadPoolTaskExecutorCustomizer(
        ThreadPoolExtensionProperties properties) {
        return executor -> {
            // 核心（最小）线程数
            executor.setCorePoolSize(corePoolSize);
            // 最大线程数
            executor.setMaxPoolSize(maxPoolSize);
            // 队列容量
            executor.setQueueCapacity(queueCapacity);
            // 当线程池的任务缓存队列已满并且线程池中的线程数已达到 maxPoolSize 时采取的任务拒绝策略
            executor.setRejectedExecutionHandler(properties.getExecution()
                .getRejectedPolicy()
                .getRejectedExecutionHandler());
        };
    }

    /**
     * 异步方法执行器
     *
     * @see org.springframework.scheduling.annotation.Async
     */
    @Bean
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    public AsyncConfigurer asyncConfigurer(BeanFactory beanFactory,
        ObjectProvider<AsyncUncaughtExceptionHandler> exceptionHandlerProvider) {

        return new AsyncConfigurer() {

            @Override
            public Executor getAsyncExecutor() {
                return beanFactory
                    .getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
                        Executor.class);
            }

            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                AsyncUncaughtExceptionHandler handler = exceptionHandlerProvider.getIfAvailable();
                return Objects.requireNonNullElseGet(handler,
                    () -> (throwable, method, objects) -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Exception message: ")
                            .append(throwable.getMessage())
                            .append(", Method name: ")
                            .append(method.getName());
                        if (ArrayUtil.isNotEmpty(objects)) {
                            sb.append(", Parameter value: ").append(Arrays.toString(objects));
                        }
                        throw new BaseException(sb.toString(), throwable);
                    });
            }
        };
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'TaskExecutor' completed initialization.");
    }
}
