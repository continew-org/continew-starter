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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.CollUtils;
import top.continew.starter.license.exception.LicenseException;
import top.continew.starter.license.model.LicenseExtraModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Set;

/**
 * 服务器信息工具类
 *
 * @author Rong.Jia
 * @since 2.12.0
 */
public class ServerInfoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfoUtils.class);

    private static class ServerInfosContainer {

        private static Set<String> ipAddress = null;
        private static Set<String> macAddress = null;
        private static String cpuSerial = null;
        private static String mainBoardSerial = null;
    }

    private ServerInfoUtils() {
    }

    /**
     * 组装需要额外校验的License参数
     *
     * @return {@link LicenseExtraModel }
     */
    public static LicenseExtraModel getServerInfos() {
        LicenseExtraModel result = new LicenseExtraModel();
        try {
            initServerInfos();
            result.setIpAddress(ServerInfosContainer.ipAddress);
            result.setMacAddress(ServerInfosContainer.macAddress);
            result.setCpuSerial(ServerInfosContainer.cpuSerial);
            result.setMainBoardSerial(ServerInfosContainer.mainBoardSerial);
        } catch (Exception e) {
            LOGGER.error("获取服务器硬件信息异常", e);
            throw new LicenseException(String.format("获取服务器硬件信息异常, %s", e.getMessage()));
        }
        return result;
    }

    /**
     * 初始化服务器硬件信息，并将信息缓存到内存
     *
     * @throws Exception 例外
     */
    private static void initServerInfos() throws Exception {
        if (ServerInfosContainer.ipAddress == null) {
            ServerInfosContainer.ipAddress = getIpAddress();
        }
        if (ServerInfosContainer.macAddress == null) {
            ServerInfosContainer.macAddress = getMacAddress();
        }
        if (ServerInfosContainer.cpuSerial == null) {
            ServerInfosContainer.cpuSerial = getCpuSerial();
        }
        if (ServerInfosContainer.mainBoardSerial == null) {
            ServerInfosContainer.mainBoardSerial = getMainBoardSerial();
        }
    }

    /**
     * 获取服务器临时磁盘位置
     *
     * @return {@link String}
     */
    public static String getServerTempPath() {
        return System.getProperty("user.dir");
    }

    /**
     * 在仅当前用户可访问的私有临时目录中创建脚本文件，避免共享临时目录下的脚本被其他用户篡改
     *
     * @param suffix  脚本文件后缀
     * @param content 脚本内容
     * @return 脚本文件
     * @throws IOException 创建或写入文件失败
     */
    private static File createPrivateTempScript(String suffix, String content) throws IOException {
        Path tempDir = createPrivateTempDirectory();
        tempDir.toFile().deleteOnExit();
        File script = createPrivateTempFile(tempDir, suffix).toFile();
        script.deleteOnExit();
        try (FileWriter fw = new FileWriter(script, StandardCharsets.UTF_8)) {
            fw.write(content);
        }
        return script;
    }

    /**
     * 在给定目录中创建临时脚本文件：POSIX 系统在创建时即限定属主可读写（rw-------），
     * 非 POSIX 系统退回默认权限（文件位于属主独占目录内，同样受保护）
     */
    private static Path createPrivateTempFile(Path dir, String suffix) throws IOException {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE);
            return Files.createTempFile(dir, "hw-info", suffix,
                PosixFilePermissions.asFileAttribute(perms));
        } catch (UnsupportedOperationException e) {
            return Files.createTempFile(dir, "hw-info", suffix);
        }
    }

    /**
     * 创建仅当前用户可访问（rwx------）的私有临时目录，避免共享临时目录下的脚本被其他用户读写
     */
    // S5443 针对 POSIX 世界可写临时目录：POSIX 分支已显式限定 rwx------；
    // Windows 回退分支使用 %USERPROFILE%\AppData\Local\Temp，按用户 ACL 隔离、不存在世界可写问题
    @SuppressWarnings("java:S5443")
    private static Path createPrivateTempDirectory() throws IOException {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
            return Files.createTempDirectory("continew-license-",
                PosixFilePermissions.asFileAttribute(perms));
        } catch (UnsupportedOperationException e) {
            return Files.createTempDirectory("continew-license-");
        }
    }

    /**
     * 获取 Windows 脚本宿主 cscript 的绝对路径，避免依赖可被篡改的 PATH 环境变量
     *
     * @return cscript 可执行文件路径
     */
    private static String getWindowsCscriptPath() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            systemRoot = "C:\\Windows";
        }
        return systemRoot + "\\System32\\cscript.exe";
    }

    /**
     * 获取CPU序列号
     *
     * @return String CPU 序列号
     */
    public static String getCpuSerial() {
        return FileUtil.isWindows() ? getWindowCpuSerial() : getLinuxCpuSerial();
    }

    /**
     * 获取主板序列号
     *
     * @return String 主板序列号
     */
    public static String getMainBoardSerial() {
        return FileUtil.isWindows() ? getWindowMainBoardSerial() : getLinuxMainBoardSerial();
    }

    /**
     * 获取linux cpu 序列号
     *
     * @return {@link String}
     */
    private static String getLinuxCpuSerial() {
        String result = StringConstants.EMPTY;
        String cpuIdCmd = "dmidecode";
        // 使用绝对路径调用 shell，避免依赖可被篡改的 PATH 环境变量
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cpuIdCmd});
            try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int index;
                while ((line = bufferedReader.readLine()) != null) {
                    // 寻找标示字符串[uuid]
                    index = line.toLowerCase().indexOf("uuid");
                    if (index >= 0) {
                        // 取出序列号并去除两边空格
                        result = line.substring(index + "uuid".length() + 1).trim();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("获取Linux cpu信息错误 {}", e.getMessage());
        }
        return result.trim();
    }

    /**
     * 获取Window cpu 序列号
     *
     * @return {@link String}
     */
    private static String getWindowCpuSerial() {
        StringBuilder result = new StringBuilder(StringConstants.EMPTY);
        String vbs = """
            Set objWMIService = GetObject("winmgmts:\\\\.\\root\\cimv2")
            Set colItems = objWMIService.ExecQuery("Select * from Win32_Processor")

            For Each objItem In colItems
                WScript.Echo objItem.ProcessorId
                Exit For ' do the first cpu only!
            Next
            """;
        try {
            File file = createPrivateTempScript(".vbs", vbs);
            Process p = new ProcessBuilder(getWindowsCscriptPath(), "//NoLogo", file.getPath())
                .start();
            try (BufferedReader input = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    result.append(line);
                }
            } finally {
                FileUtil.del(file);
            }
        } catch (Exception e) {
            LOGGER.error("获取window cpu信息错误, {}", e.getMessage());
        }
        return result.toString().trim();
    }

    /**
     * 获取Linux主板序列号
     *
     * @return {@link String}
     */
    private static String getLinuxMainBoardSerial() {
        String command = "dmidecode | grep 'Serial Number' | awk '{print $3}' | tail -1";
        try {
            // 使用绝对路径调用 shell，避免依赖可被篡改的 PATH 环境变量
            Process process = new ProcessBuilder("/bin/sh", "-c", command).start();
            try (BufferedReader reader =
                new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().findFirst().orElse(StringConstants.EMPTY);
            }
        } catch (IOException e) {
            LOGGER.error("获取 Linux 主板序列号失败: {}", e.getMessage());
            return StringConstants.EMPTY;
        }
    }

    /**
     * 获取window主板序列号
     *
     * @return {@link String}
     */
    private static String getWindowMainBoardSerial() {
        StringBuilder result = new StringBuilder(StringConstants.EMPTY);
        String vbs = """
            Set objWMIService = GetObject("winmgmts:\\\\.\\root\\cimv2")
            Set colItems = objWMIService.ExecQuery _
               ("Select * from Win32_BaseBoard")
            For Each objItem in colItems
                Wscript.Echo objItem.SerialNumber
                exit for  ' do the first cpu only!
            Next
            """;
        try {
            File file = createPrivateTempScript(".vbs", vbs);
            Process p = new ProcessBuilder(getWindowsCscriptPath(), "//NoLogo", file.getPath())
                .start();
            try (BufferedReader input = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    result.append(line);
                }
            } finally {
                FileUtil.del(file);
            }
        } catch (Exception e) {
            LOGGER.error("获取Window主板信息错误 {}", e.getMessage());
        }
        return result.toString().trim();
    }

    /**
     * <p>获取Mac地址</p>
     *
     * @return List&lt;String&gt; Mac地址
     * @throws Exception 默认异常
     */
    public static Set<String> getMacAddress() throws Exception {
        // 获取所有网络接口
        Set<InetAddress> inetAddresses = getLocalAllInetAddress();
        if (CollectionUtil.isNotEmpty(inetAddresses)) {
            return CollUtils.mapToSet(inetAddresses, ServerInfoUtils::getMacByInetAddress);
        }
        return Collections.emptySet();
    }

    /**
     * <p>获取IP地址</p>
     *
     * @return List&lt;String&gt; IP地址
     * @throws Exception 默认异常
     */
    public static Set<String> getIpAddress() throws Exception {
        // 获取所有网络接口
        Set<InetAddress> inetAddresses = getLocalAllInetAddress();
        if (CollectionUtil.isNotEmpty(inetAddresses)) {
            return CollUtils.mapToSet(inetAddresses, InetAddress::getHostAddress);
        }
        return Collections.emptySet();
    }

    /**
     * <p>获取某个网络地址对应的Mac地址</p>
     *
     * @param inetAddr 网络地址
     * @return String Mac地址
     */
    private static String getMacByInetAddress(InetAddress inetAddr) {
        try {
            byte[] mac = NetworkInterface.getByInetAddress(inetAddr).getHardwareAddress();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    stringBuilder.append("-");
                }
                // 将十六进制byte转化为字符串
                String temp = Integer.toHexString(mac[i] & 0xff);
                if (temp.length() == 1) {
                    stringBuilder.append("0").append(temp);
                } else {
                    stringBuilder.append(temp);
                }
            }
            return stringBuilder.toString().toUpperCase();
        } catch (SocketException e) {
            LOGGER.error("getMacByInetAddress {}", e.getMessage());
        }
        return null;
    }

    /**
     * <p>获取当前服务器所有符合条件的网络地址</p>
     *
     * @return List&lt;InetAddress&gt; 网络地址列表
     * @throws SocketException 获取网络接口信息失败
     */
    private static Set<InetAddress> getLocalAllInetAddress() throws SocketException {
        Set<InetAddress> result = CollUtil.newHashSet();
        // 遍历所有的网络接口
        for (Enumeration<NetworkInterface> networkInterfaces =
            NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements();) {
            NetworkInterface ni = networkInterfaces.nextElement();
            // 在所有的接口下再遍历IP
            for (Enumeration<InetAddress> addresses = ni.getInetAddresses(); addresses
                .hasMoreElements();) {
                InetAddress address = addresses.nextElement();
                //排除LoopbackAddress、SiteLocalAddress、LinkLocalAddress、MulticastAddress类型的IP地址
                /*&& !inetAddr.isSiteLocalAddress()*/
                if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()
                    && !address.isMulticastAddress()) {
                    result.add(address);
                }
            }
        }
        return result;
    }
}
