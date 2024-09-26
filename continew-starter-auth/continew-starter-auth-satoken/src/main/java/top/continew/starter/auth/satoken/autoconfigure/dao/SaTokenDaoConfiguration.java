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

package top.continew.starter.auth.satoken.autoconfigure.dao;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
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
import top.continew.starter.cache.redisson.autoconfigure.RedissonAutoConfiguration;

/**
 * SaToken 持久层配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class SaTokenDaoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SaTokenDaoConfiguration.class);

    private SaTokenDaoConfiguration() {
    }

    /**
     * 自定义持久层实现-默认（内存）
     */
    @ConditionalOnMissingBean(SaTokenDao.class)
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "default", matchIfMissing = true)
    public static class Default {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'SaToken-Dao-Default' completed initialization.");
        }

        @Bean
        public SaTokenDao saTokenDao() {
            return new SaTokenDaoDefaultImpl();
        }
    }

    /**
     * 自定义持久层实现-Redis（默认）
     */
    @ConditionalOnMissingBean(SaTokenDao.class)
    @ConditionalOnClass(RedisClient.class)
    @AutoConfigureBefore(RedissonAutoConfiguration.class)
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "redis")
    public static class Redis {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'SaToken-Dao-Redis' completed initialization.");
        }

        @Bean
        public SaTokenDao saTokenDao() {
            return new SaTokenDaoRedisDefaultImpl();
        }
    }

    /**
     * 自定义持久层实现
     */
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "custom")
    public static class Custom {
        @Bean
        @ConditionalOnMissingBean
        public SaTokenDao saTokenDao() {
            if (log.isErrorEnabled()) {
                log.error("Consider defining a bean of type '{}' in your configuration.", ResolvableType
                    .forClass(SaTokenDao.class));
            }
            throw new NoSuchBeanDefinitionException(SaTokenDao.class);
        }
    }
}