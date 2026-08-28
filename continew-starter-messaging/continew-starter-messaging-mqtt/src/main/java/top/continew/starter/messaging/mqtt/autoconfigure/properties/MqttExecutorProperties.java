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

package top.continew.starter.messaging.mqtt.autoconfigure.properties;

/**
 * 连接池属性
 *
 * @author echo
 * @since 2.15.0
 */
public class MqttExecutorProperties {

    /**
     * 线程池核心线程数，表示即使线程处于空闲状态，也会保留的最小线程数。
     * 通常设置为常规负载下的并发处理数量，默认值为 5。
     */
    private Integer corePoolSize = 5;

    /**
     * 线程池最大线程数，表示线程池允许创建的最大线程数量。
     * 超过 corePoolSize 后，如果任务队列已满，会继续创建线程直到该值。
     * 默认值为 10。
     */
    private Integer maxPoolSize = 10;

    /**
     * 线程池中线程最大空闲时间（单位：秒）。
     * 当线程数量超过 corePoolSize 且处于空闲状态超过该时间时会被销毁。
     * 默认值为 60 秒。
     */
    private Integer keepAliveSeconds = 60;

    /**
     * 线程池的任务队列容量。
     * 用于缓冲提交但尚未执行的任务，超过该容量时新任务会触发拒绝策略。
     * 默认值为 512。
     */
    private Integer queueCapacity = 512;

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

    public Integer getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(Integer keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public Integer getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(Integer queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
