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

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * 线程池配置属性
 *
 * @author Charles7c
 * @author Lion Li（<a href="https://gitee.com/dromara/RuoYi-Vue-Plus">RuoYi-Vue-Plus</a>）
 * @since 1.0.0
 */
@ConfigurationProperties(PropertiesConstants.THREAD_POOL)
public class ThreadPoolProperties {

    /**
     * 是否启用线程池配置
     */
    private boolean enabled = false;

    /**
     * 核心/最小线程数（默认：CPU 核心数 + 1）
     */
    private Integer corePoolSize;

    /**
     * 最大线程数（默认：(CPU 核心数 + 1) * 2）
     */
    private Integer maxPoolSize;

    /**
     * 队列容量
     */
    private int queueCapacity = 128;

    /**
     * 活跃时间（单位：秒）
     */
    private int keepAliveSeconds = 300;

    /**
     * 关闭线程池是否等待任务完成
     */
    private boolean waitForTasksToCompleteOnShutdown = false;

    /**
     * 执行器在关闭时阻塞的最长毫秒数，以等待剩余任务完成执行
     */
    private long awaitTerminationMillis = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public boolean isWaitForTasksToCompleteOnShutdown() {
        return waitForTasksToCompleteOnShutdown;
    }

    public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
    }

    public long getAwaitTerminationMillis() {
        return awaitTerminationMillis;
    }

    public void setAwaitTerminationMillis(long awaitTerminationMillis) {
        this.awaitTerminationMillis = awaitTerminationMillis;
    }
}
