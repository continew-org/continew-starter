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

package top.continew.starter.data.idgenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import me.ahoo.cosid.snowflake.SnowflakeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.continew.starter.data.autoconfigure.idgenerator.MyBatisPlusIdGeneratorConfiguration;

/**
 * MyBatis Plus ID 生成器 - CosId 装配测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class MyBatisPlusCosIdIdentifierGeneratorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MyBatisPlusIdGeneratorConfiguration.class))
        .withPropertyValues("mybatis-plus.extension.id-generator.type=cosid");

    /**
     * type=cosid 时装配 CosId 生成器并生成 ID
     */
    @Test
    @DisplayName("type=cosid 时装配 CosId 生成器并生成 ID")
    void shouldAssembleCosIdGeneratorAndGenerateId() {
        SnowflakeId snowflakeId = mock(SnowflakeId.class);
        when(snowflakeId.generate()).thenReturn(1234567890L);
        runner.withBean("__share__SnowflakeId", SnowflakeId.class, () -> snowflakeId)
            .run(context -> {
                assertThat(context).hasSingleBean(IdentifierGenerator.class);
                IdentifierGenerator generator = context.getBean(IdentifierGenerator.class);
                assertThat(generator.nextId(new Object())).isEqualTo(1234567890L);
            });
    }

    /**
     * 缺少 CosId 内置共享雪花 Bean（__share__SnowflakeId）时，生成器经 @Lazy 延迟解析，调用方在首次生成 ID 时获得明确的缺 Bean 异常
     */
    @Test
    @DisplayName("缺少 __share__SnowflakeId Bean 时生成 ID 抛缺 Bean 异常")
    void shouldThrowWhenSharedSnowflakeIdMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IdentifierGenerator.class);
            IdentifierGenerator generator = context.getBean(IdentifierGenerator.class);
            Object entity = new Object();
            assertThatThrownBy(() -> generator.nextId(entity))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        });
    }
}
