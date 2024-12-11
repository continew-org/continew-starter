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

package top.continew.starter.log.aop.autoconfigure;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.continew.starter.log.aop.annotation.ConditionalOnEnabledLog;
import top.continew.starter.log.aop.aspect.ConsoleLogAspect;
import top.continew.starter.log.aop.aspect.LogAspect;
import top.continew.starter.log.core.dao.LogDao;
import top.continew.starter.log.core.dao.impl.LogDaoDefaultImpl;

/**
 * 日志自动配置
 *
 * @author Charles7c
 * @since 1.1.0
 */
@Configuration
@ConditionalOnEnabledLog
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LogAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LogAutoConfiguration.class);
    private final LogProperties logProperties;

    public LogAutoConfiguration(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    /**
     * 记录日志切面
     *
     * @return {@link LogAspect }
     */
    @Bean
    @ConditionalOnMissingBean
    public LogAspect logAspect() {
        return new LogAspect(logDao(),logProperties);
    }

    /**
     * 控制台输出日志切面
     *
     * @return {@link LogAspect }
     */
    @Bean
    @ConditionalOnMissingBean
    public ConsoleLogAspect consoleLogAspect() {
        return new ConsoleLogAspect(logProperties);
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
        log.debug("[ContiNew Starter] - Auto Configuration 'Log-aop' completed initialization.");
    }
}
