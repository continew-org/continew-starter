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

package top.continew.starter.cache.multicache.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import top.continew.starter.cache.multicache.annotation.MultiCacheRefresh;

import jakarta.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多级缓存定时刷新切面
 *
 * <p>
 * 拦截标注 {@link MultiCacheRefresh} 的方法：方法正常返回后，按注解周期在后台调度线程中刷新。
 * 刷新时先按 @Cacheable 声明的 cacheName 失效该键，再调用<b>Spring 代理对象</b>重放方法，
 * 让调用链经过 @Cacheable 的缓存拦截器完成回源并写回新值（等效于 JetCache 的 {@code @CacheRefresh}）。
 * 若不先失效，重放会被 @Cacheable 命中短路而无法真正刷新。
 * </p>
 * <p>
 * 同一方法 + 同一参数组合只注册一个刷新任务（任务数有上限，防止动态参数方法无界注册）；
 * 刷新执行期间设置线程标记，防止刷新调用再次进入本切面造成任务重复注册。
 * 仅支持 @Cacheable 使用默认键生成器或显式 keyGenerator 的场景，SpEL key 不支持。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
@Aspect
public class MultiCacheRefreshAspect implements BeanFactoryAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiCacheRefreshAspect.class);

    /**
     * 刷新任务数上限：任务按“方法 + 实参”注册，动态参数方法需封顶防止无界增长
     */
    private static final int MAX_TASKS = 1024;

    /**
     * 刷新执行标记：刷新线程重放方法时不再注册新任务
     */
    private static final ThreadLocal<Boolean> REFRESHING = ThreadLocal.withInitial(() -> false);

    private final CacheManager cacheManager;
    private final ScheduledExecutorService scheduler = Executors
        .newScheduledThreadPool(1, new RefreshThreadFactory());

    /**
     * 已注册的刷新任务（方法签名 + 参数摘要 → 任务），防止重复注册
     */
    private final Map<String, RefreshTask> refreshTasks = new ConcurrentHashMap<>();
    private BeanFactory beanFactory;

    public MultiCacheRefreshAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * 拦截标注 {@link MultiCacheRefresh} 的方法：正常返回后注册刷新任务，刷新线程内不重复注册
     *
     * @param joinPoint          连接点
     * @param multiCacheRefresh 刷新注解配置
     * @return 目标方法执行结果
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("@annotation(multiCacheRefresh)")
    public Object around(ProceedingJoinPoint joinPoint, MultiCacheRefresh multiCacheRefresh)
        throws Throwable {
        Object result = joinPoint.proceed();
        if (!Boolean.TRUE.equals(REFRESHING.get())) {
            registerRefreshTask(joinPoint, multiCacheRefresh);
        }
        return result;
    }

    private void registerRefreshTask(ProceedingJoinPoint joinPoint, MultiCacheRefresh annotation) {
        if (refreshTasks.size() >= MAX_TASKS) {
            LOGGER.warn("[ContiNew Starter] - Cache refresh task limit ({}) reached, no more tasks "
                + "will be registered.", MAX_TASKS);
            return;
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Cacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(method, Cacheable.class);
        if (cacheable == null) {
            LOGGER.warn("[ContiNew Starter] - @MultiCacheRefresh requires @Cacheable on the same "
                + "method, refresh task is not registered: {}", method);
            return;
        }
        if (StringUtils.hasText(cacheable.key())) {
            LOGGER.warn("[ContiNew Starter] - @MultiCacheRefresh does not support SpEL key on "
                + "@Cacheable, refresh task is not registered: {}", method);
            return;
        }
        long period = annotation.timeUnit().toMillis(annotation.refresh());
        if (period <= 0) {
            LOGGER.warn("[ContiNew Starter] - Cache refresh period must be positive, refresh task "
                + "is not registered: {}", method);
            return;
        }
        String[] cacheNames = cacheable.cacheNames().length > 0
            ? cacheable.cacheNames()
            : cacheable.value();
        Object[] args = joinPoint.getArgs();
        Object cacheKey = resolveKeyGenerator(cacheable.keyGenerator())
            .generate(joinPoint.getTarget(), method, args);
        Object proxy = joinPoint.getThis();
        // 代理对象上反射查找目标方法：多切面场景（如 @Transactional + @Cacheable + @MultiCacheRefresh）
        // 下 proxy 是 CGLIB 子类，findMethod 按方法签名查找仍能定位到目标方法。反射调用会经过
        // 完整的代理链（含 @Cacheable 拦截器），确保刷新时真正回源。
        Method proxyMethod = ReflectionUtils.findMethod(proxy.getClass(), method.getName(),
            method.getParameterTypes());
        if (proxyMethod == null) {
            LOGGER.warn("[ContiNew Starter] - Cannot resolve proxy method for cache refresh: {}",
                method);
            return;
        }
        RefreshTask task =
            new RefreshTask(proxy, proxyMethod, args, cacheNames, cacheKey, period, Math
                .max(annotation.timeUnit().toMillis(annotation.retryAfterFailure()), 1L));
        String taskKey = joinPoint.getSignature().toLongString() + "#" + Arrays.deepHashCode(args);
        if (refreshTasks.putIfAbsent(taskKey, task) == null) {
            schedule(task, task.period);
            LOGGER.debug("[ContiNew Starter] - Registered cache refresh task: {}, period: {}ms",
                joinPoint.getSignature().toShortString(), task.period);
        }
    }

    /**
     * 与 @Cacheable 一致地解析键生成器：显式指定的按 Bean 名查找；未指定时优先复用容器中
     * 唯一的 KeyGenerator Bean（含 CachingConfigurer 提供的），否则回退 SimpleKeyGenerator
     *
     * @param generatorBeanName @Cacheable 显式指定的键生成器 Bean 名（可为空）
     * @return 键生成器
     */
    private KeyGenerator resolveKeyGenerator(String generatorBeanName) {
        if (StringUtils.hasText(generatorBeanName) && beanFactory != null) {
            return beanFactory.getBean(generatorBeanName, KeyGenerator.class);
        }
        if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
            Map<String, KeyGenerator> generators =
                listableBeanFactory.getBeansOfType(KeyGenerator.class);
            if (generators.size() == 1) {
                return generators.values().iterator().next();
            }
            KeyGenerator generator = generators.get("keyGenerator");
            if (generator != null) {
                return generator;
            }
        }
        return new SimpleKeyGenerator();
    }

    /**
     * 单次调度、自续期：刷新成功按刷新周期续期，失败按重试间隔续期
     *
     * @param task         刷新任务
     * @param delayMillis  距下次执行的延迟（毫秒）
     */
    private void schedule(RefreshTask task, long delayMillis) {
        if (task.cancelled) {
            return;
        }
        task.currentFuture = scheduler.schedule(() -> {
            boolean success = executeRefresh(task);
            schedule(task, success ? task.period : task.retryDelay);
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 刷新：先逐个 cacheName 失效该键，再经代理重放方法，@Cacheable 未命中后回源并写回新值
     *
     * @param task 刷新任务
     * @return 刷新是否成功（失败后按重试间隔续期）
     */
    private boolean executeRefresh(RefreshTask task) {
        REFRESHING.set(true);
        try {
            for (String cacheName : task.cacheNames) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.evict(task.cacheKey);
                }
            }
            ReflectionUtils.makeAccessible(task.method);
            task.method.invoke(task.proxy, task.args);
            return true;
        } catch (Exception e) {
            Throwable cause = e instanceof InvocationTargetException ite && ite.getCause() != null
                ? ite.getCause()
                : e;
            LOGGER.warn("[ContiNew Starter] - Cache refresh task failed, will retry after {}ms. "
                + "Method: {}, cause: {}", task.retryDelay, task.method, cause.getMessage());
            return false;
        } finally {
            REFRESHING.remove();
        }
    }

    /**
     * 停止全部刷新任务并关闭调度线程
     */
    @PreDestroy
    public void destroy() {
        refreshTasks.values().forEach(task -> {
            task.cancelled = true;
            ScheduledFuture<?> future = task.currentFuture;
            if (future != null) {
                future.cancel(false);
            }
        });
        refreshTasks.clear();
        scheduler.shutdownNow();
    }

    /**
     * 当前已注册的刷新任务数（监控/测试用）
     *
     * @return 已注册的刷新任务数
     */
    public int getRefreshTaskCount() {
        return refreshTasks.size();
    }

    /**
     * 刷新任务（持有重放所需的代理、方法与参数快照）
     */
    private static final class RefreshTask {

        final Object proxy;
        final Method method;
        final Object[] args;
        final String[] cacheNames;
        final Object cacheKey;
        final long period;
        final long retryDelay;
        volatile ScheduledFuture<?> currentFuture;
        volatile boolean cancelled;

        RefreshTask(Object proxy, Method method, Object[] args, String[] cacheNames,
            Object cacheKey,
            long period, long retryDelay) {
            this.proxy = proxy;
            this.method = method;
            this.args = args;
            this.cacheNames = cacheNames;
            this.cacheKey = cacheKey;
            this.period = period;
            this.retryDelay = retryDelay;
        }
    }

    /**
     * 刷新调度线程工厂（守护线程，命名规范）
     */
    private static class RefreshThreadFactory implements ThreadFactory {

        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable,
                "continew-cache-refresh-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
