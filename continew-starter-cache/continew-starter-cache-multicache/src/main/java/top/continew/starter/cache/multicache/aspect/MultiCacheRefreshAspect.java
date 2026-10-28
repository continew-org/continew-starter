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
 * 同一方法 + 同一参数组合只注册一个刷新任务。任务按方法限流（每方法 {@code MAX_TASKS_PER_METHOD} 个、
 * 全局 {@code MAX_TASKS} 个），达到上限时淘汰最近最少使用的任务而不是丢弃新任务，
 * 因此某个动态参数方法刷屏不会让参数固定的正常方法静默失效。
 * 刷新执行期间设置线程标记，防止刷新调用再次进入本切面造成任务重复注册。
 * 仅支持 @Cacheable 使用默认键生成器或显式 keyGenerator 的场景，SpEL key 不支持。
 * 仅支持 CGLIB 代理（默认）：JDK 动态代理下无法在代理类上定位目标方法，任务不会注册并输出 WARN。
 * </p>
 *
 * @author KAI
 * @since 3.0.0
 */
@Aspect
public class MultiCacheRefreshAspect implements BeanFactoryAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiCacheRefreshAspect.class);

    /**
     * 刷新任务全局上限：任务按“方法 + 实参”注册，防止动态参数方法无界增长
     */
    private static final int MAX_TASKS = 1024;

    /**
     * 单个方法的刷新任务上限：动态参数方法只影响自己，不会挤占其他方法的名额
     */
    private static final int MAX_TASKS_PER_METHOD = 64;

    /**
     * 刷新执行标记：刷新线程重放方法时不再注册新任务
     */
    private static final ThreadLocal<Boolean> REFRESHING = ThreadLocal.withInitial(() -> false);

    private final CacheManager cacheManager;
    private final ScheduledExecutorService scheduler;
    private final int maxTasks;
    private final int maxTasksPerMethod;

    /**
     * 已注册的刷新任务（方法签名 → (任务键 → 任务)），按方法分组便于按方法限流与淘汰
     */
    private final Map<String, Map<String, RefreshTask>> refreshTasks = new ConcurrentHashMap<>();

    /**
     * 已注册任务总数（与 {@link #refreshTasks} 同步维护，避免每次调用遍历统计）
     */
    private final AtomicInteger taskCount = new AtomicInteger();
    private BeanFactory beanFactory;

    public MultiCacheRefreshAspect(CacheManager cacheManager) {
        this(cacheManager, Executors.newScheduledThreadPool(1, new RefreshThreadFactory()),
            MAX_TASKS,
            MAX_TASKS_PER_METHOD);
    }

    MultiCacheRefreshAspect(CacheManager cacheManager, int maxTasks, int maxTasksPerMethod) {
        this(cacheManager, Executors.newScheduledThreadPool(1, new RefreshThreadFactory()),
            maxTasks,
            maxTasksPerMethod);
    }

    /**
     * 可指定上限与调度器的构造器（仅供测试缩小上限或注入调度器，业务请使用
     * {@link #MultiCacheRefreshAspect(CacheManager)}）
     *
     * @param cacheManager      缓存管理器
     * @param scheduler         刷新调度器
     * @param maxTasks          刷新任务全局上限
     * @param maxTasksPerMethod 单个方法的刷新任务上限
     */
    MultiCacheRefreshAspect(CacheManager cacheManager, ScheduledExecutorService scheduler,
        int maxTasks, int maxTasksPerMethod) {
        this.cacheManager = cacheManager;
        this.scheduler = scheduler;
        this.maxTasks = maxTasks;
        this.maxTasksPerMethod = maxTasksPerMethod;
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
        String methodKey = joinPoint.getSignature().toLongString();
        Object[] args = joinPoint.getArgs();
        String taskKey = methodKey + "#" + Arrays.deepHashCode(args);
        Map<String, RefreshTask> existingTasks = refreshTasks.get(methodKey);
        if (existingTasks != null) {
            RefreshTask registered = existingTasks.get(taskKey);
            if (registered != null) {
                // 热路径：已有任务只更新时间戳，作为达到上限时的淘汰依据
                registered.lastAccessNanos = System.nanoTime();
                return;
            }
        }
        Object cacheKey = resolveKeyGenerator(cacheable.keyGenerator())
            .generate(joinPoint.getTarget(), method, args);
        Object proxy = joinPoint.getThis();
        // 代理对象上反射查找目标方法：多切面场景（如 @Transactional + @Cacheable + @MultiCacheRefresh）
        // 下 proxy 是 CGLIB 子类，findMethod 按方法签名查找仍能定位到目标方法。反射调用会经过
        // 完整的代理链（含 @Cacheable 拦截器），确保刷新时真正回源。
        Method proxyMethod = ReflectionUtils.findMethod(proxy.getClass(), method.getName(),
            method.getParameterTypes());
        if (proxyMethod == null) {
            LOGGER.warn("[ContiNew Starter] - Cannot resolve proxy method for cache refresh, "
                + "JDK dynamic proxy is not supported: {}", method);
            return;
        }
        RefreshTask task = new RefreshTask(methodKey, taskKey, proxy, proxyMethod, args, cacheNames,
            cacheKey, period, Math.max(annotation.timeUnit().toMillis(annotation
                .retryAfterFailure()), 1L));
        synchronized (this) {
            // 任务组在确认注册成功后才创建，JDK 动态代理等提前返回的路径不留空分组
            Map<String, RefreshTask> methodTasks =
                refreshTasks.computeIfAbsent(methodKey, key -> new ConcurrentHashMap<>());
            if (methodTasks.get(taskKey) != null) {
                return;
            }
            if (methodTasks.size() >= maxTasksPerMethod) {
                evictLeastRecentlyUsed(methodTasks);
            }
            if (taskCount.get() >= maxTasks) {
                evictGlobalLeastRecentlyUsed();
            }
            methodTasks.put(taskKey, task);
            taskCount.incrementAndGet();
            schedule(task, task.period);
        }
        LOGGER.debug("[ContiNew Starter] - Registered cache refresh task: {}, period: {}ms",
            joinPoint.getSignature().toShortString(), task.period);
    }

    /**
     * 淘汰同一方法内最近最少使用的任务（达到按方法上限时调用），保证该方法的新任务仍能注册
     *
     * @param methodTasks 该方法的任务表
     */
    private void evictLeastRecentlyUsed(Map<String, RefreshTask> methodTasks) {
        RefreshTask victim = null;
        for (RefreshTask task : methodTasks.values()) {
            if (victim == null || task.lastAccessNanos < victim.lastAccessNanos) {
                victim = task;
            }
        }
        if (victim == null || methodTasks.remove(victim.taskKey) == null) {
            return;
        }
        cancelTask(victim);
        LOGGER.warn("[ContiNew Starter] - Cache refresh task limit per method ({}) reached, "
            + "evicted least recently used task: {}, period: {}ms", maxTasksPerMethod,
            victim.taskKey,
            victim.period);
    }

    /**
     * 淘汰全局最近最少使用的任务（达到全局上限时调用），保证新任务仍能注册
     */
    private void evictGlobalLeastRecentlyUsed() {
        RefreshTask victim = null;
        for (Map<String, RefreshTask> methodTasks : refreshTasks.values()) {
            for (RefreshTask task : methodTasks.values()) {
                if (victim == null || task.lastAccessNanos < victim.lastAccessNanos) {
                    victim = task;
                }
            }
        }
        if (victim == null) {
            return;
        }
        Map<String, RefreshTask> methodTasks = refreshTasks.get(victim.methodKey);
        if (methodTasks == null || methodTasks.remove(victim.taskKey) == null) {
            return;
        }
        cancelTask(victim);
        LOGGER.warn("[ContiNew Starter] - Cache refresh task limit ({}) reached, evicted least "
            + "recently used task: {}, method: {}", maxTasks, victim.taskKey, victim.methodKey);
    }

    /**
     * 注销任务：标记取消并撤销后续调度
     *
     * @param task 待注销的任务
     */
    private void cancelTask(RefreshTask task) {
        task.cancelled = true;
        ScheduledFuture<?> future = task.currentFuture;
        if (future != null) {
            future.cancel(false);
        }
        taskCount.decrementAndGet();
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
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            boolean success = executeRefresh(task);
            schedule(task, success ? task.period : task.retryDelay);
        }, delayMillis, TimeUnit.MILLISECONDS);
        task.currentFuture = future;
        // 复查取消：cancelTask 可能恰在上面的 cancelled 检查后、本行赋值前执行，
        // 彼时它读到旧的（或空的）currentFuture，新提交的 future 由这里兜住取消
        if (task.cancelled) {
            future.cancel(false);
        }
    }

    /**
     * 刷新：先逐个 cacheName 失效该键，再经代理重放方法，@Cacheable 未命中后回源并写回新值
     *
     * @param task 刷新任务
     * @return 刷新是否成功（失败后按重试间隔续期）
     */
    private boolean executeRefresh(RefreshTask task) {
        if (task.cancelled) {
            return true;
        }
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
        refreshTasks.values().forEach(methodTasks -> methodTasks.values().forEach(task -> {
            task.cancelled = true;
            ScheduledFuture<?> future = task.currentFuture;
            if (future != null) {
                future.cancel(false);
            }
        }));
        refreshTasks.clear();
        taskCount.set(0);
        scheduler.shutdownNow();
    }

    /**
     * 当前已注册的刷新任务数（监控/测试用）
     *
     * @return 已注册的刷新任务数
     */
    public int getRefreshTaskCount() {
        return taskCount.get();
    }

    /**
     * 刷新任务（持有重放所需的代理、方法与参数快照）
     */
    private static final class RefreshTask {

        final String methodKey;
        final String taskKey;
        final Object proxy;
        final Method method;
        final Object[] args;
        final String[] cacheNames;
        final Object cacheKey;
        final long period;
        final long retryDelay;
        volatile ScheduledFuture<?> currentFuture;
        volatile boolean cancelled;

        /**
         * 最近一次注册/命中时间（纳秒），达到上限时据此淘汰
         */
        volatile long lastAccessNanos;

        RefreshTask(String methodKey,
            String taskKey,
            Object proxy,
            Method method,
            Object[] args,
            String[] cacheNames,
            Object cacheKey,
            long period,
            long retryDelay) {
            this.methodKey = methodKey;
            this.taskKey = taskKey;
            this.proxy = proxy;
            this.method = method;
            this.args = args;
            this.cacheNames = cacheNames;
            this.cacheKey = cacheKey;
            this.period = period;
            this.retryDelay = retryDelay;
            this.lastAccessNanos = System.nanoTime();
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
