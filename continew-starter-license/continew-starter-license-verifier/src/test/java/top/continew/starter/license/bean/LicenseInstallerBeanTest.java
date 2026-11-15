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

package top.continew.starter.license.bean;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.schlichtherle.license.LicenseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.license.autoconfigure.LicenseVerifyProperties;
import top.continew.starter.license.exception.LicenseException;
import top.continew.starter.license.initializing.LicenseStarterInitializingBean;

import java.lang.reflect.Field;

/**
 * {@link LicenseInstallerBean} 与 {@link LicenseStarterInitializingBean} 行为单元测试。
 *
 * <p>licenseManager 私有字段经反射注入 mock，覆盖 verify/卸载/安装失败与初始化委托路径。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseInstallerBeanTest {

    private LicenseInstallerBean installerBean;
    private LicenseManager licenseManager;

    @BeforeEach
    void setUp() throws Exception {
        installerBean = new LicenseInstallerBean(new LicenseVerifyProperties());
        licenseManager = mock(LicenseManager.class);
        Field field = LicenseInstallerBean.class.getDeclaredField("licenseManager");
        field.setAccessible(true);
        field.set(installerBean, licenseManager);
    }

    /**
     * verify 应委托 licenseManager.verify()。
     */
    @Test
    @DisplayName("verify 委托 manager")
    void shouldDelegateVerifyToManager() throws Exception {
        when(licenseManager.verify()).thenReturn(null);

        installerBean.verify();

        verify(licenseManager).verify();
    }

    /**
     * manager 校验失败应转译为 LicenseException（证书认证失败）。
     */
    @Test
    @DisplayName("verify 失败转译 LicenseException")
    void shouldTranslateVerifyFailure() throws Exception {
        doThrow(new RuntimeException("签名不匹配")).when(licenseManager).verify();

        assertThatThrownBy(() -> installerBean.verify()).isInstanceOf(LicenseException.class)
            .hasMessageContaining("证书认证失败");
    }

    /**
     * licenseManager 为 null 时 verify 应直接抛出 LicenseException。
     */
    @Test
    @DisplayName("manager 为 null verify 抛异常")
    void shouldThrowWhenManagerMissing() throws Exception {
        Field field = LicenseInstallerBean.class.getDeclaredField("licenseManager");
        field.setAccessible(true);
        field.set(installerBean, null);

        assertThatThrownBy(() -> installerBean.verify()).isInstanceOf(LicenseException.class)
            .hasMessageContaining("licenseManager is null");
    }

    /**
     * uninstallLicense 应委托 manager.uninstall()。
     */
    @Test
    @DisplayName("uninstall 委托 manager")
    void shouldDelegateUninstall() throws Exception {
        installerBean.uninstallLicense();

        verify(licenseManager).uninstall();
    }

    /**
     * manager 为 null 时 uninstallLicense 应静默返回。
     */
    @Test
    @DisplayName("manager 为 null uninstall 静默")
    void shouldSkipUninstallWhenManagerMissing() throws Exception {
        Field field = LicenseInstallerBean.class.getDeclaredField("licenseManager");
        field.setAccessible(true);
        field.set(installerBean, null);

        assertThatCode(() -> installerBean.uninstallLicense()).doesNotThrowAnyException();
        verify(licenseManager, never()).uninstall();
    }

    /**
     * LicenseStarterInitializingBean 应在初始化时委托 installLicense。
     */
    @Test
    @DisplayName("InitializingBean 委托安装")
    void shouldDelegateInstallOnInit() {
        LicenseInstallerBean installer = mock(LicenseInstallerBean.class);
        LicenseStarterInitializingBean initializingBean = new LicenseStarterInitializingBean(
            installer);

        initializingBean.afterPropertiesSet();

        verify(installer).installLicense();
    }
}
