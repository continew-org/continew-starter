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

import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import top.continew.starter.storage.strategy.StorageStrategy;
import top.continew.starter.storage.strategy.impl.StorageStrategyDecorator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 存储装饰器管理器
 *
 * @author echo
 * @since 2.14.0
 */
public class StorageDecoratorManager {

    private final ApplicationContext applicationContext;

    private final Map<Class<? extends StorageStrategy>, List<StorageStrategyDecorator<?>>> decoratorMap =
        new ConcurrentHashMap<>();

    private volatile boolean initialized = false;

    public StorageDecoratorManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化装饰器管理器
     * <p>
     * 扫描容器中所有 {@link StorageStrategyDecorator} 类型的 Bean，按目标策略类型分组注册，并按优先级排序，重复调用不会重复初始化
     */
    @PostConstruct
    public void init() {
        if (initialized) {
            return;
        }
        Map<String, StorageStrategyDecorator> decorators = applicationContext
            .getBeansOfType(StorageStrategyDecorator.class);

        for (Map.Entry<String, StorageStrategyDecorator> entry : decorators.entrySet()) {
            StorageStrategyDecorator<?> decorator = entry.getValue();
            Class<?> targetClass = decorator.getTargetStrategyClass();

            if (targetClass != null) {
                decoratorMap
                    .computeIfAbsent((Class<? extends StorageStrategy>) targetClass,
                        k -> new ArrayList<>())
                    .add(decorator);
            }
        }

        // 按优先级排序
        decoratorMap.values().forEach(
            list -> list.sort(Comparator.comparingInt(StorageStrategyDecorator::getOrder)));
        this.initialized = true;
    }

    /**
     * 应用装饰器到策略实例
     *
     * @param strategy 存储策略实例
     * @return {@link StorageStrategy }
     */
    @SuppressWarnings("unchecked")
    public StorageStrategy applyDecorators(StorageStrategy strategy) {
        if (!initialized) {
            init();
        }
        if (strategy == null) {
            return null;
        }
        Class<? extends StorageStrategy> strategyClass = strategy.getClass();
        List<StorageStrategyDecorator<?>> decorators = findApplicableDecorators(strategyClass);
        if (decorators.isEmpty()) {
            return strategy;
        }

        // 应用装饰器链
        StorageStrategy decorated = strategy;
        for (StorageStrategyDecorator decorator : decorators) {
            decorator.setDelegate(decorated);
            decorated = decorator;
        }

        return decorated;
    }

    /**
     * 查找适用的装饰器
     *
     * @param strategyClass 策略类
     * @return {@link List }<{@link StorageStrategyDecorator }<{@link ? }>>
     */
    private List<StorageStrategyDecorator<?>> findApplicableDecorators(
        Class<? extends StorageStrategy> strategyClass) {
        List<StorageStrategyDecorator<?>> result = new ArrayList<>();

        // 精确匹配
        List<StorageStrategyDecorator<?>> exactMatch = decoratorMap.get(strategyClass);
        if (exactMatch != null) {
            result.addAll(exactMatch);
        }

        // 继承匹配
        for (Map.Entry<Class<? extends StorageStrategy>, List<StorageStrategyDecorator<?>>> entry : decoratorMap
            .entrySet()) {
            if (entry.getKey() != strategyClass && entry.getKey().isAssignableFrom(strategyClass)) {
                result.addAll(entry.getValue());
            }
        }

        // 去重并排序
        return result.stream()
            .distinct()
            .sorted(Comparator.comparingInt(StorageStrategyDecorator::getOrder))
            .collect(Collectors.toList());
    }

    /**
     * 动态注册装饰器
     *
     * @param decorator 装饰器
     */
    public void registerDecorator(StorageStrategyDecorator<?> decorator) {
        Class<?> targetClass = decorator.getTargetStrategyClass();
        if (targetClass != null) {
            decoratorMap
                .computeIfAbsent((Class<? extends StorageStrategy>) targetClass,
                    k -> new ArrayList<>())
                .add(decorator);
            // 重新排序
            decoratorMap.get((Class<? extends StorageStrategy>) targetClass)
                .sort(Comparator.comparingInt(StorageStrategyDecorator::getOrder));
        }
    }

    /**
     * 移除装饰器
     *
     * @param decorator 装饰器
     */
    public void unregisterDecorator(StorageStrategyDecorator<?> decorator) {
        Class<?> targetClass = decorator.getTargetStrategyClass();
        if (targetClass != null) {
            List<StorageStrategyDecorator<?>> decorators = decoratorMap
                .get((Class<? extends StorageStrategy>) targetClass);
            if (decorators != null) {
                decorators.remove(decorator);
            }
        }
    }

    /**
     * 装饰器注册事件
     */
    public static class DecoratorRegisteredEvent extends ApplicationEvent {

        private final Class<?> targetClass;

        public DecoratorRegisteredEvent(Object source, Class<?> targetClass) {
            super(source);
            this.targetClass = targetClass;
        }

        public Class<?> getTargetClass() {
            return targetClass;
        }
    }

    /**
     * 装饰器注销事件
     */
    public static class DecoratorUnregisteredEvent extends ApplicationEvent {

        private final Class<?> targetClass;

        public DecoratorUnregisteredEvent(Object source, Class<?> targetClass) {
            super(source);
            this.targetClass = targetClass;
        }

        public Class<?> getTargetClass() {
            return targetClass;
        }
    }
}
