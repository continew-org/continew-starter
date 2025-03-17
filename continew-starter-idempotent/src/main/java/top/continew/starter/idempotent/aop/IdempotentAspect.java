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

package top.continew.starter.idempotent.aop;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.util.expression.ExpressionUtils;
import top.continew.starter.idempotent.annotation.Idempotent;
import top.continew.starter.idempotent.autoconfigure.IdempotentProperties;
import top.continew.starter.idempotent.exception.IdempotentException;
import top.continew.starter.idempotent.generator.IdempotentNameGenerator;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 幂等切面
 *
 * @author loach
 * @author Charles7c
 * @since 2.10.0
 */
@Aspect
public class IdempotentAspect {

    private final IdempotentProperties properties;
    private final IdempotentNameGenerator nameGenerator;

    public IdempotentAspect(IdempotentProperties properties, IdempotentNameGenerator nameGenerator) {
        this.properties = properties;
        this.nameGenerator = nameGenerator;
    }

    /**
     * 幂等处理
     *
     * @param joinPoint  切点
     * @param idempotent 幂等注解
     * @return 目标方法的执行结果
     * @throws Throwable /
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String cacheKey = this.getCacheKey(joinPoint, idempotent);
        // 如果键已存在，则抛出异常
        if (!RedisUtils.setIfAbsent(cacheKey, Duration.ofMillis(idempotent.unit().toMillis(idempotent.timeout())))) {
            throw new IdempotentException(idempotent.message());
        }
        // 执行目标方法
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 删除键
            RedisUtils.delete(cacheKey);
            throw e;
        }
    }

    /**
     * 获取缓存 Key
     *
     * @param joinPoint  切点
     * @param idempotent 幂等注解
     * @return 缓存 Key
     */
    private String getCacheKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        Object target = joinPoint.getTarget();
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        // 获取名称
        String name = idempotent.name();
        if (CharSequenceUtil.isBlank(name)) {
            name = nameGenerator.generate(target, method, args);
        }
        // 解析 Key
        String key = idempotent.key();
        if (CharSequenceUtil.isNotBlank(key)) {
            Object eval = ExpressionUtils.eval(key, target, method, args);
            if (ObjectUtil.isNull(eval)) {
                throw new IdempotentException("幂等 Key 解析错误");
            }
            key = Convert.toStr(eval);
        }
        return RedisUtils.formatKey(properties.getKeyPrefix(), name, key);
    }
}