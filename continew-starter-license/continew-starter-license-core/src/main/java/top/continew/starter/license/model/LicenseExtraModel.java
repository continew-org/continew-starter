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

package top.continew.starter.license.model;

import java.io.Serializable;
import java.util.Set;

/**
 * 额外的服务器硬件校验信息对象,这里的属性可根据需求自定义
 *
 * @author loach
 * @since 2.12.0
 */
public class LicenseExtraModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 可被允许的IP地址
     */
    private Set<String> ipAddress;

    /**
     * 可被允许的mac地址
     */
    private Set<String> macAddress;

    /**
     * 可被允许的CPU序列号
     */
    private String cpuSerial;

    /**
     * 可被允许的主板序列号
     */
    private String mainBoardSerial;

    public Set<String> getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(Set<String> ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Set<String> getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(Set<String> macAddress) {
        this.macAddress = macAddress;
    }

    public String getCpuSerial() {
        return cpuSerial;
    }

    public void setCpuSerial(String cpuSerial) {
        this.cpuSerial = cpuSerial;
    }

    public String getMainBoardSerial() {
        return mainBoardSerial;
    }

    public void setMainBoardSerial(String mainBoardSerial) {
        this.mainBoardSerial = mainBoardSerial;
    }

}
