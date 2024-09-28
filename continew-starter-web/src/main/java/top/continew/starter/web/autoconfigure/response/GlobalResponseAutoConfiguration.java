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

package top.continew.starter.web.autoconfigure.response;

import com.feiniaojin.gracefulresponse.ExceptionAliasRegister;
import com.feiniaojin.gracefulresponse.advice.*;
import com.feiniaojin.gracefulresponse.advice.lifecycle.exception.BeforeControllerAdviceProcess;
import com.feiniaojin.gracefulresponse.advice.lifecycle.exception.ControllerAdvicePredicate;
import com.feiniaojin.gracefulresponse.advice.lifecycle.response.ResponseBodyAdvicePredicate;
import com.feiniaojin.gracefulresponse.api.ResponseFactory;
import com.feiniaojin.gracefulresponse.api.ResponseStatusFactory;
import com.feiniaojin.gracefulresponse.defaults.DefaultResponseFactory;
import com.feiniaojin.gracefulresponse.defaults.DefaultResponseStatusFactoryImpl;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.parsers.ReturnTypeParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ResourceBundleMessageSource;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局响应自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GlobalResponseProperties.class)
@PropertySource(value = "classpath:default-web.yml", factory = GeneralPropertySourceFactory.class)
public class GlobalResponseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GlobalResponseAutoConfiguration.class);
    private final GlobalResponseProperties globalResponseProperties;

    public GlobalResponseAutoConfiguration(GlobalResponseProperties globalResponseProperties) {
        this.globalResponseProperties = globalResponseProperties;
    }

    /**
     * 全局响应体处理（非 void）
     */
    @Bean
    @ConditionalOnMissingBean
    public GrNotVoidResponseBodyAdvice grNotVoidResponseBodyAdvice() {
        GrNotVoidResponseBodyAdvice notVoidResponseBodyAdvice = new GrNotVoidResponseBodyAdvice();
        CopyOnWriteArrayList<ResponseBodyAdvicePredicate> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        copyOnWriteArrayList.add(notVoidResponseBodyAdvice);
        notVoidResponseBodyAdvice.setPredicates(copyOnWriteArrayList);
        notVoidResponseBodyAdvice.setResponseBodyAdviceProcessor(notVoidResponseBodyAdvice);
        return notVoidResponseBodyAdvice;
    }

    /**
     * 全局响应体处理（void）
     */
    @Bean
    @ConditionalOnMissingBean
    public GrVoidResponseBodyAdvice grVoidResponseBodyAdvice() {
        GrVoidResponseBodyAdvice voidResponseBodyAdvice = new GrVoidResponseBodyAdvice();
        CopyOnWriteArrayList<ResponseBodyAdvicePredicate> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        copyOnWriteArrayList.add(voidResponseBodyAdvice);
        voidResponseBodyAdvice.setPredicates(copyOnWriteArrayList);
        voidResponseBodyAdvice.setResponseBodyAdviceProcessor(voidResponseBodyAdvice);
        return voidResponseBodyAdvice;
    }

    /**
     * 处理前回调（目前仅打印异常日志）
     */
    @Bean
    @ConditionalOnMissingBean
    public BeforeControllerAdviceProcess beforeControllerAdviceProcess() {
        return new DefaultBeforeControllerAdviceProcessImpl(globalResponseProperties);
    }

    /**
     * 框架异常处理器
     */
    @Bean
    public FrameworkExceptionAdvice frameworkExceptionAdvice(BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
        FrameworkExceptionAdvice frameworkExceptionAdvice = new FrameworkExceptionAdvice();
        frameworkExceptionAdvice.setRejectStrategy(new DefaultRejectStrategyImpl());
        frameworkExceptionAdvice.setControllerAdviceProcessor(frameworkExceptionAdvice);
        frameworkExceptionAdvice.setBeforeControllerAdviceProcess(beforeControllerAdviceProcess);
        frameworkExceptionAdvice.setControllerAdviceHttpProcessor(frameworkExceptionAdvice);
        return frameworkExceptionAdvice;
    }

    /**
     * 数据校验异常处理器
     */
    @Bean
    public DataExceptionAdvice dataExceptionAdvice(BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
        DataExceptionAdvice dataExceptionAdvice = new DataExceptionAdvice();
        dataExceptionAdvice.setRejectStrategy(new DefaultRejectStrategyImpl());
        dataExceptionAdvice.setControllerAdviceProcessor(dataExceptionAdvice);
        dataExceptionAdvice.setBeforeControllerAdviceProcess(beforeControllerAdviceProcess);
        dataExceptionAdvice.setControllerAdviceHttpProcessor(dataExceptionAdvice);
        return dataExceptionAdvice;
    }

    /**
     * 默认全局异常处理器
     */
    @Bean
    public DefaultGlobalExceptionAdvice defaultGlobalExceptionAdvice(BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
        DefaultGlobalExceptionAdvice advice = new DefaultGlobalExceptionAdvice();
        advice.setRejectStrategy(new DefaultRejectStrategyImpl());
        CopyOnWriteArrayList<ControllerAdvicePredicate> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        copyOnWriteArrayList.add(advice);
        advice.setPredicates(copyOnWriteArrayList);
        advice.setControllerAdviceProcessor(advice);
        advice.setBeforeControllerAdviceProcess(beforeControllerAdviceProcess);
        advice.setControllerAdviceHttpProcessor(advice);
        return advice;
    }

    /**
     * 默认参数校验异常处理器
     */
    @Bean
    public DefaultValidationExceptionAdvice defaultValidationExceptionAdvice(BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
        DefaultValidationExceptionAdvice advice = new DefaultValidationExceptionAdvice();
        advice.setRejectStrategy(new DefaultRejectStrategyImpl());
        advice.setControllerAdviceProcessor(advice);
        advice.setBeforeControllerAdviceProcess(beforeControllerAdviceProcess);
        advice.setControllerAdviceHttpProcessor(advice);
        return advice;
    }

    /**
     * 国际化支持
     */
    @Bean
    @ConditionalOnProperty(prefix = PropertiesConstants.WEB_RESPONSE, name = "i18n", havingValue = "true")
    public GrI18nResponseBodyAdvice grI18nResponseBodyAdvice() {
        GrI18nResponseBodyAdvice i18nResponseBodyAdvice = new GrI18nResponseBodyAdvice();
        CopyOnWriteArrayList<ResponseBodyAdvicePredicate> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        copyOnWriteArrayList.add(i18nResponseBodyAdvice);
        i18nResponseBodyAdvice.setPredicates(copyOnWriteArrayList);
        i18nResponseBodyAdvice.setResponseBodyAdviceProcessor(i18nResponseBodyAdvice);
        return i18nResponseBodyAdvice;
    }

    /**
     * 国际化配置
     */
    @Bean
    @ConditionalOnProperty(prefix = PropertiesConstants.WEB_RESPONSE, name = "i18n", havingValue = "true")
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n", "i18n/empty-messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.CHINA);
        return messageSource;
    }

    /**
     * 响应工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public ResponseFactory responseBeanFactory() {
        return new DefaultResponseFactory();
    }

    /**
     * 响应状态工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public ResponseStatusFactory responseStatusFactory() {
        return new DefaultResponseStatusFactoryImpl();
    }

    /**
     * 异常别名注册
     */
    @Bean
    public ExceptionAliasRegister exceptionAliasRegister() {
        return new ExceptionAliasRegister();
    }

    /**
     * 响应支持
     */
    @Bean
    public AdviceSupport adviceSupport() {
        return new AdviceSupport();
    }

    /**
     * SpringDoc 全局响应处理器
     *
     * @return {@link ApiDocGlobalResponseHandler }
     */
    @Bean
    @ConditionalOnClass(ReturnTypeParser.class)
    @ConditionalOnMissingBean
    public ApiDocGlobalResponseHandler apiDocGlobalResponseHandler() {
        return new ApiDocGlobalResponseHandler(globalResponseProperties);
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Web-Global Response' completed initialization.");
    }
}
