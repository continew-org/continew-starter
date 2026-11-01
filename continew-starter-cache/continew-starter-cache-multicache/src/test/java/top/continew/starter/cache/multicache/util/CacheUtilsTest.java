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

package top.continew.starter.cache.multicache.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import top.continew.starter.cache.multicache.autoconfigure.MultiLevelCacheProperties;
import top.continew.starter.cache.multicache.cache.FakeRedisCacheWriter;
import top.continew.starter.cache.multicache.cache.MultiLevelCacheManager;

import java.util.List;

/**
 * CacheUtils 单元测试（evictAll 未注册 cacheName 的 WARN 提示与既有行为守护）
 *
 * @author Charles7c
 * @since 3.0.0
 */
class CacheUtilsTest {

    private static final String CACHE_NAME = "TEST:";

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        FakeRedisCacheWriter cacheWriter = new FakeRedisCacheWriter();
        MultiLevelCacheManager manager = new MultiLevelCacheManager(cacheWriter,
            RedisCacheConfiguration.defaultCacheConfig(), new MultiLevelCacheProperties(), null,
            List.of(CACHE_NAME));
        manager.afterPropertiesSet();
        CacheUtils.setCacheManager(manager);
        Logger logger = (Logger) LoggerFactory.getLogger(CacheUtils.class);
        logAppender.setContext(logger.getLoggerContext());
        logAppender.start();
        logAppender.list.clear();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        CacheUtils.setCacheManager(null);
        ((Logger) LoggerFactory.getLogger(CacheUtils.class)).detachAppender(logAppender);
    }

    @Test
    @DisplayName("evictAll 未注册 cacheName 输出 WARN 并保持既有失败行为")
    void testEvictAllUnregisteredCacheNameWarns() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> CacheUtils.evictAll("UNREGISTERED"));
        assertEquals("Cache 'UNREGISTERED' is not registered.", exception.getMessage());
        assertEquals(1, logAppender.list.size());
        ILoggingEvent event = logAppender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("UNREGISTERED"), "WARN 日志应包含 cacheName");
        assertTrue(message.contains("not registered"), "WARN 日志应包含未注册提示");
    }

    @Test
    @DisplayName("evictAll 已注册 cacheName 正常清空，不输出 WARN")
    void testEvictAllRegisteredCacheNameClears() {
        CacheUtils.put(CACHE_NAME, "k1", "v1");
        assertEquals("v1", CacheUtils.get(CACHE_NAME, "k1"));
        CacheUtils.evictAll(CACHE_NAME);
        assertNull(CacheUtils.get(CACHE_NAME, "k1"));
        assertEquals(0, logAppender.list.size());
    }

    @Test
    @DisplayName("evictAll 动态模式未预注册的 cacheName 仍按动态缓存清空")
    void testEvictAllDynamicCacheNameClears() {
        FakeRedisCacheWriter cacheWriter = new FakeRedisCacheWriter();
        MultiLevelCacheManager manager = new MultiLevelCacheManager(cacheWriter,
            RedisCacheConfiguration.defaultCacheConfig(), new MultiLevelCacheProperties(), null,
            null);
        manager.afterPropertiesSet();
        CacheUtils.setCacheManager(manager);
        CacheUtils.put(CACHE_NAME, "k1", "v1");
        CacheUtils.evictAll(CACHE_NAME);
        assertNull(CacheUtils.get(CACHE_NAME, "k1"));
        assertNotNull(manager.getCache(CACHE_NAME));
        assertEquals(0, logAppender.list.size());
    }
}
