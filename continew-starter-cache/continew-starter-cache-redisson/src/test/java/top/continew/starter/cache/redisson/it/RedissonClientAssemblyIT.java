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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationV4;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import top.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration;

import java.time.Duration;
import java.util.List;

/**
 * Redisson 客户端真实装配集成测试。
 *
 * <p>按生产链路（DataRedis 属性 + Redisson V4 自动配置 + 本模块定制器）装配出
 * {@code RedissonClient}，对 Testcontainers 拉起的真实 Redis 完成读写往返，
 * 验证单测无法覆盖的装配连通性与 NameMapper 真实键名映射。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("Redisson 客户端真实装配集成测试")
class RedissonClientAssemblyIT extends RedisContainerSupport {

    @Test
    @DisplayName("自动配置装配的客户端连通真实 Redis，生产 codec 对象值往返携带 @class")
    void shouldAssembleConnectedClientWithTypedCodecRoundTrip() {
        assemblyRunner().run(context -> {
            RedissonClient redisson = context.getBean(RedissonClient.class);
            RBucket<ItestPayload> bucket = redisson.getBucket("itest:assembly:typed");
            bucket.set(new ItestPayload(), Duration.ofSeconds(60));
            ItestPayload loaded = assertInstanceOf(ItestPayload.class, bucket.get());
            assertEquals("alice", loaded.getUsername());
            assertEquals(List.of("admin", "user"), loaded.getRoles());
            // 以原始字节读取真实落库内容，验证生产构造的 codec 写入了多态类型标记
            RBucket<String> rawBucket = client().getBucket("itest:assembly:typed",
                StringCodec.INSTANCE);
            String rawValue = rawBucket.get();
            assertTrue(rawValue.contains("\"@class\""), rawValue);
        });
    }

    @Test
    @DisplayName("keyPrefix 配置经 NameMapper 映射后，真实 Redis 键名携带前缀")
    void shouldMapRealKeyWithConfiguredKeyPrefix() {
        assemblyRunner("spring.data.redisson.key-prefix=itest:prefix:").run(context -> {
            RedissonClient redisson = context.getBean(RedissonClient.class);
            redisson.getBucket("assembly:key-prefix").set("v", Duration.ofSeconds(60));
        });
        assertTrue(client().getBucket("itest:prefix:assembly:key-prefix").isExists());
        assertFalse(client().getBucket("assembly:key-prefix").isExists());
    }

    /**
     * 构建按生产链路装配的上下文运行器。
     *
     * @param extraProperties 额外配置属性
     * @return 指向共享容器的运行器
     */
    private ApplicationContextRunner assemblyRunner(String... extraProperties) {
        // ObjectMapper 直接注册最小实例：生产由 spring-boot-jackson 提供，测试侧用等价的
        // JsonMapper 免去额外 test 依赖
        return new ApplicationContextRunner().withBean(ObjectMapper.class,
            () -> JsonMapper.builder().build())
            .withPropertyValues("spring.data.redis.host=" + redisHost(), "spring.data.redis.port="
                + redisPort())
            .withPropertyValues(extraProperties)
            .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class,
                RedissonAutoConfigurationV4.class, RedissonAutoConfiguration.class));
    }

    /**
     * 模拟业务缓存对象的样本模型
     */
    static class ItestPayload {

        private String username = "alice";
        private List<String> roles = List.of("admin", "user");

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
