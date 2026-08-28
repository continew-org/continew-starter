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

package top.continew.starter.data.autoconfigure.idgenerator;

import cn.hutool.core.net.NetUtil;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import jakarta.annotation.PostConstruct;
import me.ahoo.cosid.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import top.continew.starter.data.idgenerator.MyBatisPlusCosIdIdentifierGenerator;

/**
 * MyBatis Plus ID 生成器配置
 *
 * @author Charles7c
 * @since 1.4.0
 */
@Configuration(proxyBeanMethods = false)
public class MyBatisPlusIdGeneratorConfiguration {

    private static final Logger log =
        LoggerFactory.getLogger(MyBatisPlusIdGeneratorConfiguration.class);

    /**
     * 使用雪花算法（使用网卡信息绑定雪花生成器，防止集群雪花 ID 重复）
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    @ConditionalOnProperty(name = "mybatis-plus.extension.id-generator.type",
        havingValue = "default", matchIfMissing = true)
    static class Default {

        @Bean
        public IdentifierGenerator identifierGenerator() {
            log.debug(
                "[ContiNew Starter] - Auto Configuration 'MyBatis Plus-IdGenerator-Default' completed initialization.");
            return new DefaultIdentifierGenerator(NetUtil.getLocalhost());
        }
    }

    /**
     * 使用 CosID
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    @ConditionalOnClass(IdGenerator.class)
    @ConditionalOnProperty(name = "mybatis-plus.extension.id-generator.type", havingValue = "cosid")
    static class CosId {

        @Bean
        public IdentifierGenerator identifierGenerator() {
            log.debug(
                "[ContiNew Starter] - Auto Configuration 'MyBatis Plus-IdGenerator-CosId' completed initialization.");
            return new MyBatisPlusCosIdIdentifierGenerator();
        }
    }

    /**
     * 自定义
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "mybatis-plus.extension.id-generator.type",
        havingValue = "custom")
    static class Custom {

        @Bean
        @ConditionalOnMissingBean
        public IdentifierGenerator identifierGenerator() {
            if (log.isErrorEnabled()) {
                log.error(
                    "[ContiNew Starter] - When 'mybatis-plus.extension.id-generator.type' is 'custom', you must provide a bean of type '{}' in your configuration.",
                    ResolvableType
                        .forClass(IdentifierGenerator.class));
            }
            throw new NoSuchBeanDefinitionException(IdentifierGenerator.class);
        }

        @PostConstruct
        public void postConstruct() {
            log.debug(
                "[ContiNew Starter] - Auto Configuration 'MyBatis Plus-IdGenerator-Custom' completed initialization.");
        }
    }
}
