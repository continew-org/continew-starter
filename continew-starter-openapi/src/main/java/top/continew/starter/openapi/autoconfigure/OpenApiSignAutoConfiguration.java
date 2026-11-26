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

package top.continew.starter.openapi.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.GeneralPropertySourceFactory;
import top.continew.starter.openapi.audit.OpenApiSignAuditSink;
import top.continew.starter.openapi.filter.OpenApiSignFilter;
import top.continew.starter.openapi.spi.AuthorizationChecker;
import top.continew.starter.openapi.spi.CredentialResolver;
import top.continew.starter.openapi.spi.DefaultReplayGuard;
import top.continew.starter.openapi.spi.ReplayGuard;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;
import java.util.Optional;

/**
 * 开放接口验签自动配置
 *
 * <p>提供验签过滤器装配与三个扩展点的默认实现：凭证查询默认返回空（拒绝）、授权判定默认
 * 全部拒绝（安全默认）、重放防护默认为 JVM 内存实现（仅单实例）。使用方定义同名 Bean 即可
 * 覆盖默认实现，覆盖后默认 Bean 自动退位。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenApiSignProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.OPENAPI, name = PropertiesConstants.ENABLED,
    havingValue = "true")
@PropertySource(value = "classpath:default-openapi.yml",
    factory = GeneralPropertySourceFactory.class)
public class OpenApiSignAutoConfiguration {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(OpenApiSignAutoConfiguration.class);

    /**
     * 开放接口验签过滤器（按 include-paths 限定拦截范围）
     */
    @Bean
    public FilterRegistrationBean<OpenApiSignFilter> openApiSignFilter(
        OpenApiSignProperties properties,
        CredentialResolver credentialResolver, AuthorizationChecker authorizationChecker,
        ReplayGuard replayGuard, OpenApiSignAuditSink auditSink) {
        FilterRegistrationBean<OpenApiSignFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new OpenApiSignFilter(properties, credentialResolver,
            authorizationChecker, replayGuard, auditSink));
        registrationBean.setOrder(OrderedConstants.Filter.OPENAPI_SIGN_FILTER);
        properties.getIncludePaths().forEach(pattern -> {
            // Servlet url-pattern 不支持 Ant 风格，含 ** 的模式被容器接受但永不匹配，提示使用方改写
            if (pattern.contains("**")) {
                LOGGER
                    .warn(
                        "[ContiNew Starter] - Open API include-path '{}' uses Ant-style '**' which Servlet "
                            + "url-pattern does not support and will never match, use '/xxx/*' instead",
                        pattern);
            }
            registrationBean.addUrlPatterns(pattern);
        });
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        return registrationBean;
    }

    /**
     * 凭证查询默认实现：返回空，所有请求按凭证无效拒绝（starter 不持有应用数据）
     */
    @Bean
    @ConditionalOnMissingBean
    public CredentialResolver credentialResolver() {
        return accessKey -> Optional.empty();
    }

    /**
     * 授权判定默认实现：全部拒绝（安全默认，未接入授权数据时宁可全拒不可全放）
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationChecker authorizationChecker() {
        return (appId, method, path) -> false;
    }

    /**
     * 重放防护默认实现：JVM 内存占位，仅适用于单实例部署，多实例部署须覆盖为集中式存储实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ReplayGuard replayGuard() {
        return new DefaultReplayGuard();
    }

    /**
     * 审计事件默认实现：空操作，不影响验签主流程
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenApiSignAuditSink openApiSignAuditSink() {
        return event -> {
        };
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'OpenApiSign' completed initialization.");
    }
}
