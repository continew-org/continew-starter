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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.dao.SaTokenDaoForRedisson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.auth.satoken.autoconfigure.SaTokenAutoConfiguration;

/**
 * SaToken 持久层配置测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class SaTokenDaoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SaTokenAutoConfiguration.class))
        .withPropertyValues("sa-token.extension.enabled=true");

    /**
     * 默认装配内存持久层
     */
    @Test
    @DisplayName("dao.type 缺省（DEFAULT）时装配内存持久层")
    void shouldAssembleDefaultDao() {
        runner.run(context -> assertThat(context.getBean(SaTokenDao.class))
            .isInstanceOf(SaTokenDaoDefaultImpl.class));
    }

    /**
     * REDIS 类型装配 Redisson 持久层
     */
    @Test
    @DisplayName("dao.type=REDIS 时装配 Redisson 持久层")
    void shouldAssembleRedisDao() {
        runner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .withPropertyValues("sa-token.extension.dao.type=redis")
            .run(context -> assertThat(context.getBean(SaTokenDao.class))
                .isInstanceOf(SaTokenDaoForRedisson.class));
    }

    /**
     * CUSTOM 类型缺少 SaTokenDao Bean 时启动失败
     */
    @Test
    @DisplayName("dao.type=CUSTOM 且缺少 SaTokenDao Bean 时启动失败")
    void shouldFailWhenCustomDaoMissing() {
        runner.withPropertyValues("sa-token.extension.dao.type=custom")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
            });
    }
}
