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

package top.continew.starter.test.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import top.continew.starter.test.support.probe.DisabledAutoConfigurationProbe;
import top.continew.starter.test.support.probe.UnconditionalAutoConfigurationProbe;
import top.continew.starter.test.support.probe.WebOnlyAutoConfigurationProbe;

import java.util.List;

/**
 * AutoConfigurationImportsAssert 自身行为的负向验证
 *
 * <p>探针类均为独立顶层类型，标注 {@code @AutoConfiguration} 且不登记进任何 imports 资源。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AutoConfigurationImportsAssertTest {

    private static final String PROBE_PACKAGE = "top.continew.starter.test.support.probe";

    private static final String BOOT_INFO_PACKAGE = "org.springframework.boot.autoconfigure.info";

    /**
     * 条件注解不得影响扫描：默认关闭、Web-only、无条件三个未登记探针都必须被报告。
     *
     * <p>ClassPathScanningCandidateComponentProvider 会在扫描阶段用条件评估器提前排除前两类，
     * 探针因此覆盖带条件与无条件两种形态。</p>
     */
    @Test
    void findUnregisteredShouldReportAllProbesRegardlessOfConditions() {
        List<String> unregistered = AutoConfigurationImportsAssert
            .findUnregistered(PROBE_PACKAGE);
        assertTrue(unregistered.contains(DisabledAutoConfigurationProbe.class.getName()));
        assertTrue(unregistered.contains(WebOnlyAutoConfigurationProbe.class.getName()));
        assertTrue(unregistered.contains(UnconditionalAutoConfigurationProbe.class.getName()));
    }

    /**
     * 扫描不到任何候选的基础包名必须抛异常，不得静默当作注册完整。
     */
    @Test
    void findUnregisteredShouldRejectPackageWithNoCandidates() {
        assertThrows(IllegalStateException.class, () -> AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.no.such.package"));
    }

    /**
     * 对确实已登记的自动配置包，findUnregistered 应返回空且 scannedCandidates 非空。
     */
    @Test
    void findUnregisteredShouldReturnEmptyForRegisteredPackage() {
        assertTrue(AutoConfigurationImportsAssert.findUnregistered(BOOT_INFO_PACKAGE)
            .isEmpty());
        assertFalse(AutoConfigurationImportsAssert.scannedCandidates(BOOT_INFO_PACKAGE)
            .isEmpty());
    }

    /**
     * isRegistered 应区分已登记类型与未登记探针，覆盖仅靠 imports 资源显式登记的场景。
     */
    @Test
    void isRegisteredShouldReflectImportsResources() {
        assertTrue(AutoConfigurationImportsAssert
            .isRegistered(ProjectInfoAutoConfiguration.class));
        assertFalse(AutoConfigurationImportsAssert
            .isRegistered(UnconditionalAutoConfigurationProbe.class));
    }
}
