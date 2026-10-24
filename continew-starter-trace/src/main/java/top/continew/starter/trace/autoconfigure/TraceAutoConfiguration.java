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

package top.continew.starter.trace.autoconfigure;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.trace.filter.TraceResponseFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;

/**
 * 链路追踪自动配置
 *
 * @author Jasmine
 * @author Charles7c
 * @since 1.3.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(TraceProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.TRACE, name = PropertiesConstants.ENABLED,
    havingValue = "true")
public class TraceAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceAutoConfiguration.class);

    private final TraceProperties traceProperties;

    public TraceAutoConfiguration(TraceProperties traceProperties) {
        this.traceProperties = traceProperties;
    }

    /**
     * 链路追踪响应过滤器
     *
     * <p>
     * Tracer 不可用时启动即失败，避免追踪能力静默失效。
     * </p>
     *
     * @param tracerProvider Tracer 提供者
     * @return 过滤器注册信息
     */
    @Bean
    public FilterRegistrationBean<TraceResponseFilter> traceResponseFilter(
        ObjectProvider<Tracer> tracerProvider) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null || tracer == Tracer.NOOP) {
            String tracerType = tracer == null ? "缺失" : tracer.getClass().getName();
            throw new IllegalStateException(
                "[ContiNew Starter] - 已启用链路追踪，但未检测到可用的 Micrometer Tracing 桥（当前 Tracer: "
                    + tracerType
                    + "）。请确认 micrometer-tracing-bridge-brave 在类路径中且未被排除，"
                    + "spring-boot-micrometer-tracing-brave 也未被 spring.autoconfigure.exclude 排除");
        }
        FilterRegistrationBean<TraceResponseFilter> registrationBean =
            new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceResponseFilter(tracer, traceProperties));
        registrationBean.setOrder(OrderedConstants.Filter.TRACE_FILTER);
        registrationBean.addUrlPatterns(StringConstants.PATH_PATTERN_CURRENT_DIR);
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        return registrationBean;
    }

    @PostConstruct
    public void postConstruct() {
        TLogProperties tlogProperties = traceProperties.getTlog();
        if (tlogProperties != null && tlogProperties.isConfigured()) {
            LOGGER.warn(
                "[ContiNew Starter] - 检测到已移除的 continew-starter.trace.tlog 配置，自 3.0.0 起该配置不再生效，请移除");
        }
        LOGGER.debug("[ContiNew Starter] - Auto Configuration 'Trace' completed initialization.");
    }
}
