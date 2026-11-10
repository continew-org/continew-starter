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

package top.continew.starter.extension.tenant.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

/**
 * 租户上下文 Holder 测试
 *
 * <p>
 * 通过静态模拟 {@link SpringUtil} 固化隔离级别取值语义：上下文已指定时优先于全局配置且不触碰 Spring 容器，
 * 上下文未指定时取全局配置，配置类未装配则回退行级隔离。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("上下文已指定隔离级别时返回上下文值，不触碰 Spring 容器")
    void shouldReturnContextIsolationLevelWithoutTouchingContainer() {
        TenantContext context = new TenantContext();
        context.setTenantId(1L);
        context.setIsolationLevel(TenantIsolationLevel.DATASOURCE);
        TenantContextHolder.setContext(context);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(TenantProperties.class))
                .thenThrow(new NoSuchBeanDefinitionException(TenantProperties.class));
            assertThat(TenantContextHolder.getIsolationLevel())
                .isEqualTo(TenantIsolationLevel.DATASOURCE);
            // 容器访问位于兜底分支，上下文有值时不应有任何 Bean 查询发生
            springUtil.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("上下文为空且 TenantProperties 未装配时回退默认行级隔离")
    void shouldFallbackToLineWhenBeanMissing() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(TenantProperties.class))
                .thenThrow(new NoSuchBeanDefinitionException(TenantProperties.class));
            assertThat(TenantContextHolder.getIsolationLevel())
                .isEqualTo(TenantIsolationLevel.LINE);
        }
    }

    @Test
    @DisplayName("上下文为空时透传 TenantProperties 配置的隔离级别")
    void shouldPassThroughConfiguredIsolationLevel() {
        TenantProperties properties = new TenantProperties();
        properties.setIsolationLevel(TenantIsolationLevel.DATASOURCE);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(TenantProperties.class))
                .thenReturn(properties);
            assertThat(TenantContextHolder.getIsolationLevel())
                .isEqualTo(TenantIsolationLevel.DATASOURCE);
        }
    }
}
