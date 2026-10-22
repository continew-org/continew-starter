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

import org.springframework.beans.factory.ObjectProvider;
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
import top.continew.starter.web.model.ResponseConfig;

import java.util.Locale;

/**
 * 全局响应自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GlobalResponseProperties.class)
@PropertySource(value = "classpath:default-response.yml",
    factory = GeneralPropertySourceFactory.class)
public class GlobalResponseAutoConfiguration {

    /**
     * 构造方法
     *
     * @param properties 全局响应配置属性
     */
    public GlobalResponseAutoConfiguration(GlobalResponseProperties properties) {
        ResponseConfig.configure(properties.getDefaultSuccessCode(), properties
            .getDefaultSuccessMsg(), properties.getDefaultErrorCode(),
            properties.getDefaultErrorMsg());
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionAliasRegister exceptionAliasRegister() {
        return new ExceptionAliasRegister();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeforeControllerAdviceProcess beforeControllerAdviceProcess(
        GlobalResponseProperties properties) {
        return new DefaultBeforeControllerAdviceProcess(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalResponseBodyAdvice globalResponseBodyAdvice(GlobalResponseProperties properties,
        ObjectProvider<MessageSource> messageSourceProvider) {
        return new GlobalResponseBodyAdvice(properties, messageSourceProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(GlobalResponseProperties properties,
        ExceptionAliasRegister exceptionAliasRegister,
        BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
        return new GlobalExceptionHandler(properties, exceptionAliasRegister,
            beforeControllerAdviceProcess);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = PropertiesConstants.WEB_RESPONSE,
        name = "i18n",
        havingValue = "true")
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n", "i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.CHINA);
        return messageSource;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
    static class ConstraintViolationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ConstraintViolationExceptionHandler constraintViolationExceptionHandler(
            GlobalResponseProperties properties,
            ExceptionAliasRegister exceptionAliasRegister,
            BeforeControllerAdviceProcess beforeControllerAdviceProcess) {
            return new ConstraintViolationExceptionHandler(properties,
                exceptionAliasRegister,
                beforeControllerAdviceProcess);
        }
    }
}
