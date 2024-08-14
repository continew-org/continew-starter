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
import com.feiniaojin.gracefulresponse.api.ResponseFactory;
import com.feiniaojin.gracefulresponse.api.ResponseStatusFactory;
import com.feiniaojin.gracefulresponse.defaults.DefaultResponseFactory;
import com.feiniaojin.gracefulresponse.defaults.DefaultResponseStatusFactoryImpl;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /**
     * 全局异常处理
     */
    @Bean
    @ConditionalOnMissingBean
    public GrGlobalExceptionAdvice globalExceptionAdvice() {
        return new GrGlobalExceptionAdvice();
    }

    /**
     * 全局校验异常处理
     */
    @Bean
    @ConditionalOnMissingBean
    public GrValidationExceptionAdvice validationExceptionAdvice() {
        return new GrValidationExceptionAdvice();
    }

    /**
     * 全局响应体处理（非 void）
     */
    @Bean
    @ConditionalOnMissingBean
    public GrNotVoidResponseBodyAdvice notVoidResponseBodyAdvice() {
        return new GrNotVoidResponseBodyAdvice();
    }

    /**
     * 全局响应体处理（void）
     */
    @Bean
    @ConditionalOnMissingBean
    public GrVoidResponseBodyAdvice voidResponseBodyAdvice() {
        return new GrVoidResponseBodyAdvice();
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
     * 国际化支持
     */
    @Bean
    @ConditionalOnProperty(prefix = PropertiesConstants.WEB_RESPONSE, name = "i18n", havingValue = "true")
    public GrI18nAdvice i18nAdvice() {
        return new GrI18nAdvice();
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
     * SpringDoc 全局响应处理器
     *
     * @return {@link ApiDocGlobalResponseHandler }
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiDocGlobalResponseHandler apiDocGlobalResponseHandler() {
        return new ApiDocGlobalResponseHandler();
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Web-Global Response' completed initialization.");
    }
}
