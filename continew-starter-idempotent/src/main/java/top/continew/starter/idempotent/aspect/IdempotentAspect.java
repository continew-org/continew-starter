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

package top.continew.starter.idempotent.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.idempotent.annotation.Idempotent;
import top.continew.starter.idempotent.service.IdempotentService;
import top.continew.starter.idempotent.util.IdempotentKeyGenerator;

/**
 * 注解切面
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 19:27
 * @Package top.continew.starter.idempotent.aspect.IdempotentAspect
 */
@Aspect
public class IdempotentAspect {

    private final IdempotentService idempotentService;

    public IdempotentAspect(IdempotentService idempotentService) {
        this.idempotentService = idempotentService;
    }

    /**
     * AspectJ 环绕通知
     *
     * @param joinPoint  切点
     * @param idempotent 注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes())
            .getRequest();
        String key = IdempotentKeyGenerator.generateKey(idempotent.prefix(), joinPoint, request);

        try {
            if (!idempotentService.checkAndLock(key, idempotent.timeout())) {
                throw new RuntimeException(idempotent.message());
            }
            return joinPoint.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            idempotentService.unlock(key);
        }
    }
}