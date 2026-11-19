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

package top.continew.starter.storage.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import top.continew.starter.storage.engine.StorageDecoratorManager.DecoratorRegisteredEvent;
import top.continew.starter.storage.engine.StorageDecoratorManager.DecoratorUnregisteredEvent;
import top.continew.starter.storage.strategy.StorageStrategy;
import top.continew.starter.storage.strategy.impl.LocalStorageStrategy;
import top.continew.starter.storage.strategy.impl.StorageStrategyDecorator;

import java.util.Map;

/**
 * 存储装饰器管理器单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("StorageDecoratorManager 单元测试")
class StorageDecoratorManagerTest {

    private ApplicationContext applicationContext;
    private StorageDecoratorManager manager;

    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(StorageStrategyDecorator.class))
            .thenReturn(Map.of());
        manager = new StorageDecoratorManager(applicationContext);
    }

    @Test
    @DisplayName("策略为 null 时返回 null")
    void shouldReturnNullForNullStrategy() {
        assertNull(manager.applyDecorators(null));
    }

    @Test
    @DisplayName("无装饰器时返回原策略实例")
    void shouldReturnOriginalStrategyWhenNoDecorators() {
        StorageStrategy strategy = mock(StorageStrategy.class);

        assertSame(strategy, manager.applyDecorators(strategy));
    }

    @Test
    @DisplayName("init 注册容器装饰器并按优先级构建装饰链")
    void shouldApplyDecoratorsFromContextInOrder() {
        StubDecorator low = new StubDecorator(20);
        StubDecorator high = new StubDecorator(5);
        when(applicationContext.getBeansOfType(StorageStrategyDecorator.class))
            .thenReturn(Map.of("low", low, "high", high));

        StorageStrategy strategy = mock(StorageStrategy.class);
        StorageStrategy decorated = manager.applyDecorators(strategy);

        assertEquals(low, decorated);
        assertEquals(high, low.currentDelegate());
        assertEquals(strategy, high.currentDelegate());
    }

    @Test
    @DisplayName("init 跳过目标类型为空的装饰器，且重复调用不重复初始化")
    void shouldSkipDecoratorWithoutTargetAndInitOnlyOnce() {
        StubDecorator noTarget = new StubDecorator(1) {

            @Override
            public Class<StorageStrategy> getTargetStrategyClass() {
                return null;
            }
        };
        StubDecorator valid = new StubDecorator(1);
        when(applicationContext.getBeansOfType(StorageStrategyDecorator.class))
            .thenReturn(Map.of("noTarget", noTarget, "valid", valid));

        StorageDecoratorManager localManager = new StorageDecoratorManager(applicationContext);
        localManager.init();
        localManager.init();

        StorageStrategy strategy = mock(StorageStrategy.class);
        StorageStrategy decorated = localManager.applyDecorators(strategy);
        assertEquals(valid, decorated);
        assertEquals(strategy, valid.currentDelegate());
    }

    @Test
    @DisplayName("通过 assignable 匹配父类装饰器")
    void shouldApplyDecoratorMatchingSuperclass() {
        StubDecorator decorator = new StubDecorator(1);
        when(applicationContext.getBeansOfType(StorageStrategyDecorator.class))
            .thenReturn(Map.of("d", decorator));

        StorageStrategy subclassStrategy = mock(LocalStorageStrategy.class);
        StorageStrategy decorated = manager.applyDecorators(subclassStrategy);

        assertEquals(decorator, decorated);
        assertEquals(subclassStrategy, decorator.currentDelegate());
    }

    @Test
    @DisplayName("动态注册与注销装饰器即时生效")
    void shouldRegisterAndUnregisterDecoratorDynamically() {
        StubDecorator decorator = new StubDecorator(1);
        StorageStrategy strategy = mock(StorageStrategy.class);

        manager.registerDecorator(decorator);
        assertEquals(decorator, manager.applyDecorators(strategy));

        manager.unregisterDecorator(decorator);
        assertSame(strategy, manager.applyDecorators(strategy));
    }

    @Test
    @DisplayName("目标类型为空的动态注册与注销不抛异常")
    void shouldIgnoreDynamicOperationWithNullTarget() {
        StubDecorator noTarget = new StubDecorator(1) {

            @Override
            public Class<StorageStrategy> getTargetStrategyClass() {
                return null;
            }
        };

        manager.registerDecorator(noTarget);
        manager.unregisterDecorator(noTarget);

        StorageStrategy strategy = mock(StorageStrategy.class);
        assertSame(strategy, manager.applyDecorators(strategy));
    }

    @Test
    @DisplayName("未注册到容器的装饰器按注册顺序形成装饰链")
    void shouldApplyMultipleDecoratorsInOrder() {
        StubDecorator first = new StubDecorator(1);
        StubDecorator second = new StubDecorator(2);
        manager.registerDecorator(second);
        manager.registerDecorator(first);

        StorageStrategy strategy = mock(StorageStrategy.class);
        StorageStrategy decorated = manager.applyDecorators(strategy);

        assertEquals(second, decorated);
        assertEquals(first, second.currentDelegate());
        assertEquals(strategy, first.currentDelegate());
    }

    @Test
    @DisplayName("事件对象暴露目标策略类型")
    void shouldExposeTargetClassInEvents() {
        DecoratorRegisteredEvent registered =
            new DecoratorRegisteredEvent(this, LocalStorageStrategy.class);
        DecoratorUnregisteredEvent unregistered =
            new DecoratorUnregisteredEvent(this, LocalStorageStrategy.class);

        assertEquals(LocalStorageStrategy.class, registered.getTargetClass());
        assertEquals(LocalStorageStrategy.class, unregistered.getTargetClass());
        assertSame(this, registered.getSource());
        assertSame(this, unregistered.getSource());
    }

    private static class StubDecorator extends StorageStrategyDecorator<StorageStrategy> {

        private final int order;

        StubDecorator(int order) {
            this.order = order;
        }

        @Override
        public Class<StorageStrategy> getTargetStrategyClass() {
            return StorageStrategy.class;
        }

        @Override
        public int getOrder() {
            return order;
        }

        StorageStrategy currentDelegate() {
            return getDelegate();
        }
    }
}
