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

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.schlichtherle.license.CipherParam;
import de.schlichtherle.license.DefaultCipherParam;
import de.schlichtherle.license.DefaultLicenseParam;
import de.schlichtherle.license.KeyStoreParam;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import de.schlichtherle.license.LicenseParam;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.license.exception.LicenseException;
import top.continew.starter.license.manager.ServerLicenseManager;
import top.continew.starter.license.model.BuildCreatorResp;
import top.continew.starter.license.model.ConfigParam;
import top.continew.starter.license.model.CustomKeyStoreParam;
import top.continew.starter.license.model.LicenseCreatorParam;
import top.continew.starter.license.model.LicenseCreatorParamVO;
import top.continew.starter.license.model.LicenseExtraModel;
import top.continew.starter.license.util.ExecCmdUtil;
import top.continew.starter.license.util.ServerInfoUtils;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * 证书生成接口 实现类
 *
 * @author loach
 * @since 2.12.0
 */
public class LicenseCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseCreateService.class);

    private static volatile LicenseCreateService instance;

    private static final X500Principal DEFAULT_HOLDER_ISSUER = new X500Principal(
        "CN=localhost, OU=localhost, O=localhost, L=SH, ST=SH, C=CN");

    private LicenseCreateService() {
    }

    /**
     * 获取实例
     *
     * @return {@link LicenseCreateService }
     */
    public static LicenseCreateService getInstance() {
        if (instance == null) {
            synchronized (LicenseCreateService.class) {
                if (instance == null) {
                    instance = new LicenseCreateService();
                }
            }
        }
        return instance;
    }

    /**
     * 获取服务器信息
     *
     * @return {@link LicenseExtraModel }
     */
    public LicenseExtraModel getServerInfo() {
        return ServerInfoUtils.getServerInfos();
    }

    /**
     * 生成一个证书
     *
     * @param paramVO param vo
     * @throws Exception 例外
     */
    public void generateLicense(LicenseCreatorParamVO paramVO) throws Exception {
        BuildCreatorResp buildCreatorResp = buildCreator(paramVO);
        LicenseCreatorParam param = buildCreatorResp.getParam();
        ZipFile clientZipFile = buildCreatorResp.getClientZipFile();
        try {
            LicenseParam licenseParam = initLicenseParam(param);
            LicenseManager licenseManager = new ServerLicenseManager(licenseParam);
            LicenseContent licenseContent = initLicenseContent(param);
            licenseManager.store(licenseContent, new File(param.getLicensePath()));
            LOGGER.info("{} 证书生成成功 路径: {}", param.getSubject(), param.getLicensePath());
            clientZipFile.addFile(param.getLicensePath());
        } catch (Exception e) {
            throw new LicenseException("生成证书失败:" + param.getSubject(), e);
        }
    }

    /**
     * 构建 License 创建者对象及客户端配置压缩包。
     *
     * @param paramVO 创建参数封装对象，包含客户名、密码、描述、扩展信息等。
     * @return Map 包含 LicenseCreatorParam（creator） 和 生成的客户端 Zip 文件（clientZipFile）
     * @throws Exception 命令执行或文件操作过程中出现异常
     */
    private BuildCreatorResp buildCreator(LicenseCreatorParamVO paramVO) throws Exception {
        String customerName = paramVO.getCustomerName();
        String privateAlias = customerName + "-private-alias";
        String publicAlias = customerName + "-public-alias";
        String currentCustomerDir =
            relativePath(paramVO) + customerName + IdUtil.fastSimpleUUID() + File.separator;
        File customerDirFile = new File(currentCustomerDir);
        if (!customerDirFile.exists() && !customerDirFile.mkdirs()) {
            throw new IOException("Failed to create directory: " + currentCustomerDir);
        }

        String privateKeystore = currentCustomerDir + "privateKeys.keystore";
        String publicKeystore = currentCustomerDir + "publicCerts.keystore";
        String certFilePath = currentCustomerDir + "certfile.cer";
        String licensePath = currentCustomerDir + "license.lic";

        LicenseCreatorParam param = new LicenseCreatorParam();
        param.setSubject(customerName);
        param.setPrivateAlias(privateAlias);
        param.setKeyPass(paramVO.getKeyPass());
        param.setStorePass(paramVO.getStorePass());
        param.setLicensePath(licensePath);
        param.setPrivateKeysStorePath(privateKeystore);
        param.setExpiryTime(paramVO.getExpireTime());
        param.setDescription(paramVO.getDescription());
        param.setLicenseExtraModel(paramVO.getLicenseExtraModel());

        int validity = getValidity(param.getIssuedTime(), paramVO.getExpireTime());
        String dname = "\"CN=localhost, OU=localhost, O=localhost, L=SH, ST=SH, C=CN\"";

        // 生成私钥库
        String keyAlgOption = checkJavaVersion() ? "-keyalg DSA" : ""; // JDK>=17 要指定 keyalg
        String genKeyCmd = String
            .format(
                "keytool -genkeypair %s -keysize 1024 -validity %d -alias %s -keystore %s -storepass %s -keypass %s -dname %s",
                keyAlgOption, validity, privateAlias, privateKeystore, paramVO
                    .getStorePass(),
                paramVO.getKeyPass(), dname);

        // 导出证书
        String exportCertCmd = String
            .format("keytool -exportcert -alias %s -keystore %s -storepass %s -file \"%s\"",
                privateAlias,
                privateKeystore, paramVO
                    .getStorePass(),
                certFilePath);

        // 导入到公钥库
        String importCertCmd = String
            .format("keytool -noprompt -import -alias %s -file \"%s\" -keystore %s -storepass %s",
                publicAlias,
                certFilePath, publicKeystore, paramVO
                    .getStorePass());

        // 执行命令
        ExecCmdUtil.exec(genKeyCmd);
        ExecCmdUtil.exec(exportCertCmd);
        ExecCmdUtil.exec(importCertCmd);

        // 生成客户端配置文件
        ZipFile clientZipFile = generateClientConfig(param, currentCustomerDir, publicAlias);
        return new BuildCreatorResp(param, clientZipFile);
    }

    /**
     * 校验JDK版本
     *
     * @return boole T 17 版本 F 非 17 版本
     * @throws Exception 例外
     */
    private boolean checkJavaVersion() throws Exception {
        String version = System.getProperty("java.version");
        int currentVersion = 0;
        if (version.startsWith("1.")) {
            currentVersion = Integer.parseInt(version.split("\\.")[1]);
        } else {
            currentVersion = Integer.parseInt(version.split("\\.")[0]);
        }
        return currentVersion >= 17;
    }

    private ZipFile generateClientConfig(LicenseCreatorParam param,
        String currentCustomerDir,
        String publicAlias) throws Exception {
        ZipFile clientLicense = new ZipFile(currentCustomerDir + "clientLicense.zip");
        File config = new File(currentCustomerDir + "clientConfig.json");
        ConfigParam configParam = new ConfigParam();
        configParam.setPublicAlias(publicAlias);
        configParam.setStorePass(param.getStorePass());
        configParam.setSubject(param.getSubject());
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(configParam);
        try (FileOutputStream out = new FileOutputStream(config)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new LicenseException("密钥文件生成失败", e);
        }
        List<File> files = new ArrayList<>();
        files.add(config);
        files.add(new File(currentCustomerDir + "publicCerts.keystore"));
        clientLicense.addFiles(files);
        return clientLicense;
    }

    /**
     * 将有效时间转换成天
     *
     * @param issuedTime 签发时间
     * @param expireTime 过期时间
     * @return int
     */
    private int getValidity(Date issuedTime, Date expireTime) {
        long issued = issuedTime.getTime();
        long expire = expireTime.getTime();
        long differ = expire - issued;
        long remaining = differ % (24L * 3600L * 1000L);
        long validity = differ / (24L * 3600L * 1000L);
        if (remaining > 0) {
            validity++;
        }
        return (int) validity;
    }

    /**
     * 是否是 Windows
     *
     * @return boolean
     */
    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().contains("windows");
    }

    /**
     * 证书生成路径
     *
     * @param paramVO param vo
     * @return {@link String }
     */
    private String relativePath(LicenseCreatorParamVO paramVO) {
        if (paramVO.getLicenseSavePath() != null) {
            return paramVO.getLicenseSavePath();
        }
        if (isWindows()) {
            return "C:/license/";
        }
        return "/data/license/";
    }

    /**
     * 设置证书生成参数
     */
    private LicenseParam initLicenseParam(LicenseCreatorParam param) {
        Preferences preferences = Preferences.userNodeForPackage(LicenseCreateService.class);
        //设置密钥
        CipherParam cipherParam = new DefaultCipherParam(param.getStorePass());
        KeyStoreParam privateStoreParam = new CustomKeyStoreParam(LicenseCreateService.class, param
            .getPrivateKeysStorePath(), param.getPrivateAlias(), param.getStorePass(),
            param.getKeyPass());
        return new DefaultLicenseParam(param.getSubject(), preferences, privateStoreParam,
            cipherParam);

    }

    /**
     * 设置证书生成内容
     *
     * @param param 参数
     * @return {@link LicenseContent }
     */
    private LicenseContent initLicenseContent(LicenseCreatorParam param) {
        LicenseContent licenseContent = new LicenseContent();
        licenseContent.setHolder(DEFAULT_HOLDER_ISSUER);
        licenseContent.setIssuer(DEFAULT_HOLDER_ISSUER);
        licenseContent.setSubject(param.getSubject());
        licenseContent.setIssued(param.getIssuedTime());
        licenseContent.setNotBefore(param.getIssuedTime());
        licenseContent.setNotAfter(param.getExpiryTime());
        licenseContent.setConsumerType(param.getConsumerType());
        licenseContent.setConsumerAmount(param.getConsumerAmount());
        licenseContent.setInfo(param.getDescription());

        if (param.getLicenseExtraModel() != null) {
            licenseContent.setExtra(param.getLicenseExtraModel());
        }

        return licenseContent;
    }

}
