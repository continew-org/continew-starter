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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.task.TaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * 线程池自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Lazy
@AutoConfiguration
@EnableConfigurationProperties(ThreadPoolExtensionProperties.class)
public class ThreadPoolAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolAutoConfiguration.class);

    /** 核心（最小）线程数 = CPU 核心数 + 1 */
    private final int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;

    /**
     * 异步任务线程池配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.task.execution.extension", name = PropertiesConstants.ENABLED, matchIfMissing = true)
    public TaskExecutorCustomizer taskExecutorCustomizer(ThreadPoolExtensionProperties properties) {
        return executor -> {
            if (executor.getMaxPoolSize() == Integer.MAX_VALUE) {
                // 核心（最小）线程数
                executor.setCorePoolSize(corePoolSize);
                // 最大线程数
                executor.setMaxPoolSize(corePoolSize * 2);
                // 队列容量
                executor.setQueueCapacity(executor.getMaxPoolSize());
            }
            // 当线程池的任务缓存队列已满并且线程池中的线程数已达到 maxPoolSize 时采取的任务拒绝策略
            executor.setRejectedExecutionHandler(properties.getExecution()
                .getRejectedPolicy()
                .getRejectedExecutionHandler());
            log.debug("[ContiNew Starter] - Auto Configuration 'TaskExecutor' completed initialization.");
        };
    }

    /**
     * 调度任务线程池配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.task.scheduling.extension", name = PropertiesConstants.ENABLED, matchIfMissing = true)
    public TaskSchedulerCustomizer taskSchedulerCustomizer(ThreadPoolExtensionProperties properties) {
        return executor -> {
            if (executor.getPoolSize() <= 1) {
                executor.setPoolSize(corePoolSize);
            }
            executor.setRejectedExecutionHandler(properties.getScheduling()
                .getRejectedPolicy()
                .getRejectedExecutionHandler());
            log.debug("[ContiNew Starter] - Auto Configuration 'TaskScheduler' completed initialization.");
        };
    }
}
