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

package top.continew.starter.idempotent.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.continew.starter.idempotent.aspect.IdempotentAspect;
import top.continew.starter.idempotent.service.IdempotentService;
import top.continew.starter.idempotent.service.impl.MemoryIdempotentServiceImpl;
import top.continew.starter.idempotent.service.impl.RedisIdempotentServiceImpl;

/**
 * 引用配置：暂定默认内存实现，扫描到启用redis 使用redis实现
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 20:03
 * @Package top.continew.starter.idempotent.config.IdempotentAutoConfiguration
 */
@Configuration
@EnableConfigurationProperties(IdempotentProperties.class)
public class IdempotentAutoConfiguration {

    @Autowired
    private IdempotentService idempotentService;

    private final IdempotentProperties properties;

    public IdempotentAutoConfiguration(IdempotentProperties properties) {
        this.properties = properties;
    }

    @Bean
    public IdempotentAspect idempotentAspect(IdempotentService idempotentService) {
        return new IdempotentAspect(idempotentService);
    }

    @Bean(name = "redisIdempotentService")
    @ConditionalOnBean(StringRedisTemplate.class)
    public IdempotentService redisIdempotentService(StringRedisTemplate redisTemplate) {
        return new RedisIdempotentServiceImpl(redisTemplate, properties.getRedisTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(IdempotentService.class)
    public IdempotentService memoryIdempotentService() {
        return new MemoryIdempotentServiceImpl(properties.getCleanInterval());
    }

    @Bean
    public IdempotentProperties idempotentProperties() {
        return new IdempotentProperties();
    }
}