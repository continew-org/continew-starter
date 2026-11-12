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

package top.continew.starter.cache.redisson.it;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.test.support.ContainerImages;

/**
 * Redisson 模块集成测试基座：整个 Failsafe JVM 共享一个真实 Redis 容器与客户端。
 *
 * <p>{@code RedisUtils} 在类首次加载时经 {@code SpringUtil} 静态绑定
 * {@code RedissonClient}，且该绑定在 JVM 存续期内不会更新；因此容器、客户端与
 * SpringUtil 上下文必须跨测试类共享，容器由 JVM 退出时的 Ryuk 守护进程统一回收。
 * 测试类只依赖 {@code @EnabledIfDockerAvailable} 门禁，无 Docker 时整体跳过。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
abstract class RedisContainerSupport {

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
        DockerImageName.parse(ContainerImages.REDIS_IMAGE)).withExposedPorts(6379);
    private static volatile RedissonClient sharedClient;

    /**
     * 启动共享的真实 Redis 容器，并建立 StringCodec 客户端与最小 Spring 上下文。
     *
     * <p>统一 StringCodec：RedisUtils 的键值按原始文本落库，断言方能以原样字节读取
     * 真实 Redis 状态；客户端本身不配置 NameMapper，可作为无前缀探针核对真实键名。</p>
     */
    @BeforeAll
    static void startSharedRedis() {
        if (sharedClient != null) {
            return;
        }
        REDIS.start();
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        config.setCodec(StringCodec.INSTANCE);
        RedissonClient client = Redisson.create(config);
        // RedisUtils/RedisLockUtils 经 SpringUtil 静态定位 RedissonClient，测试侧用最小上下文满足该约定
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(SpringUtil.class);
        context.registerBean(RedissonClient.class, () -> client);
        context.refresh();
        sharedClient = client;
    }

    /**
     * 获取共享的 Redisson 客户端。
     *
     * @return 与共享容器连接的客户端
     */
    static RedissonClient client() {
        return sharedClient;
    }

    /**
     * 获取共享容器的 Redis 主机。
     *
     * @return 容器映射后的主机地址
     */
    static String redisHost() {
        return REDIS.getHost();
    }

    /**
     * 获取共享容器的 Redis 映射端口。
     *
     * @return 容器映射后的端口
     */
    static Integer redisPort() {
        return REDIS.getMappedPort(6379);
    }
}
