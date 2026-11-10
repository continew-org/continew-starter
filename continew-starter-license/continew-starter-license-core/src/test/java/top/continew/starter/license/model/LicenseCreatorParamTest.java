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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Set;

/**
 * {@link LicenseCreatorParam} 默认值与读写行为测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseCreatorParamTest {

    @Test
    @DisplayName("新建参数携带默认值：issuedTime 取当前时间、consumerType 为 user、consumerAmount 为 1")
    void shouldProvideDefaultsOnCreation() {
        long before = System.currentTimeMillis();
        LicenseCreatorParam param = new LicenseCreatorParam();
        // issuedTime 依赖 JVM 时钟，亚毫秒精度在不同时钟源间存在偏差，只以毫秒下界约束
        assertThat(param.getIssuedTime().getTime()).isGreaterThanOrEqualTo(before);
        assertThat(param.getConsumerType()).isEqualTo("user");
        assertThat(param.getConsumerAmount()).isEqualTo(1);
        assertThat(param.getSubject()).isNull();
        assertThat(param.getPrivateAlias()).isNull();
        assertThat(param.getKeyPass()).isNull();
        assertThat(param.getStorePass()).isNull();
        assertThat(param.getLicensePath()).isNull();
        assertThat(param.getPrivateKeysStorePath()).isNull();
        assertThat(param.getExpiryTime()).isNull();
        assertThat(param.getDescription()).isNull();
        assertThat(param.getLicenseExtraModel()).isNull();
    }

    @Test
    @DisplayName("全部字段 setter 写入后 getter 原样读出")
    void shouldRoundTripAllProperties() {
        LicenseExtraModel extraModel = new LicenseExtraModel();
        extraModel.setCpuSerial("BFEBFBFF000806EC");
        Date issuedTime = new Date(0L);
        Date expiryTime = new Date(32503680000000L);

        LicenseCreatorParam param = new LicenseCreatorParam();
        param.setSubject("license-subject");
        param.setPrivateAlias("private-alias");
        param.setKeyPass("key-pass-123");
        param.setStorePass("store-pass-123");
        param.setLicensePath("D:/license/license.lic");
        param.setPrivateKeysStorePath("D:/license/privateKeys.store");
        param.setIssuedTime(issuedTime);
        param.setExpiryTime(expiryTime);
        param.setConsumerType("user-set");
        param.setConsumerAmount(8);
        param.setDescription("描述信息");
        param.setLicenseExtraModel(extraModel);

        assertThat(param.getSubject()).isEqualTo("license-subject");
        assertThat(param.getPrivateAlias()).isEqualTo("private-alias");
        assertThat(param.getKeyPass()).isEqualTo("key-pass-123");
        assertThat(param.getStorePass()).isEqualTo("store-pass-123");
        assertThat(param.getLicensePath()).isEqualTo("D:/license/license.lic");
        assertThat(param.getPrivateKeysStorePath()).isEqualTo("D:/license/privateKeys.store");
        assertThat(param.getIssuedTime()).isSameAs(issuedTime);
        assertThat(param.getExpiryTime()).isSameAs(expiryTime);
        assertThat(param.getConsumerType()).isEqualTo("user-set");
        assertThat(param.getConsumerAmount()).isEqualTo(8);
        assertThat(param.getDescription()).isEqualTo("描述信息");
        assertThat(param.getLicenseExtraModel()).isSameAs(extraModel);
    }

    @Test
    @DisplayName("issuedTime 可被显式覆盖为 null")
    void shouldAllowNullIssuedTimeAfterOverride() {
        // 锁定当前行为：默认值仅由字段初始化提供，显式置 null 后不被兜底
        LicenseCreatorParam param = new LicenseCreatorParam();
        param.setIssuedTime(null);
        assertThat(param.getIssuedTime()).isNull();
    }

    @Test
    @DisplayName("licenseExtraModel 的 Set 字段引用透传")
    void shouldPassThroughLicenseExtraModelCollections() {
        LicenseExtraModel extraModel = new LicenseExtraModel();
        extraModel.setIpAddress(Set.of("192.168.1.10"));
        extraModel.setMacAddress(Set.of("AA-BB-CC-DD-EE-FF"));

        LicenseCreatorParam param = new LicenseCreatorParam();
        param.setLicenseExtraModel(extraModel);
        assertThat(param.getLicenseExtraModel().getIpAddress()).containsExactly("192.168.1.10");
        assertThat(param.getLicenseExtraModel().getMacAddress())
            .containsExactly("AA-BB-CC-DD-EE-FF");
    }
}
