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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.config.TenantDataSource;

import java.util.stream.Stream;

/**
 * {@link TenantContextHolder} 上下文存取与启用状态测试（隔离级别装配语义见 {@link TenantContextHolderTest}）。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantContextHolderApiTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("上下文设置后读取一致")
    void shouldRoundTripContext() {
        TenantContext context = new TenantContext();
        context.setTenantId(1L);
        TenantContextHolder.setContext(context);
        assertThat(TenantContextHolder.getContext()).isSameAs(context);
    }

    @Test
    @DisplayName("无上下文时租户 ID 与数据源均为空")
    void shouldReturnNullWithoutContext() {
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.getDataSource()).isNull();
    }

    @Test
    @DisplayName("上下文内的租户 ID 与数据源透传返回")
    void shouldPassThroughTenantIdAndDataSource() {
        TenantContext context = new TenantContext();
        context.setTenantId(9L);
        TenantDataSource dataSource = new TenantDataSource();
        context.setDataSource(dataSource);
        TenantContextHolder.setContext(context);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(9L);
        assertThat(TenantContextHolder.getDataSource()).isSameAs(dataSource);
    }

    @Test
    @DisplayName("忽略标记默认为 false，设置后为 true")
    void shouldDefaultIgnoreToFalse() {
        assertThat(TenantContextHolder.isIgnore()).isFalse();
        TenantContextHolder.setIgnore(true);
        assertThat(TenantContextHolder.isIgnore()).isTrue();
    }

    @Test
    @DisplayName("清除后上下文与忽略标记全部归空")
    void shouldClearContextAndIgnore() {
        TenantContext context = new TenantContext();
        context.setTenantId(1L);
        TenantContextHolder.setContext(context);
        TenantContextHolder.setIgnore(true);
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getContext()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @ParameterizedTest(name = "[{index}] TenantProperties {0} 时启用状态判定正确")
    @MethodSource("tenantEnabledScenarios")
    void shouldJudgeTenantEnabledByProperties(String scenario,
        TenantProperties properties,
        boolean expectedEnabled) {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            if (properties == null) {
                springUtil.when(() -> SpringUtil.getBean(TenantProperties.class))
                    .thenThrow(new NoSuchBeanDefinitionException(TenantProperties.class));
            } else {
                springUtil.when(() -> SpringUtil.getBean(TenantProperties.class))
                    .thenReturn(properties);
            }
            assertThat(TenantContextHolder.isTenantEnabled()).isEqualTo(expectedEnabled);
            assertThat(TenantContextHolder.isTenantDisabled()).isNotEqualTo(expectedEnabled);
        }
    }

    /**
     * 启用状态判定场景：装配与否、enabled 开关各取向。
     *
     * @return 场景名、配置实例（null 表示未装配）、期望启用状态
     */
    private static Stream<Arguments> tenantEnabledScenarios() {
        TenantProperties enabled = new TenantProperties();
        TenantProperties disabled = new TenantProperties();
        disabled.setEnabled(false);
        return Stream.of(arguments("已装配且 enabled=true", enabled, true), arguments(
            "已装配且 enabled=false", disabled, false), arguments("未装配", null, false));
    }
}
