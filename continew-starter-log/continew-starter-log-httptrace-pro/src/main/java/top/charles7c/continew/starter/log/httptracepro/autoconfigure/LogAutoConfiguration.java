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

package top.charles7c.continew.starter.log.httptracepro.autoconfigure;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.charles7c.continew.starter.log.common.dao.LogDao;
import top.charles7c.continew.starter.log.common.dao.impl.LogDaoDefaultImpl;
import top.charles7c.continew.starter.log.httptracepro.handler.LogFilter;
import top.charles7c.continew.starter.log.httptracepro.handler.LogInterceptor;

/**
 * 日志自动配置
 *
 * @author Charles7c
 * @since 1.1.0
 */
@Slf4j
@Configuration
@ConditionalOnEnabledLog
@RequiredArgsConstructor
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LogAutoConfiguration implements WebMvcConfigurer {

    private final LogProperties logProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor(logDao(), logProperties));
    }

    /**
     * 日志过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public LogFilter logFilter() {
        return new LogFilter(logProperties);
    }

    /**
     * 日志持久层接口
     */
    @Bean
    @ConditionalOnMissingBean
    public LogDao logDao() {
        return new LogDaoDefaultImpl();
    }

    @PostConstruct
    public void postConstruct() {
        log.info("[ContiNew Starter] - Auto Configuration 'Log-HttpTracePro' completed initialization.");
    }
}
