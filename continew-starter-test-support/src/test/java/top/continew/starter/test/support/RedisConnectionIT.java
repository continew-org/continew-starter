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

package top.continew.starter.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis 连接集成测试：验证 Testcontainers 基座可拉起真实 Redis 并完成协议交互
 *
 * <p>本机无 Docker 时由 {@link EnabledIfDockerAvailable} 自动跳过，CI 环境真实执行。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@Testcontainers
class RedisConnectionIT {

    @Container
    private static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse(ContainerImages.REDIS_IMAGE))
            .withExposedPorts(6379);

    /**
     * lettuce 连接容器映射端口后，PING 命令应返回 PONG
     */
    @Test
    void shouldPingRedisContainer() {
        RedisClient client =
            RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        StatefulRedisConnection<String, String> connection = client.connect();
        try {
            assertEquals("PONG", connection.sync().ping());
        } finally {
            connection.close();
            client.shutdown();
        }
    }
}
