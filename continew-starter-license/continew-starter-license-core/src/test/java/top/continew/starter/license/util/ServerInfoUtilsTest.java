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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.continew.starter.license.model.LicenseExtraModel;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
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
    @DisplayName("getMacAddress 正常返回 MAC 格式集合，硬件地址不可用的网卡被跳过")
    void shouldCollectMacAddressSet() throws Exception {
        Set<String> macAddresses = ServerInfoUtils.getMacAddress();
        assertThat(macAddresses).isNotNull().doesNotContainNull();
        for (String macAddress : macAddresses) {
            assertThat(macAddress).matches("([0-9A-F]{2}-){5}[0-9A-F]{2}");
        }
    }

    @Test
    @DisplayName("按地址未找到网卡时跳过，不抛 NPE 也不进入集合")
    void shouldSkipWhenNicNotFoundByInetAddress() throws Exception {
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            InetAddress address = InetAddress.getByName("192.168.1.10");
            NetworkInterface nic = mock(NetworkInterface.class);
            when(nic.getInetAddresses()).thenReturn(Collections.enumeration(Collections
                .singletonList(address)));
            mocked.when(NetworkInterface::getNetworkInterfaces)
                .thenReturn(Collections.enumeration(Collections.singletonList(nic)));
            mocked.when(() -> NetworkInterface.getByInetAddress(any())).thenReturn(null);

            Set<String> macAddresses = ServerInfoUtils.getMacAddress();

            assertThat(macAddresses).isNotNull();
            assertThat(macAddresses).isEmpty();
        }
    }

    @Test
    @DisplayName("网卡 getHardwareAddress 返回 null 时跳过该网卡，不抛 NPE 也不进入集合")
    void shouldSkipNicWithNullHardwareAddress() throws Exception {
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            InetAddress address = InetAddress.getByName("192.168.1.10");
            NetworkInterface nic = mock(NetworkInterface.class);
            when(nic.getInetAddresses()).thenReturn(Collections.enumeration(Collections
                .singletonList(address)));
            when(nic.getHardwareAddress()).thenReturn(null);
            mocked.when(NetworkInterface::getNetworkInterfaces)
                .thenReturn(Collections.enumeration(Collections.singletonList(nic)));
            mocked.when(() -> NetworkInterface.getByInetAddress(any())).thenReturn(nic);

            Set<String> macAddresses = ServerInfoUtils.getMacAddress();

            assertThat(macAddresses).isNotNull();
            assertThat(macAddresses).isEmpty();
        }
    }

    @Test
    @DisplayName("网卡 getHardwareAddress 返回空数组时跳过该网卡，不产生空串元素")
    void shouldSkipNicWithEmptyHardwareAddress() throws Exception {
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            InetAddress address = InetAddress.getByName("192.168.1.10");
            NetworkInterface nic = mock(NetworkInterface.class);
            when(nic.getInetAddresses()).thenReturn(Collections.enumeration(Collections
                .singletonList(address)));
            when(nic.getHardwareAddress()).thenReturn(new byte[0]);
            mocked.when(NetworkInterface::getNetworkInterfaces)
                .thenReturn(Collections.enumeration(Collections.singletonList(nic)));
            mocked.when(() -> NetworkInterface.getByInetAddress(any())).thenReturn(nic);

            Set<String> macAddresses = ServerInfoUtils.getMacAddress();

            assertThat(macAddresses).isNotNull();
            assertThat(macAddresses).isEmpty();
        }
    }

    @Test
    @DisplayName("网卡 getHardwareAddress 抛 SocketException 时跳过该网卡，整体采集不失败")
    void shouldSkipNicThrowingSocketException() throws Exception {
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            InetAddress address = InetAddress.getByName("192.168.1.10");
            NetworkInterface nic = mock(NetworkInterface.class);
            when(nic.getInetAddresses()).thenReturn(Collections.enumeration(Collections
                .singletonList(address)));
            when(nic.getHardwareAddress()).thenThrow(new SocketException("模拟网卡硬件地址不可用"));
            mocked.when(NetworkInterface::getNetworkInterfaces)
                .thenReturn(Collections.enumeration(Collections.singletonList(nic)));
            mocked.when(() -> NetworkInterface.getByInetAddress(any())).thenReturn(nic);

            Set<String> macAddresses = ServerInfoUtils.getMacAddress();

            assertThat(macAddresses).isNotNull();
            assertThat(macAddresses).isEmpty();
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
    @DisplayName("getServerInfos 正常组装全部校验信息且 MAC 集合无 null 元素")
    void shouldAssembleLicenseExtraModel() {
        LicenseExtraModel serverInfos = ServerInfoUtils.getServerInfos();
        assertThat(serverInfos).isNotNull();
        assertThat(serverInfos.getIpAddress()).isNotNull();
        assertThat(serverInfos.getMacAddress()).isNotNull();
        assertThat(serverInfos.getMacAddress()).doesNotContainNull();
        assertThat(serverInfos.getCpuSerial()).isNotNull();
        assertThat(serverInfos.getMainBoardSerial()).isNotNull();
    }

    @Test
    @DisplayName("getServerInfos 二次调用命中静态缓存，各校验信息集合为同一引用")
    void shouldReuseCachedServerInfos() {
        LicenseExtraModel first = ServerInfoUtils.getServerInfos();
        LicenseExtraModel second = ServerInfoUtils.getServerInfos();
        assertThat(second).isNotNull();
        assertThat(second.getIpAddress()).isSameAs(first.getIpAddress());
        assertThat(second.getMacAddress()).isSameAs(first.getMacAddress());
        assertThat(second.getCpuSerial()).isSameAs(first.getCpuSerial());
        assertThat(second.getMainBoardSerial()).isSameAs(first.getMainBoardSerial());
    }
}
