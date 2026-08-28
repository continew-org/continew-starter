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

package top.continew.starter.license.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import de.schlichtherle.license.LicenseManager;
import jakarta.annotation.PostConstruct;
import top.continew.starter.license.initializing.LicenseStarterInitializingBean;
import top.continew.starter.license.bean.LicenseInstallerBean;
import top.continew.starter.license.manager.CustomLicenseManager;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * license 校验模块 自动配置
 *
 * @author loach
 * @since 2.12.0
 */
@AutoConfiguration
@EnableConfigurationProperties(LicenseVerifyProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.LICENSE_VERIFIER,
    name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class LicenseVerifyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LicenseVerifyAutoConfiguration.class);

    /**
     * 证书安装业务类
     *
     * @param properties 属性
     * @return {@link LicenseInstallerBean }
     */
    @Bean
    public LicenseInstallerBean licenseInstallerBean(LicenseVerifyProperties properties) {
        return new LicenseInstallerBean(properties);
    }

    /**
     * 启动校验 License服务
     *
     * @param licenseInstallerBean 许可证安装程序bean
     * @return {@link LicenseStarterInitializingBean }
     */
    @Bean
    @DependsOn("licenseInstallerBean")
    public LicenseStarterInitializingBean licenseStarterInitializingBean(
        LicenseInstallerBean licenseInstallerBean) {
        return new LicenseStarterInitializingBean(licenseInstallerBean);
    }

    /**
     * 客户端证书管理类(证书验证)
     *
     * @param properties 属性
     * @return {@link LicenseManager }
     */
    @Bean
    public LicenseManager licenseManager(LicenseVerifyProperties properties) {
        return CustomLicenseManager.getInstance(properties);
    }

    @PostConstruct
    public void postConstruct() {
        log.debug(
            "[ContiNew Starter] - Auto Configuration 'License-Verifier' completed initialization.");
    }

}
