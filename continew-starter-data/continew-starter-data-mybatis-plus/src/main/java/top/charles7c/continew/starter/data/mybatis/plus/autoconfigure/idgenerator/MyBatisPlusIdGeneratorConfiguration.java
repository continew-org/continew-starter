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

package top.charles7c.continew.starter.data.mybatis.plus.autoconfigure.idgenerator;

import cn.hutool.core.net.NetUtil;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import me.ahoo.cosid.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;

/**
 * MyBatis ID 生成器配置
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class MyBatisPlusIdGeneratorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MyBatisPlusIdGeneratorConfiguration.class);

    private MyBatisPlusIdGeneratorConfiguration() {
    }

    /**
     * 自定义 ID 生成器-默认（雪花算法，使用网卡信息绑定雪花生成器，防止集群雪花 ID 重复）
     */
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    @ConditionalOnProperty(name = "mybatis-plus.extension.id-generator.type", havingValue = "default", matchIfMissing = true)
    public static class Default {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'MyBatis Plus-IdGenerator-Default' completed initialization.");
        }

        @Bean
        public IdentifierGenerator identifierGenerator() {
            return new DefaultIdentifierGenerator(NetUtil.getLocalhost());
        }
    }

    /**
     * 自定义 ID 生成器-CosId
     */
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    @ConditionalOnClass(IdGenerator.class)
    @ConditionalOnProperty(name = "mybatis-plus.extension.id-generator.type", havingValue = "cosid")
    public static class CosId {
        static {
            log.debug("[ContiNew Starter] - Auto Configuration 'MyBatis Plus-IdGenerator-CosId' completed initialization.");
        }

        @Bean
        public IdentifierGenerator identifierGenerator() {
            return new MyBatisPlusCosIdIdentifierGenerator();
        }
    }

    /**
     * 自定义 ID 生成器
     */
    @ConditionalOnProperty(name = "mybatis-plus.extension.id-generator.type", havingValue = "custom")
    public static class Custom {
        @Bean
        @ConditionalOnMissingBean
        public IdentifierGenerator identifierGenerator() {
            if (log.isErrorEnabled()) {
                log.error("Consider defining a bean of type '{}' in your configuration.", ResolvableType
                    .forClass(IdentifierGenerator.class));
            }
            throw new NoSuchBeanDefinitionException(IdentifierGenerator.class);
        }
    }
}