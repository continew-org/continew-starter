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

package top.continew.starter.extension.tenant.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import top.continew.starter.extension.tenant.TenantDataSourceHandler;
import top.continew.starter.extension.tenant.autoconfigure.TenantProperties;
import top.continew.starter.extension.tenant.config.TenantDataSource;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContext;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link TenantUtils} 租户执行工具测试。
 *
 * <p>
 * {@code TenantContextHolder} 的启用状态经 {@code SpringUtils} 委托 hutool {@code SpringUtil}
 * 查询配置，因此统一以静态模拟 {@link SpringUtil} 隔离容器。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantUtilsTest {

    private final TenantProvider tenantProvider = mock(TenantProvider.class);

    private final TenantDataSourceHandler dataSourceHandler = mock(TenantDataSourceHandler.class);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("租户未启用时直接执行业务逻辑，不查询租户提供者")
    void shouldRunDirectlyWhenTenantDisabled() {
        AtomicBoolean executed = new AtomicBoolean(false);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantDisabled(springUtil);
            TenantUtils.execute(9L, () -> executed.set(true));
            assertThat(executed.get()).isTrue();
            verifyNoInteractions(tenantProvider);
        }
        assertThat(TenantContextHolder.getContext()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("租户启用且行级隔离：以目标租户执行且不触碰数据源，结束后恢复原上下文")
    void shouldExecuteWithinTargetTenantAndRestoreForLineLevel() {
        TenantContext oldContext = new TenantContext();
        oldContext.setTenantId(1L);
        TenantContextHolder.setContext(oldContext);
        TenantContext newContext = newTenantContext(TenantIsolationLevel.LINE);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantEnabled(springUtil);
            springUtil.when(() -> SpringUtil.getBean(TenantProvider.class))
                .thenReturn(tenantProvider);
            springUtil.when(() -> SpringUtil.getBean(TenantDataSourceHandler.class))
                .thenReturn(dataSourceHandler);
            when(tenantProvider.getByTenantId("9", false)).thenReturn(newContext);
            AtomicBoolean insideTarget = new AtomicBoolean(false);
            TenantUtils.execute(9L, () -> insideTarget.set(Long.valueOf(9L)
                .equals(TenantContextHolder.getTenantId()) && !TenantContextHolder.isIgnore()));
            assertThat(insideTarget.get()).isTrue();
            verify(tenantProvider).getByTenantId("9", false);
            verifyNoInteractions(dataSourceHandler);
        }
        assertThat(TenantContextHolder.getContext()).isSameAs(oldContext);
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("租户启用且数据源级隔离：执行前切换数据源，执行后轮询恢复")
    void shouldChangeDataSourceAndPollForDatasourceLevel() {
        TenantDataSource dataSource = new TenantDataSource();
        TenantContext newContext = newTenantContext(TenantIsolationLevel.DATASOURCE);
        newContext.setDataSource(dataSource);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantEnabled(springUtil);
            springUtil.when(() -> SpringUtil.getBean(TenantProvider.class))
                .thenReturn(tenantProvider);
            springUtil.when(() -> SpringUtil.getBean(TenantDataSourceHandler.class))
                .thenReturn(dataSourceHandler);
            when(tenantProvider.getByTenantId("9", false)).thenReturn(newContext);
            TenantUtils.execute(9L, () -> {
            });
            verify(dataSourceHandler).changeDataSource(dataSource);
            verify(dataSourceHandler).poll();
        }
    }

    @Test
    @DisplayName("业务逻辑抛异常时仍恢复原上下文与忽略标记，数据源级隔离仍轮询恢复")
    void shouldRestoreContextAndPollWhenRunnableFails() {
        TenantContext oldContext = new TenantContext();
        oldContext.setTenantId(1L);
        TenantContextHolder.setContext(oldContext);
        TenantContextHolder.setIgnore(true);
        TenantContext newContext = newTenantContext(TenantIsolationLevel.DATASOURCE);
        newContext.setDataSource(new TenantDataSource());
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantEnabled(springUtil);
            springUtil.when(() -> SpringUtil.getBean(TenantProvider.class))
                .thenReturn(tenantProvider);
            springUtil.when(() -> SpringUtil.getBean(TenantDataSourceHandler.class))
                .thenReturn(dataSourceHandler);
            when(tenantProvider.getByTenantId("9", false)).thenReturn(newContext);
            assertThatThrownBy(() -> TenantUtils.execute(9L, () -> {
                throw new IllegalStateException("boom");
            })).isInstanceOf(IllegalStateException.class).hasMessage("boom");
            verify(dataSourceHandler).changeDataSource(any(TenantDataSource.class));
            verify(dataSourceHandler).poll();
        }
        assertThat(TenantContextHolder.getContext()).isSameAs(oldContext);
        assertThat(TenantContextHolder.isIgnore()).isTrue();
    }

    @Test
    @DisplayName("租户未启用时忽略执行直接运行且不改忽略标记")
    void shouldRunDirectlyWhenTenantDisabledForExecuteIgnore() {
        AtomicBoolean executed = new AtomicBoolean(false);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantDisabled(springUtil);
            TenantUtils.executeIgnore(() -> executed.set(true));
            assertThat(executed.get()).isTrue();
        }
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("已处于忽略状态时直接执行，结束后保持忽略标记（锁定当前行为：早退分支不重置标记）")
    void shouldKeepIgnoreWhenAlreadyIgnored() {
        TenantContextHolder.setIgnore(true);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantEnabled(springUtil);
            AtomicBoolean executed = new AtomicBoolean(false);
            TenantUtils.executeIgnore(() -> executed.set(true));
            assertThat(executed.get()).isTrue();
        }
        assertThat(TenantContextHolder.isIgnore()).isTrue();
    }

    @Test
    @DisplayName("租户启用且未忽略：执行期间忽略生效，结束后恢复 false")
    void shouldIgnoreDuringExecutionAndRestore() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantEnabled(springUtil);
            AtomicBoolean ignoredInside = new AtomicBoolean(false);
            TenantUtils.executeIgnore(() -> ignoredInside.set(TenantContextHolder.isIgnore()));
            assertThat(ignoredInside.get()).isTrue();
        }
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("忽略执行期间业务异常仍恢复忽略标记为 false")
    void shouldRestoreIgnoreWhenRunnableFails() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantEnabled(springUtil);
            assertThatThrownBy(() -> TenantUtils.executeIgnore(() -> {
                throw new IllegalStateException("boom");
            })).isInstanceOf(IllegalStateException.class).hasMessage("boom");
        }
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("租户启用时执行传入 null 租户 ID 抛 IllegalArgumentException")
    void shouldThrowIllegalArgumentWhenTenantIdIsNull() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantEnabled(springUtil);
            AtomicBoolean executed = new AtomicBoolean(false);
            assertThatThrownBy(() -> TenantUtils.execute(null, () -> executed.set(true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("租户 ID");
            assertThat(executed.get()).isFalse();
        }
        assertThat(TenantContextHolder.getContext()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("execute 与 executeIgnore 传入 null 业务逻辑抛 IllegalArgumentException")
    void shouldThrowIllegalArgumentWhenRunnableIsNull() {
        assertThatThrownBy(() -> TenantUtils.execute(9L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("runnable");
        assertThatThrownBy(() -> TenantUtils.executeIgnore(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("runnable");
    }

    @Test
    @DisplayName("租户未启用时执行传入 null 租户 ID 仍直接执行业务逻辑")
    void shouldRunDirectlyWhenTenantDisabledAndTenantIdIsNull() {
        AtomicBoolean executed = new AtomicBoolean(false);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            stubTenantDisabled(springUtil);
            TenantUtils.execute(null, () -> executed.set(true));
            assertThat(executed.get()).isTrue();
        }
        assertThat(TenantContextHolder.getContext()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    /**
     * 构造目标租户上下文
     *
     * @param level 隔离级别
     * @return 租户 ID 为 9 的上下文
     */
    private TenantContext newTenantContext(TenantIsolationLevel level) {
        TenantContext context = new TenantContext();
        context.setTenantId(9L);
        context.setIsolationLevel(level);
        return context;
    }

    /**
     * 静态模拟租户启用：配置已装配且默认开启
     *
     * @param springUtil 静态模拟句柄
     */
    private void stubTenantEnabled(MockedStatic<SpringUtil> springUtil) {
        springUtil.when(() -> SpringUtil.getBean(TenantProperties.class))
            .thenReturn(new TenantProperties());
    }

    /**
     * 静态模拟租户未启用：配置类未装配
     *
     * @param springUtil 静态模拟句柄
     */
    private void stubTenantDisabled(MockedStatic<SpringUtil> springUtil) {
        springUtil.when(() -> SpringUtil.getBean(TenantProperties.class))
            .thenThrow(new NoSuchBeanDefinitionException(TenantProperties.class));
    }
}
