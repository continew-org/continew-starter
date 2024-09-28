package top.continew.starter.extension.tenant.handler;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * 租户数据源级隔离通知
 *
 * @author Charles7c
 * @since 2.7.0
 */
public class TenantDataSourceAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private final Advice advice;
    private final Pointcut pointcut;

    public TenantDataSourceAdvisor(TenantDataSourceInterceptor interceptor) {
        this.advice = interceptor;
        this.pointcut = buildPointcut();
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

    /**
     * 构建切点
     *
     * @return 切点
     */
    private Pointcut buildPointcut() {
        AspectJExpressionPointcut cut = new AspectJExpressionPointcut();
        cut.setExpression("!@annotation(top.continew.starter.extension.tenant.annotation.TenantDataSourceIgnore)");
        return new ComposablePointcut((Pointcut) cut);
    }
}
