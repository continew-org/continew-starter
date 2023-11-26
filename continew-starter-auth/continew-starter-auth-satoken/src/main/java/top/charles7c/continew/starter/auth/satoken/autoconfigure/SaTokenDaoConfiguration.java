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

package top.charles7c.continew.starter.auth.satoken.autoconfigure;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.hutool.core.util.ReflectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.charles7c.continew.starter.auth.satoken.impl.SaTokenDaoRedisImpl;
import top.charles7c.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration;

/**
 * SaToken 持久层配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class SaTokenDaoConfiguration {

    /**
     * 自定义持久层实现类-Redis
     */
    @ConditionalOnClass(RedisClient.class)
    @ConditionalOnMissingBean(SaTokenDao.class)
    @AutoConfigureBefore(RedissonAutoConfiguration.class)
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "redis")
    static class Redis {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'SaToken-SaTokenDao-Redis' completed initialization.");
        }

        @Bean
        public SaTokenDao saTokenDao() {
            return new SaTokenDaoRedisImpl();
        }
    }

    /**
     * 自定义持久层实现类-自定义
     */
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "custom")
    static class Custom {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'SaToken-SaTokenDao-Custom' completed initialization.");
        }

        @Bean
        public SaTokenDao saTokenDao(SaTokenExtensionProperties properties) {
            return ReflectUtil.newInstance(properties.getDao().getImpl());
        }
    }
}