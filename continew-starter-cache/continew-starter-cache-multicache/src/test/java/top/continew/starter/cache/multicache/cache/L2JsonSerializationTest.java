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

package top.continew.starter.cache.multicache.cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.json.JsonMapper;
import top.continew.starter.cache.multicache.autoconfigure.MultiLevelCacheProperties;
import top.continew.starter.cache.springcache.autoconfigure.SpringCacheAutoConfiguration;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * L2 JSON 序列化往返测试（不可变集合、Map 与 Long 根值）
 *
 * <p>
 * 守护两类适配阻塞的修复：不可变集合（{@code List.of()} / {@code Stream.toList()} 产物）
 * 与 int 范围 {@code Long} 作为缓存根值经 L2 JSON 序列化往返后类型丢失或反序列化失败，
 * 以及不可变 Map 根值类型 id 混入内容造成静默污染。
 * 序列化配置直接取自 {@link SpringCacheAutoConfiguration} 生产 Bean，防止生产构造与测试构造漂移。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
class L2JsonSerializationTest {

    private static final String CACHE_NAME = "TEST:";

    private RedisCacheConfiguration productionConfiguration;
    private FakeRedisCacheWriter cacheWriter;

    @BeforeEach
    void setUp() {
        productionConfiguration = newProductionRedisCacheConfiguration();
    }

    /**
     * 生产 Bean 的 L2 序列化配置（与 SpringCacheAutoConfiguration 保持一致）
     *
     * @return Redis 缓存配置
     */
    private static RedisCacheConfiguration newProductionRedisCacheConfiguration() {
        return new SpringCacheAutoConfiguration(JsonMapper.builder()
            .build()).redisCacheConfiguration(new CacheProperties());
    }

    /**
     * 生产配置的值序列化对（序列化器层往返入口）
     *
     * @return 值序列化对
     */
    private static RedisSerializationContext.SerializationPair<Object> valuePair() {
        return newProductionRedisCacheConfiguration().getValueSerializationPair();
    }

    private Cache newCache() {
        cacheWriter = new FakeRedisCacheWriter();
        MultiLevelCacheManager manager = new MultiLevelCacheManager(cacheWriter,
            productionConfiguration, new MultiLevelCacheProperties(), null, List.of(CACHE_NAME));
        manager.afterPropertiesSet();
        return manager.getCache(CACHE_NAME);
    }

    /**
     * 以同一 L2 存储构建另一"节点"的管理器（L1 为空，读取必经 L2）
     *
     * @return 另一节点的多级缓存管理器
     */
    private MultiLevelCacheManager otherNodeManager() {
        MultiLevelCacheManager otherManager = new MultiLevelCacheManager(cacheWriter,
            productionConfiguration, new MultiLevelCacheProperties(), null, List.of(CACHE_NAME));
        otherManager.afterPropertiesSet();
        return otherManager;
    }

