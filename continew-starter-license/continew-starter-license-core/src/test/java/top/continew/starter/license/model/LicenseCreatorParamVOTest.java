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

/**
 * {@link LicenseCreatorParamVO} 读写行为测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseCreatorParamVOTest {

    @Test
    @DisplayName("新建对象全部字段为 null，无隐藏默认值")
    void shouldCreateWithAllFieldsNull() {
        LicenseCreatorParamVO vo = new LicenseCreatorParamVO();
        assertThat(vo.getExpireTime()).isNull();
        assertThat(vo.getCustomerName()).isNull();
        assertThat(vo.getStorePass()).isNull();
        assertThat(vo.getKeyPass()).isNull();
        assertThat(vo.getDescription()).isNull();
        assertThat(vo.getLicenseSavePath()).isNull();
        assertThat(vo.getLicenseExtraModel()).isNull();
    }

    @Test
    @DisplayName("全部字段 setter 写入后 getter 原样读出")
    void shouldRoundTripAllProperties() {
        Date expireTime = new Date(4102444800000L);
        LicenseExtraModel extraModel = new LicenseExtraModel();
        extraModel.setMainBoardSerial("MB-serial-01");

        LicenseCreatorParamVO vo = new LicenseCreatorParamVO();
        vo.setExpireTime(expireTime);
        vo.setCustomerName("客户名称");
        vo.setStorePass("store-pass-123");
        vo.setKeyPass("key-pass-123");
        vo.setDescription("描述信息");
        vo.setLicenseSavePath("D:/license/license.lic");
        vo.setLicenseExtraModel(extraModel);

        assertThat(vo.getExpireTime()).isSameAs(expireTime);
        assertThat(vo.getCustomerName()).isEqualTo("客户名称");
        assertThat(vo.getStorePass()).isEqualTo("store-pass-123");
        assertThat(vo.getKeyPass()).isEqualTo("key-pass-123");
        assertThat(vo.getDescription()).isEqualTo("描述信息");
        assertThat(vo.getLicenseSavePath()).isEqualTo("D:/license/license.lic");
        assertThat(vo.getLicenseExtraModel()).isSameAs(extraModel);
    }
}
