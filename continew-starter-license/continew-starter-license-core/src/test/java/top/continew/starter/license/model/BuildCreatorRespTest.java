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

import net.lingala.zip4j.ZipFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link BuildCreatorResp} 构造与读写行为测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class BuildCreatorRespTest {

    @Test
    @DisplayName("全参构造保留 param 与 clientZipFile 引用")
    void shouldKeepReferencesFromFullArgsConstructor() {
        // ZipFile 为惰性对象，仅构造不触发磁盘 IO
        ZipFile zipFile = new ZipFile("dummy-client.zip");
        LicenseCreatorParam param = new LicenseCreatorParam();
        param.setSubject("license-subject");

        BuildCreatorResp resp = new BuildCreatorResp(param, zipFile);
        assertThat(resp.getParam()).isSameAs(param);
        assertThat(resp.getClientZipFile()).isSameAs(zipFile);
    }

    @Test
    @DisplayName("无参构造字段为 null，setter 写入后 getter 原样读出")
    void shouldRoundTripViaSetters() {
        BuildCreatorResp resp = new BuildCreatorResp();
        assertThat(resp.getParam()).isNull();
        assertThat(resp.getClientZipFile()).isNull();

        LicenseCreatorParam param = new LicenseCreatorParam();
        ZipFile zipFile = new ZipFile("dummy-client.zip");
        resp.setParam(param);
        resp.setClientZipFile(zipFile);
        assertThat(resp.getParam()).isSameAs(param);
        assertThat(resp.getClientZipFile()).isSameAs(zipFile);
    }
}
