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

package top.charles7c.continew.starter.web.autoconfigure.trace;

import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.spring.TLogPropertyInit;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

/**
 * 链路跟踪自动配置
 *
 * @author Jasmine
 * @author Charles7c
 * @since 1.3.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(TraceProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.TRACE, name = PropertiesConstants.ENABLED, havingValue = "true")
public class TraceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TraceAutoConfiguration.class);

    private final TraceProperties traceProperties;

    public TraceAutoConfiguration(TraceProperties traceProperties) {
        this.traceProperties = traceProperties;
    }

    @Bean
    @Primary
    public TLogPropertyInit tLogPropertyInit(TLogIdGenerator tLogIdGenerator) {
        TLogProperties tLogProperties = traceProperties.getTlog();
        TLogPropertyInit tLogPropertyInit = new TLogPropertyInit();
        tLogPropertyInit.setPattern(tLogProperties.getPattern());
        tLogPropertyInit.setEnableInvokeTimePrint(tLogProperties.getEnableInvokeTimePrint());
        tLogPropertyInit.setMdcEnable(tLogProperties.getMdcEnable());
        // 设置自定义 TraceId 生成器
        TLogIdGeneratorLoader.setIdGenerator(tLogIdGenerator);
        return tLogPropertyInit;
    }

    /**
     * TLog 过滤器配置
     */
    @Bean
    public FilterRegistrationBean<TLogServletFilter> tLogServletFilter() {
        FilterRegistrationBean<TLogServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TLogServletFilter(traceProperties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 自定义 Trace ID 生成器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public TLogIdGenerator tLogIdGenerator() {
        return new TraceIdGenerator();
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Web-Trace' completed initialization.");
    }
}
