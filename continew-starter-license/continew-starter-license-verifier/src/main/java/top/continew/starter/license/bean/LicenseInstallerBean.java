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

import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.license.autoconfigure.LicenseVerifyProperties;
import top.continew.starter.license.exception.LicenseException;
import top.continew.starter.license.manager.CustomLicenseManager;

import java.io.File;
import java.nio.file.Paths;

/**
 * 证书安装业务类
 *
 * @author loach
 * @since 1.2.0
 */
public class LicenseInstallerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseInstallerBean.class);

    private final LicenseVerifyProperties properties;
    private LicenseManager licenseManager;

    public LicenseInstallerBean(LicenseVerifyProperties properties) {
        this.properties = properties;
    }

    /**
     * 安装许可证
     */
    public void installLicense() {
        try {
            this.licenseManager = CustomLicenseManager.getInstance(properties);
            licenseManager.uninstall();
            File licenseFile =
                Paths.get(properties.getStorePath(), "clientLicense", "license.lic").toFile();
            LicenseContent licenseContent = licenseManager.install(licenseFile);
            LOGGER.info("证书认证通过，安装成功: {}", licenseContent.getSubject());
        } catch (Exception e) {
            throw new LicenseException("证书认证失败", e);
        }
    }

    /**
     * 卸载许可证
     */
    public void uninstallLicense() {
        if (licenseManager != null) {
            try {
                licenseManager.uninstall();
                LOGGER.info("证书已卸载");
            } catch (Exception e) {
                LOGGER.warn("卸载证书失败", e);
            }
        }
    }

    /**
     * 即时验证证书合法性
     */
    public void verify() {
        if (licenseManager != null) {
            try {
                licenseManager.verify();
                LOGGER.info("证书验证成功");
            } catch (Exception e) {
                throw new LicenseException("证书认证失败", e);
            }
        } else {
            throw new LicenseException("证书认证失败: licenseManager is null");
        }
    }

}
