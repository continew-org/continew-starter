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

package top.continew.starter.auth.refresh.token.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.auth.refresh.token.api.AuthSessionRevocationNotifier;
import top.continew.starter.auth.refresh.token.autoconfigure.RefreshTokenProperties;
import top.continew.starter.auth.refresh.token.enums.RefreshTokenModeEnum;
import top.continew.starter.auth.refresh.token.model.RefreshClientPolicy;
import top.continew.starter.auth.refresh.token.model.RefreshSessionPrincipal;
import top.continew.starter.auth.refresh.token.service.impl.RefreshTokenServiceImpl;
import top.continew.starter.auth.refresh.token.support.AccessSessionCache;
import top.continew.starter.auth.refresh.token.support.RefreshSessionStore;
import top.continew.starter.auth.refresh.token.support.RefreshTokenCodec;
import top.continew.starter.auth.refresh.token.support.RefreshTokenRequestGuard;
import top.continew.starter.test.support.ContainerImages;

import java.util.List;

/**
 * Refresh Token 模块集成测试基座：整个 Failsafe JVM 共享一个真实 Redis 容器与客户端，
 * 并按生产构造组装真实的会话仓储、编解码器、请求守卫与服务实现。
 *
 * <p>{@code RefreshSessionStore} 同时经注入的客户端（锁、索引、安全版本）与
 * {@code RedisUtils} 静态客户端（会话数据、轮换快照）访问 Redis，后者在 JVM 存续期内
 * 只在类首次加载时绑定，因此容器、客户端与 SpringUtil 上下文必须跨测试类共享，
 * 容器由 JVM 退出时的 Ryuk 守护进程统一回收。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
abstract class RefreshTokenRedisSupport {

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
        DockerImageName.parse(ContainerImages.REDIS_IMAGE)).withExposedPorts(6379);
    private static volatile RedissonClient sharedClient;

    /**
     * 启动共享的真实 Redis 容器，并建立 StringCodec 客户端与最小 Spring 上下文。
     *
     * <p>统一 StringCodec：会话 JSON、轮换快照与失效原因按原始文本落库，断言方能以
     * 原样字节读取真实 Redis 状态；原子计数与分布式锁由 Redisson 内部编解码器处理，
     * 不受客户端 codec 影响。</p>
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
     * 按原始文本读取真实 Redis 键值。
     *
     * @param key 真实键名
     * @return 键值文本，不存在时返回 {@code null}
     */
    static String rawValue(String key) {
        RBucket<String> bucket = client().getBucket(key, StringCodec.INSTANCE);
        return bucket.get();
    }

    /**
     * 构建集成测试用的 Refresh Token 配置。
     *
     * @param rotationGracePeriod 轮换宽限期（秒）
     * @return 关闭 IP 限流与本地缓存、指向测试密钥的配置
     */
    static RefreshTokenProperties testProperties(int rotationGracePeriod) {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setSecret("test-only-refresh-token-secret-with-32-bytes");
        properties.setIpRateLimit(0);
        properties.setSessionRateLimit(10);
        properties.setRotationGracePeriod(rotationGracePeriod);
        // 关闭本地缓存，使会话校验逐请求回源真实 Redis；同时规避未初始化的 Caffeine 缓存
        properties.setAccessSessionCacheEnabled(false);
        return properties;
    }

    /**
     * 按生产构造组装真实的 Refresh Token 服务。
     *
     * @param properties Refresh Token 配置
     * @param notifiers  会话撤销通知器列表
     * @return 连接真实 Redis 的服务实现
     */
    static RefreshTokenServiceImpl newService(RefreshTokenProperties properties,
        List<AuthSessionRevocationNotifier> notifiers) {
        RefreshTokenCodec codec = new RefreshTokenCodec(properties);
        RefreshSessionStore sessionStore = new RefreshSessionStore(client());
        RefreshTokenRequestGuard requestGuard = new RefreshTokenRequestGuard(properties, codec,
            sessionStore);
        AccessSessionCache accessSessionCache = new AccessSessionCache(properties, client(),
            "it-refresh-token");
        return new RefreshTokenServiceImpl(properties, codec, sessionStore, requestGuard, notifiers,
            accessSessionCache);
    }

    /**
     * 构建 BODY 模式的客户端令牌策略：Refresh Token 有效期 600 秒，允许并发登录。
     *
     * @param clientId 客户端 ID
     * @return BODY 模式客户端策略
     */
    static RefreshClientPolicy bodyPolicy(String clientId) {
        return new RefreshClientPolicy(clientId, "web", 600, RefreshTokenModeEnum.BODY, true, null,
            -1, null);
    }

    /**
     * 轮询等待指定真实键按 TTL 过期：每轮循环一次真实 EXISTS 往返构成间隔，
     * 超时后由终态断言兜底失败。
     *
     * @param key 真实键名
     */
    static void awaitKeyExpiry(String key) {
        long deadline = System.currentTimeMillis() + 5000;
        RBucket<String> bucket = client().getBucket(key);
        boolean exists = bucket.isExists();
        while (exists && System.currentTimeMillis() < deadline) {
            exists = bucket.isExists();
        }
        assertFalse(exists, "键 " + key + " 应已按 TTL 真实过期");
    }

    /**
     * 断言管理索引包含指定会话。
     *
     * @param key       索引真实键名
     * @param sessionId 会话 ID
     */
    static void assertIndexContains(String key, String sessionId) {
        assertTrue(client().getScoredSortedSet(key, StringCodec.INSTANCE).contains(sessionId),
            "索引 " + key + " 应包含会话 " + sessionId);
    }

    /**
     * 断言管理索引不包含指定会话。
     *
     * @param key       索引真实键名
     * @param sessionId 会话 ID
     */
    static void assertIndexNotContains(String key, String sessionId) {
        assertFalse(client().getScoredSortedSet(key, StringCodec.INSTANCE).contains(sessionId),
            "索引 " + key + " 不应包含会话 " + sessionId);
    }

    /**
     * 测试主体快照：昵称等展示字段直接复用用户名，仅覆盖接口必需的读取契约。
     */
    static final class ItPrincipal implements RefreshSessionPrincipal {

        private final Long userId;
        private final String username;
        private final Long tenantId;

        ItPrincipal(Long userId, String username) {
            this(userId, username, null);
        }

        ItPrincipal(Long userId, String username, Long tenantId) {
            this.userId = userId;
            this.username = username;
            this.tenantId = tenantId;
        }

        @Override
        public Long getUserId() {
            return userId;
        }

        @Override
        public String getUsername() {
            return displayName();
        }

        @Override
        public String getNickname() {
            return displayName();
        }

        /**
         * 展示名：昵称直接复用用户名，仅覆盖接口必需的读取契约。
         *
         * @return 用户名
         */
        private String displayName() {
            return username;
        }

        @Override
        public Long getTenantId() {
            return tenantId;
        }

        @Override
        public String getIp() {
            return "127.0.0.1";
        }

        @Override
        public String getAddress() {
            return "内网";
        }

        @Override
        public String getBrowser() {
            return "JUnit";
        }

        @Override
        public String getOs() {
            return "Linux";
        }
    }
}
