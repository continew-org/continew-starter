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

package top.continew.starter.extension.crud.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import top.continew.starter.extension.crud.annotation.CrudApi;
import top.continew.starter.extension.crud.controller.BaseController;
import top.continew.starter.extension.crud.handler.CrudApiHandler;
import top.continew.starter.extension.crud.handler.CrudApiStrategy;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * CRUD API 注解拦截器
 *
 * @author Charles7c
 * @since 2.7.5
 */
public class CrudApiAnnotationInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 获取目标类
        Class<?> targetClass = AopUtils.getTargetClass(Objects.requireNonNull(invocation.getThis()));
        // 获取目标方法
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        Method targetMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        // 获取 @CrudApi 注解
        CrudApi crudApi = AnnotatedElementUtils.findMergedAnnotation(targetMethod, CrudApi.class);
        // 获取处理器
        CrudApiHandler<?> crudApiHandler = CrudApiStrategy.INSTANCE.handlerMap.get(targetClass);
        if (crudApiHandler != null) {
            crudApiHandler.preHandle(crudApi, invocation.getArguments(), targetMethod, targetClass);
        } else {
            CrudApiStrategy.INSTANCE.handlerMap.get(BaseController.class)
                .preHandle(crudApi, invocation.getArguments(), targetMethod, targetClass);
        }
        return invocation.proceed();
    }
}
