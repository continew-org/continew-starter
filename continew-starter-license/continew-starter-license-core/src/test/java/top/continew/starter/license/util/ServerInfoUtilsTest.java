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

package top.continew.starter.license.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.license.exception.LicenseException;
import top.continew.starter.license.model.LicenseExtraModel;

import java.util.Set;

/**
 * {@link ServerInfoUtils} 服务器信息采集测试，断言不绑定具体平台与网络环境特征。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ServerInfoUtilsTest {

    @Test
    @DisplayName("getServerTempPath 返回当前工作目录")
    void shouldReturnWorkingDirectoryAsTempPath() {
        assertThat(ServerInfoUtils.getServerTempPath()).isEqualTo(System.getProperty("user.dir"));
    }

    @Test
    @DisplayName("getIpAddress 返回非 null 集合，元素非空白")
    void shouldCollectIpAddressSet() throws Exception {
        Set<String> ipAddresses = ServerInfoUtils.getIpAddress();
        assertThat(ipAddresses).isNotNull();
        for (String ipAddress : ipAddresses) {
            assertThat(ipAddress).isNotBlank();
        }
    }

    @Test
    @DisplayName("getMacAddress 正常时返回 MAC 格式集合，硬件地址不可用时报 NPE")
    void shouldCollectMacAddressSet() throws Exception {
        Throwable thrown = catchThrowable(ServerInfoUtils::getMacAddress);
        if (thrown == null) {
            Set<String> macAddresses = ServerInfoUtils.getMacAddress();
            assertThat(macAddresses).isNotNull();
            for (String macAddress : macAddresses) {
                // 锁定当前行为：接口抛 SocketException 时元素为 null（疑似缺陷，另行记录）
                if (macAddress == null) {
                    continue;
                }
                assertThat(macAddress).matches("([0-9A-F]{2}-){5}[0-9A-F]{2}");
            }
        } else {
            // 锁定当前行为：接口硬件地址不可用时 getHardwareAddress 返回 null 引发 NPE
            // （疑似缺陷：NPE 不在 getMacByInetAddress 的 SocketException 捕获范围内，另行记录）
            assertThat(thrown).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    @DisplayName("getCpuSerial 采集失败时返回空串而非 null")
    void shouldReturnNonNullCpuSerial() {
        assertThat(ServerInfoUtils.getCpuSerial()).isNotNull();
    }

    @Test
    @DisplayName("getMainBoardSerial 采集失败时返回空串而非 null")
    void shouldReturnNonNullMainBoardSerial() {
        assertThat(ServerInfoUtils.getMainBoardSerial()).isNotNull();
    }

    @Test
    @DisplayName("getServerInfos 正常时组装全部校验信息，采集异常时包装为 LicenseException")
    void shouldAssembleLicenseExtraModel() {
        Throwable thrown = catchThrowable(ServerInfoUtils::getServerInfos);
        if (thrown == null) {
            LicenseExtraModel serverInfos = ServerInfoUtils.getServerInfos();
            assertThat(serverInfos).isNotNull();
            assertThat(serverInfos.getIpAddress()).isNotNull();
            assertThat(serverInfos.getMacAddress()).isNotNull();
            assertThat(serverInfos.getCpuSerial()).isNotNull();
            assertThat(serverInfos.getMainBoardSerial()).isNotNull();
        } else {
            // 锁定当前行为：任一硬件信息采集异常时整体失败并包装为 LicenseException
            assertThat(thrown).isInstanceOf(LicenseException.class);
        }
    }

    @Test
    @DisplayName("getServerInfos 二次调用与首轮行为一致，命中缓存不改变结果")
    void shouldReuseCachedServerInfos() {
        Throwable firstThrown = catchThrowable(ServerInfoUtils::getServerInfos);
        Throwable secondThrown = catchThrowable(ServerInfoUtils::getServerInfos);
        // 锁定当前行为：静态容器缓存使重复调用复用首轮结果，成功或失败语义跨调用保持
        if (firstThrown == null) {
            assertThat(secondThrown).isNull();
        } else {
            assertThat(firstThrown).isInstanceOf(LicenseException.class);
            assertThat(secondThrown).isInstanceOf(LicenseException.class);
        }
    }
}
