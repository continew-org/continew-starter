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

package top.continew.starter.core.autoconfigure.contentcache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.OrderedConstants;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.filter.CommonContentCachingFilter;

/**
 * 内容缓存自动配置
 *
 * @author Charles7c
 * @since 2.16.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ContentCachingProperties.class)
public class ContentCachingAutoConfiguration {

    /**
     * 内容缓存过滤器
     */
    @Bean
    public FilterRegistrationBean<CommonContentCachingFilter> contentCachingFilter(ContentCachingProperties properties) {
        FilterRegistrationBean<CommonContentCachingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CommonContentCachingFilter(properties));
        registrationBean.addUrlPatterns(StringConstants.PATH_PATTERN_CURRENT_DIR);
        registrationBean.setOrder(OrderedConstants.Filter.CONTENT_CACHING_FILTER);
        return registrationBean;
    }
}
