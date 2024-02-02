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

package top.charles7c.continew.starter.captcha.behavior.autoconfigure.cache;

import cn.hutool.extra.spring.SpringUtil;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaCacheServiceMemImpl;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import jakarta.annotation.PostConstruct;
import org.redisson.client.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import top.charles7c.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration;
import top.charles7c.continew.starter.captcha.behavior.enums.StorageType;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

/**
 * 行为验证码缓存配置
 *
 * @author Bull-BCLS
 * @author Charles7c
 * @since 1.1.0
 */
public class BehaviorCaptchaCacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BehaviorCaptchaCacheConfiguration.class);

    private BehaviorCaptchaCacheConfiguration() {
    }

    /**
     * 自定义缓存实现-默认（内存）
     */
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    @ConditionalOnProperty(name = PropertiesConstants.CAPTCHA_BEHAVIOR + ".cache-type", havingValue = "default", matchIfMissing = true)
    public static class Default {
        @PostConstruct
        public void postConstruct() {
            CaptchaServiceFactory.cacheService.put(StorageType.DEFAULT.name()
                .toLowerCase(), new CaptchaCacheServiceMemImpl());
            log.debug("[ContiNew Starter] - Auto Configuration 'Captcha-Behavior-Cache-Default' completed initialization.");
        }
    }

    /**
     * 自定义缓存实现-Redis
     */
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    @ConditionalOnClass(RedisClient.class)
    @AutoConfigureBefore(RedissonAutoConfiguration.class)
    @ConditionalOnProperty(name = PropertiesConstants.CAPTCHA_BEHAVIOR + ".cache-type", havingValue = "redis")
    public static class Redis {
        @PostConstruct
        public void postConstruct() {
            CaptchaServiceFactory.cacheService.put(StorageType.REDIS.name()
                .toLowerCase(), new BehaviorCaptchaCacheServiceImpl());
            log.debug("[ContiNew Starter] - Auto Configuration 'Captcha-Behavior-Cache-Redis' completed initialization.");
        }
    }

    /**
     * 自定义缓存实现
     */
    @ConditionalOnProperty(name = PropertiesConstants.CAPTCHA_BEHAVIOR + ".cache-type", havingValue = "custom")
    public static class Custom {
        @Bean
        @ConditionalOnMissingBean
        public CaptchaCacheService captchaCacheService() {
            if (log.isErrorEnabled()) {
                log.error("Consider defining a bean of type '{}' in your configuration.", ResolvableType
                    .forClass(CaptchaCacheService.class));
            }
            throw new NoSuchBeanDefinitionException(CaptchaCacheService.class);
        }

        @PostConstruct
        public void postConstruct() {
            CaptchaServiceFactory.cacheService.put(StorageType.CUSTOM.name().toLowerCase(), SpringUtil
                .getBean(CaptchaCacheService.class));
            log.debug("[ContiNew Starter] - Auto Configuration 'Captcha-Behavior-Cache-Custom' completed initialization.");
        }
    }
}