    @Test
    @DisplayName("不可变集合（List.of）经序列化器往返类型保持")
    void testListOfRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(List
            .of("admin", "user"))));
        assertEquals(List.of("admin", "user"), decoded);
    }

    @Test
    @DisplayName("不可变集合（Stream.toList）经序列化器往返类型保持")
    void testStreamToListRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(Stream
            .of("admin", "user")
            .toList())));
        assertEquals(List.of("admin", "user"), decoded);
    }

    @Test
    @DisplayName("不可变 Map（Map.of）经序列化器往返内容与容量保持")
    void testMapOfRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(Map
            .of("k", "v"))));
        assertEquals(Map.of("k", "v"), decoded, "读回内容必须与原值一致");
        assertEquals(1, ((Map<?, ?>) decoded).size(), "类型 id 不得混入 Map 内容");
    }

    @Test
    @DisplayName("不可变 Map（Map.copyOf）经序列化器往返内容与容量保持")
    void testMapCopyOfRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(Map
            .copyOf(Map.of("k", "v")))));
        assertEquals(Map.of("k", "v"), decoded);
        assertEquals(1, ((Map<?, ?>) decoded).size());
    }

    @Test
    @DisplayName("不可变 Map（unmodifiableMap）经序列化器往返内容与容量保持")
    void testUnmodifiableMapRoundTripOnSerializer() {
        Object decoded =
            assertDoesNotThrow(() -> valuePair().read(valuePair().write(java.util.Collections
                .unmodifiableMap(new java.util.HashMap<>(Map.of("k", "v"))))));
        assertEquals(Map.of("k", "v"), decoded);
        assertEquals(1, ((Map<?, ?>) decoded).size());
    }

    @Test
    @DisplayName("多条目不可变 Map（Map.of）经序列化器往返内容保持")
    void testMultiEntryMapOfRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(Map
            .of("a", "1", "b", "2"))));
        assertEquals(Map.of("a", "1", "b", "2"), decoded);
        assertEquals(2, ((Map<?, ?>) decoded).size());
    }

    @Test
    @DisplayName("混合值不可变 Map 经序列化器往返内容与嵌套类型保持")
    void testMixedValueMapOfRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(Map
            .of("n", 123L, "l", List.of("x")))));
        assertEquals(Map.of("n", 123L, "l", List.of("x")), decoded);
        assertInstanceOf(Long.class, ((Map<?, ?>) decoded).get("n"));
    }

    @Test
    @DisplayName("空不可变 Map（Map.of）经序列化器往返内容保持")
    void testEmptyMapOfRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(Map.of())));
        assertEquals(Map.of(), decoded);
    }

    @Test
    @DisplayName("用户自定义 final Map 子类往返保持原类型与条目")
    void testUserFinalMapSubclassRoundTripOnSerializer() {
        BizFinalMap map = new BizFinalMap();
        map.put("k", "v");
        map.put("bizTag", "tag");
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(map)));
        BizFinalMap finalMap = assertInstanceOf(BizFinalMap.class, decoded, "读回必须保持原实现类型");
        assertEquals("v", finalMap.get("k"), "entry 内容必须保持");
        assertEquals("tag", finalMap.get("bizTag"), "业务条目必须保持");
        assertEquals(0, sizeOfBeanProperties(finalMap), "类型 id 不得混入条目");
    }

    /**
     * 统计 Map 中混入的属性样条目（非业务数据），用于断言类型 id 未污染内容
     *
     * @param map 读回的 Map
     * @return 混入条目数
     */
    private static int sizeOfBeanProperties(BizFinalMap map) {
        return (int) map.keySet()
            .stream()
            .filter(key -> !"k".equals(key) && !"bizTag".equals(key))
            .count();
    }

    /**
     * 用户自定义 final Map 子类（模拟业务扩展 Map；Jackson 对 Map 子类的 getter 属性
     * 不参与序列化为既有语义，业务数据以条目承载）
     */
    static final class BizFinalMap extends LinkedHashMap<String, Object> {

        private String bizTag;

        public BizFinalMap() {
            // Jackson 反序列化该 Map 子类依赖默认构造器；显式声明，避免未来新增带参构造器后
            // 默认构造器被隐式移除，导致序列化静默失效
        }

        public String getBizTag() {
            return bizTag;
        }

        public void setBizTag(String bizTag) {
            this.bizTag = bizTag;
        }
    }

    @Test
    @DisplayName("int 范围 Long 经序列化器往返保持 Long 类型")
    void testLongRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(123L)));
        assertInstanceOf(Long.class, decoded);
        assertEquals(123L, decoded);
    }

    @Test
    @DisplayName("不可变集合与 Long 混合结构经序列化器往返类型保持")
    void testNestedImmutableValuesRoundTripOnSerializer() {
        Object decoded = assertDoesNotThrow(() -> valuePair().read(valuePair().write(List
            .of(123L, "admin"))));
        assertEquals(List.of(123L, "admin"), decoded);
        assertInstanceOf(Long.class, ((List<?>) decoded).get(0));
    }

    @Test
    @DisplayName("空值（NullValue）经序列化器往返保持已缓存语义")
    void testNullValueRoundTripOnSerializer() {
        ByteBuffer bytes = assertDoesNotThrow(() -> valuePair().write(NullValue.INSTANCE));
        Object decoded = assertDoesNotThrow(() -> valuePair().read(bytes));
        assertInstanceOf(NullValue.class, decoded);
    }

    @Test
    @DisplayName("不可变集合经 L2 完整链路往返（写入后读回）")
    void testImmutableListRoundTripOnL2() {
        Cache cache = newCache();
        cache.put("dict", Stream.of("admin", "user").toList());
        // 另一"节点"（L1 为空）从 L2 读回
        Object decoded = otherNodeManager().getCache(CACHE_NAME).get("dict").get();
        assertEquals(List.of("admin", "user"), decoded);
    }

    @Test
    @DisplayName("不可变 Map 经 L2 完整链路往返内容保持")
    void testMapRoundTripOnL2() {
        Cache cache = newCache();
        cache.put("dict", Map.of("k", "v"));
        Object decoded = otherNodeManager().getCache(CACHE_NAME).get("dict").get();
        assertEquals(Map.of("k", "v"), decoded, "L2 读回内容必须与原值一致");
        assertEquals(1, ((Map<?, ?>) decoded).size(), "类型 id 不得混入 Map 内容");
    }

    @Test
    @DisplayName("int 范围 Long 经 L2 完整链路往返保持 Long 类型")
    void testLongRoundTripOnL2() {
        Cache cache = newCache();
        cache.put("tenantId", 123L);
        Object decoded = otherNodeManager().getCache(CACHE_NAME).get("tenantId").get();
        assertInstanceOf(Long.class, decoded);
        assertEquals(123L, decoded);
    }
}
