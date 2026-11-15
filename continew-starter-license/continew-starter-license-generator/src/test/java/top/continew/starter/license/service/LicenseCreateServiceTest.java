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

package top.continew.starter.license.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.license.model.LicenseExtraModel;

/**
 * {@link LicenseCreateService} 单元测试：单例语义与服务器信息采集（真实环境信息）。
 *
 * <p>generateLicense 全链路依赖 keystore 证书与文件打包，超出单元测试范围，仅覆盖
 * 无外部依赖的公开入口。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseCreateServiceTest {

    /**
     * getInstance 应返回同一单例实例。
     */
    @Test
    @DisplayName("getInstance 单例")
    void shouldReturnSameSingleton() {
        assertThat(LicenseCreateService.getInstance()).isSameAs(LicenseCreateService.getInstance());
    }

    /**
     * getServerInfo 应采集到服务器信息（IP/MAC 非空；CPU/主板序列号依赖容器权限，
     * CI 环境可能为空串，仅断言非 null）。
     */
    @Test
    @DisplayName("getServerInfo 采集信息")
    void shouldCollectServerInfo() {
        LicenseExtraModel info = LicenseCreateService.getInstance().getServerInfo();

        assertThat(info).isNotNull();
        assertThat(info.getIpAddress()).isNotEmpty();
        assertThat(info.getMacAddress()).isNotEmpty();
        assertThat(info.getCpuSerial()).isNotNull();
        assertThat(info.getMainBoardSerial()).isNotNull();
    }

    /**
     * getServerInfo 重复调用应返回等价快照（IP/MAC 稳定）。
     */
    @Test
    @DisplayName("getServerInfo 重复调用稳定")
    void shouldReturnStableServerInfo() {
        LicenseExtraModel first = LicenseCreateService.getInstance().getServerInfo();
        LicenseExtraModel second = LicenseCreateService.getInstance().getServerInfo();

        assertThat(first.getIpAddress()).isEqualTo(second.getIpAddress());
        assertThat(first.getMacAddress()).isEqualTo(second.getMacAddress());
    }
}
