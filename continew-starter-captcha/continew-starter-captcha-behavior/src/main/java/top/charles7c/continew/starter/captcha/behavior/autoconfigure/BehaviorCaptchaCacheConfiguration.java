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

package top.charles7c.continew.starter.captcha.behavior.autoconfigure;

import cn.hutool.core.util.ReflectUtil;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaCacheServiceMemImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.charles7c.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration;
import top.charles7c.continew.starter.captcha.behavior.impl.BehaviorCaptchaCacheServiceImpl;

/**
 * 行为验证码缓存配置
 *
 * @author Bull-BCLS
 * @since 1.1.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
abstract class BehaviorCaptchaCacheConfiguration {

    /**
     * 自定义缓存实现类-Redis
     */
    @ConditionalOnClass(RedisClient.class)
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    @AutoConfigureBefore(RedissonAutoConfiguration.class)
    @ConditionalOnProperty(name = "continew-starter.captcha.behavior.cache-type", havingValue = "redis")
    static class Redis {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'Behavior-CaptchaCache-Redis' completed initialization.");
        }

        @Bean
        public CaptchaCacheService captchaCacheService() {
            return new BehaviorCaptchaCacheServiceImpl();
        }
    }

    /**
     * 自定义缓存实现类-内存
     */
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    @ConditionalOnProperty(name = "continew-starter.captcha.behavior.cache-type", havingValue = "local")
    static class Local {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'Behavior-CaptchaCache-Local' completed initialization.");
        }

        @Bean
        public CaptchaCacheService captchaCacheService() {
            return new CaptchaCacheServiceMemImpl();
        }
    }

    /**
     * 自定义缓存实现类-自定义
     */
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    @ConditionalOnProperty(name = "continew-starter.captcha.behavior.cache-type", havingValue = "custom")
    static class Custom {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'Behavior-CaptchaCache-Custom' completed initialization.");
        }

        @Bean
        public CaptchaCacheService captchaCacheService(BehaviorCaptchaProperties properties) {
            return ReflectUtil.newInstance(properties.getCacheImpl());
        }
    }
}
