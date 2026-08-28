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

package top.continew.starter.captcha.behavior.autoconfigure.cache;

import cn.hutool.extra.spring.SpringUtil;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaCacheServiceMemImpl;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import top.continew.starter.captcha.behavior.cache.BehaviorCaptchaCacheService;
import top.continew.starter.captcha.behavior.enums.StorageType;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * 行为验证码缓存配置
 *
 * @author Bull-BCLS
 * @author Charles7c
 * @since 1.1.0
 */
@Configuration(proxyBeanMethods = false)
public class BehaviorCaptchaCacheConfiguration {

    private static final Logger log =
        LoggerFactory.getLogger(BehaviorCaptchaCacheConfiguration.class);

    /**
     * 使用内存
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    @ConditionalOnProperty(name = PropertiesConstants.CAPTCHA_BEHAVIOR + ".cache-type",
        havingValue = "default", matchIfMissing = true)
    static class Default {

        @Bean
        public CaptchaCacheService captchaCacheService() {
            CaptchaCacheServiceMemImpl service = new CaptchaCacheServiceMemImpl();
            CaptchaServiceFactory.cacheService.put(StorageType.DEFAULT.name().toLowerCase(),
                service);
            log.debug(
                "[ContiNew Starter] - Auto Configuration 'Captcha-Behavior-Cache-Default' completed initialization.");
            return service;
        }
    }

    /**
     * 使用 Redis
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    @ConditionalOnProperty(name = PropertiesConstants.CAPTCHA_BEHAVIOR + ".cache-type",
        havingValue = "redis")
    static class Redis {

        @Bean
        public CaptchaCacheService captchaCacheService() {
            BehaviorCaptchaCacheService service = new BehaviorCaptchaCacheService();
            CaptchaServiceFactory.cacheService.put(StorageType.REDIS.name().toLowerCase(), service);
            log.debug(
                "[ContiNew Starter] - Auto Configuration 'Captcha-Behavior-Cache-Redis' completed initialization.");
            return service;
        }
    }

    /**
     * 自定义
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = PropertiesConstants.CAPTCHA_BEHAVIOR + ".cache-type",
        havingValue = "custom")
    static class Custom {

        @PostConstruct
        public void postConstruct() {
            try {
                CaptchaCacheService service = SpringUtil.getBean(CaptchaCacheService.class);
                CaptchaServiceFactory.cacheService.put(StorageType.CUSTOM.name().toLowerCase(),
                    service);
                log.debug(
                    "[ContiNew Starter] - Auto Configuration 'Captcha-Behavior-Cache-Custom' completed initialization.");
            } catch (Exception e) {
                log.error(
                    "[ContiNew Starter] - When 'continew-starter.captcha.behavior.cache-type' is 'custom', you must provide a bean of type '{}' in your configuration.",
                    ResolvableType
                        .forClass(CaptchaCacheService.class));
                throw e;
            }
        }
    }
}
