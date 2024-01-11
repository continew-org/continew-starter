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

package top.charles7c.continew.starter.auth.justauth.autoconfigure;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.cache.AuthStateCache;
import org.redisson.client.RedisClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.charles7c.continew.starter.auth.justauth.impl.JustAuthStateCacheRedisImpl;

/**
 * JustAuth 自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(before = com.xkcoding.justauth.autoconfigure.JustAuthAutoConfiguration.class)
@ConditionalOnProperty(prefix = "justauth", value = "enabled", havingValue = "true", matchIfMissing = true)
public class JustAuthAutoConfiguration {

    /**
     * 自定义 State 缓存实现
     */
    @Bean
    @ConditionalOnClass(RedisClient.class)
    @ConditionalOnProperty(prefix = "justauth.cache", value = "type", havingValue = "redis")
    public AuthStateCache authStateCache() {
        JustAuthStateCacheRedisImpl impl = new JustAuthStateCacheRedisImpl();
        log.debug("[ContiNew Starter] - Auto Configuration 'JustAuth-AuthStateCache-Redis' completed initialization.");
        return impl;
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'JustAuth' completed initialization.");
    }
}