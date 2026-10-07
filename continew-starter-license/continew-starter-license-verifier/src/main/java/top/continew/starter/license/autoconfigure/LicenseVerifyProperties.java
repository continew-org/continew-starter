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

import cn.hutool.core.io.FileUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * license 校验模块配置属性
 *
 * @author loach
 * @since 2.12.0
 */
@ConfigurationProperties(PropertiesConstants.LICENSE_VERIFIER)
public class LicenseVerifyProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 生成的license文件所在路径
     */
    private String storePath = FileUtil.getTmpDirPath();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }
}
