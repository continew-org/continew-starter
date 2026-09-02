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

package top.continew.starter.core.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import top.continew.starter.core.ContiNewStarterVersion;

/**
 * ContiNew Starter 版本日志打印
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class ContiNewStarterVersionLogger implements ApplicationListener<ApplicationPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
        boolean isLogStartupInfo =
            environment.getProperty("spring.main.log-startup-info", Boolean.class, true);
        if (!isLogStartupInfo) {
            return;
        }

        Class<?> mainApplicationClass = event.getSpringApplication().getMainApplicationClass();
        Logger log = LoggerFactory.getLogger(mainApplicationClass);
        if (log.isDebugEnabled()) {
            log.debug("Running with ContiNew Starter v{}", ContiNewStarterVersion.VERSION);
        }
    }
}
