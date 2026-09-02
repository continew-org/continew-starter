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

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import java.lang.annotation.Annotation;

/**
 * CRUD API 注解通知
 *
 * @author Charles7c
 * @since 2.7.5
 */
// Advisor 为 Spring 单例 Bean，按对象标识工作、不会真正序列化，故无需按字段重写 equals/hashCode
@SuppressWarnings("java:S2160")
public class CrudApiAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private final transient Advice advice;
    private final transient Pointcut pointcut;

    public CrudApiAnnotationAdvisor(CrudApiAnnotationInterceptor advice,
        Class<? extends Annotation> annotation) {
        this.advice = advice;
        this.pointcut =
            new ComposablePointcut(AnnotationMatchingPointcut.forMethodAnnotation(annotation));
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (this.advice instanceof BeanFactoryAware beanFactoryAware) {
            beanFactoryAware.setBeanFactory(beanFactory);
        }
    }
}
