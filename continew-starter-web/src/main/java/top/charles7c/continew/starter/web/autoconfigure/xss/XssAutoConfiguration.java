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

package top.charles7c.continew.starter.web.autoconfigure.xss;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

/**
 * XSS 过滤自动配置
 *
 * @author whhya
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(XssProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.XSS, name = PropertiesConstants.ENABLED, havingValue = "true")
public class XssAutoConfiguration {

    /**
     * XSS 过滤器配置
     */
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilter(XssProperties xssProperties) {
        FilterRegistrationBean<XssFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new XssFilter(xssProperties));
        return registrationBean;
    }
}
