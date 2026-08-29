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

package top.continew.starter.core.autoconfigure.threadpool;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskScheduler}.
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.task.scheduling.extension",
    name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
class TaskSchedulingConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskSchedulingConfiguration.class);

    @Bean
    public ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizer(
        ThreadPoolExtensionProperties properties) {
        return executor -> executor.setRejectedExecutionHandler(properties.getScheduling()
            .getRejectedPolicy()
            .getRejectedExecutionHandler());
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'TaskScheduler' completed initialization.");
    }
}
