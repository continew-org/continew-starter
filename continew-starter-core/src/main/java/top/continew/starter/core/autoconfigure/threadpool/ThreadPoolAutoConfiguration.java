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

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.ExceptionUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池自动配置
 *
 * @author Charles7c
 * @author Lion Li（<a href="https://gitee.com/dromara/RuoYi-Vue-Plus">RuoYi-Vue-Plus</a>）
 * @since 1.0.0
 */
@Lazy
@AutoConfiguration
@ConditionalOnProperty(prefix = PropertiesConstants.THREAD_POOL, name = PropertiesConstants.ENABLED, havingValue = "true")
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolAutoConfiguration.class);

    /**
     * 核心（最小）线程数 = CPU 核心数 + 1
     */
    private final int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;

    /**
     * Spring 内置线程池：ThreadPoolTaskExecutor
     */
    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(ThreadPoolProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("thread-pool");
        // 核心（最小）线程数
        executor.setCorePoolSize(ObjectUtil.defaultIfNull(properties.getCorePoolSize(), corePoolSize));
        // 最大线程数
        executor.setMaxPoolSize(ObjectUtil.defaultIfNull(properties.getMaxPoolSize(), corePoolSize * 2));
        // 队列容量
        executor.setQueueCapacity(properties.getQueueCapacity());
        // 活跃时间
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        // 配置当池内线程数已达到上限的时候，该如何处理新任务：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 关闭线程池是否等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(properties.isWaitForTasksToCompleteOnShutdown());
        // 执行器在关闭时阻塞的最长毫秒数，以等待剩余任务完成执行
        executor.setAwaitTerminationMillis(properties.getAwaitTerminationMillis());
        log.debug("[ContiNew Starter] - Auto Configuration 'ThreadPoolTaskExecutor' completed initialization.");
        return executor;
    }

    /**
     * Java 内置线程池：ScheduledExecutorService（适用于执行周期性或定时任务）
     */
    @Bean
    @ConditionalOnMissingBean
    public ScheduledExecutorService scheduledExecutorService(ThreadPoolProperties properties) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(ObjectUtil.defaultIfNull(properties
            .getCorePoolSize(), corePoolSize), ThreadUtil
                .newNamedThreadFactory("schedule-pool-%d", true), new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable runnable, Throwable throwable) {
                super.afterExecute(runnable, throwable);
                ExceptionUtils.printException(runnable, throwable);
            }
        };
        // 应用关闭时，关闭线程池
        SpringApplication.getShutdownHandlers().add(() -> this.shutdown(executor, properties));
        log.debug("[ContiNew Starter] - Auto Configuration 'ScheduledExecutorService' completed initialization.");
        return executor;
    }

    /**
     * 根据相应的配置设置关闭 ExecutorService
     *
     * @see org.springframework.scheduling.concurrent.ExecutorConfigurationSupport#shutdown()
     * @since 2.0.0
     */
    public void shutdown(ExecutorService executor, ThreadPoolProperties properties) {
        log.debug("[ContiNew Starter] - Shutting down ScheduledExecutorService start.");
        if (executor != null) {
            if (properties.isWaitForTasksToCompleteOnShutdown()) {
                executor.shutdown();
            } else {
                for (Runnable remainingTask : executor.shutdownNow()) {
                    cancelRemainingTask(remainingTask);
                }
            }
            awaitTerminationIfNecessary(executor, properties);
            log.debug("[ContiNew Starter] - Shutting down ScheduledExecutorService complete.");
        }
    }

    /**
     * Cancel the given remaining task which never commenced execution,
     * as returned from {@link ExecutorService#shutdownNow()}.
     *
     * @param task the task to cancel (typically a {@link RunnableFuture})
     * @see RunnableFuture#cancel(boolean)
     * @since 2.0.0
     */
    protected void cancelRemainingTask(Runnable task) {
        if (task instanceof Future<?> future) {
            future.cancel(true);
        }
    }

    /**
     * Wait for the executor to terminate, according to the value of the properties
     * 
     * @since 2.0.0
     */
    private void awaitTerminationIfNecessary(ExecutorService executor, ThreadPoolProperties properties) {
        if (properties.getAwaitTerminationMillis() > 0) {
            try {
                if (!executor.awaitTermination(properties.getAwaitTerminationMillis(), TimeUnit.MILLISECONDS)) {
                    if (log.isWarnEnabled()) {
                        log.warn("[ContiNew Starter] - Timed out while waiting for  executor 'ScheduledExecutorService' to terminate.");
                    }
                }
            } catch (InterruptedException ex) {
                if (log.isWarnEnabled()) {
                    log.warn("[ContiNew Starter] - Interrupted while waiting for executor 'ScheduledExecutorService' to terminate");
                }
                Thread.currentThread().interrupt();
            }
        }
    }
}
