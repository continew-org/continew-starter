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

package top.continew.starter.messaging.mqtt.it;

import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.test.support.ContainerImages;

/**
 * MQTT 集成测试基座：整个 Failsafe JVM 共享一个真实 HiveMQ 容器。
 *
 * <p>容器启动采用幂等懒启动，由首个 Spring 上下文装配（@DynamicPropertySource）时触发，
 * 保证 Docker 缺失时类级条件跳过不会误启动容器；容器与被测上下文在整个 JVM 内保持
 * 常驻，由 JVM 退出时的 Ryuk 守护进程统一回收，各测试类以独立 topic 前缀隔离。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
abstract class HivemqContainerSupport {

    private static final HiveMQContainer HIVEMQ = new HiveMQContainer(
        DockerImageName.parse(ContainerImages.HIVEMQ_IMAGE));

    private static volatile boolean started;

    /**
     * 启动共享的真实 HiveMQ 容器（幂等）。
     */
    static void ensureStarted() {
        if (!started) {
            synchronized (HivemqContainerSupport.class) {
                if (!started) {
                    HIVEMQ.start();
                    started = true;
                }
            }
        }
    }

    /**
     * 被测 MQTT 服务地址（tcp://，指向容器映射端口）。
     *
     * @return broker 地址
     */
    static String brokerUrl() {
        return "tcp://" + HIVEMQ.getHost() + ":" + HIVEMQ.getMqttPort();
    }
}
