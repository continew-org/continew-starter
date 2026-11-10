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

package top.continew.starter.extension.tenant.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.extension.tenant.annotation.TenantIgnore;
import top.continew.starter.extension.tenant.context.TenantContextHolder;

/**
 * {@link TenantIgnoreAspect} 租户忽略切面测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TenantIgnoreAspectTest {

    private final TenantIgnoreAspect aspect = new TenantIgnoreAspect();

    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("已处于忽略状态时直接放行，不改写忽略标记（锁定当前行为：放行分支不重置标记）")
    void shouldProceedWithoutTouchingIgnoreWhenAlreadyIgnored() throws Throwable {
        TenantContextHolder.setIgnore(true);
        when(joinPoint.proceed()).thenReturn("ok");
        assertThat(aspect.around(joinPoint, tenantIgnore())).isEqualTo("ok");
        verify(joinPoint).proceed();
        assertThat(TenantContextHolder.isIgnore()).isTrue();
    }

    @Test
    @DisplayName("未忽略时执行期间设置忽略标记，结束后恢复 false")
    void shouldIgnoreDuringProceedAndRestore() throws Throwable {
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.isIgnore()).isTrue();
            return "ok";
        });
        assertThat(aspect.around(joinPoint, tenantIgnore())).isEqualTo("ok");
        verify(joinPoint).proceed();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    @DisplayName("业务逻辑抛异常时忽略标记仍恢复 false，异常向上传播")
    void shouldRestoreIgnoreWhenProceedFails() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        assertThatThrownBy(() -> aspect.around(joinPoint, tenantIgnore()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    /**
     * 构造切面绑定的注解实例（切面方法体不读取注解属性，仅需非空实例）
     *
     * @return TenantIgnore 注解实例
     */
    private static TenantIgnore tenantIgnore() {
        return TenantIgnore.class.cast(AnnotatedTarget.class
            .getDeclaredMethods()[0].getAnnotation(TenantIgnore.class));
    }

    /**
     * 注解取样载体
     */
    static class AnnotatedTarget {

        /**
         * 取样方法
         */
        @TenantIgnore
        public void action() {
        }
    }
}
