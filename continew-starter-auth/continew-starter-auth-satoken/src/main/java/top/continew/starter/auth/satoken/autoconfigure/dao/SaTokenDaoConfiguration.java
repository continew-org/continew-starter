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
import cn.dev33.satoken.dao.SaTokenDaoForRedisson;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

/**
 * SaToken 持久层配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
// 这是 Spring 配置类，需由容器实例化以处理嵌套条件配置，并非工具类，故保留隐式公有构造
@SuppressWarnings("java:S1118")
@Configuration(proxyBeanMethods = false)
public class SaTokenDaoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaTokenDaoConfiguration.class);

    /**
     * 使用内存
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(SaTokenDao.class)
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "default",
        matchIfMissing = true)
    static class Default {

        @Bean
        public SaTokenDao saTokenDao() {
            LOGGER.debug(
                "[ContiNew Starter] - Auto Configuration 'SaToken-Dao-Default' completed initialization.");
            return new SaTokenDaoDefaultImpl();
        }
    }

    /**
     * 使用 Redis
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(SaTokenDao.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "redis")
    static class Redis {

        @Bean
        public SaTokenDao saTokenDao(RedissonClient redissonClient) {
            LOGGER.debug(
                "[ContiNew Starter] - Auto Configuration 'SaToken-Dao-Redis' completed initialization.");
            return new SaTokenDaoForRedisson(redissonClient);
        }
    }

    /**
     * 自定义
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "sa-token.extension.dao.type", havingValue = "custom")
    static class Custom {

        @Bean
        @ConditionalOnMissingBean
        public SaTokenDao saTokenDao() {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(
                    "[ContiNew Starter] - When 'sa-token.extension.dao.type' is 'custom', you must provide a bean of type '{}'.",
                    ResolvableType
                        .forClass(SaTokenDao.class));
            }
            throw new NoSuchBeanDefinitionException(SaTokenDao.class);
        }

        @PostConstruct
        public void postConstruct() {
            LOGGER.debug(
                "[ContiNew Starter] - Auto Configuration 'SaToken-Dao-Custom' completed initialization.");
        }
    }
}
