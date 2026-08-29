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

package top.continew.starter.security.xss.autoconfigure;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.security.xss.filter.XssFilter;

/**
 * XSS 过滤自动配置
 *
 * @author whhya
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(XssProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.SECURITY_XSS,
    name = PropertiesConstants.ENABLED, havingValue = "true")
public class XssAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(XssAutoConfiguration.class);

    /**
     * XSS 过滤器
     */
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilter(XssProperties xssProperties) {
        FilterRegistrationBean<XssFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new XssFilter(xssProperties));
        registrationBean.setOrder(OrderedConstants.Filter.XSS_FILTER);
        registrationBean.addUrlPatterns(StringConstants.PATH_PATTERN_CURRENT_DIR);
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        return registrationBean;
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'Security-XSS' completed initialization.");
    }
}
