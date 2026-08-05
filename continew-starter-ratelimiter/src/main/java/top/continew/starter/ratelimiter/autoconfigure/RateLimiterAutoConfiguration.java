```java
package top.continew.starter.ratelimiter.autoconfigure;

import top.continew.starter.ratelimiter.constant.RateLimiterConstants;
import top.continew.starter.ratelimiter.core.RateLimiterProperties;

/**
 * 限流自动配置
 *
 * @author LiQiang5433
 * @since 2.16.0-SNAPSHOT
 */
public class RateLimiterAutoConfiguration {

    /**
     * 限流属性配置
     */
    @EnableConfigurationProperties(RateLimiterProperties.class)
    @Configuration
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = RateLimiterConstants.PREFIX, value = "enabled", matchIfMissing = true)
    static class PropertiesConfiguration {

        @Bean
        @ConditionalOnMissingBean(RateLimiterProperties.class)
        public RateLimiterProperties rateLimiterProperties() {
            return new RateLimiterProperties();
        }
    }

    /**
     * 限流器 Bean
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter rateLimiter(RateLimiterProperties properties) {
        return new RedissonRateLimiter(properties);
    }

}
```