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

package top.continew.starter.auth.openapi.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import top.continew.starter.auth.openapi.autoconfigure.dao.OpenApiAppDaoProperties;
import top.continew.starter.auth.openapi.enums.SignAlgorithm;
import top.continew.starter.core.constant.PropertiesConstants;

/**
 * 开放 API 配置属性
 *
 * @author Charles7c
 * @since 2.16.0
 */
@ConfigurationProperties(PropertiesConstants.AUTH_OPENAPI)
public class OpenApiProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 应用 ID 参数名
     */
    private String appIdParamName = "appId";

    /**
     * 签名参数名
     */
    private String signParamName = "sign";

    /**
     * 时间戳参数名
     */
    private String timestampParamName = "timestamp";

    /**
     * 随机字符串参数名
     */
    private String nonceParamName = "nonce";

    /**
     * 时间戳过期时间（毫秒）
     */
    private Long timestampExpireInMillis = 5 * 60 * 1000L;

    /**
     * 是否启用 nonce 防重放
     */
    private boolean nonceEnabled = true;

    /**
     * 签名算法
     */
    private SignAlgorithm signAlgorithm = SignAlgorithm.MD5;

    /**
     * 应用 DAO 配置
     */
    @NestedConfigurationProperty
    private OpenApiAppDaoProperties appDao;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppIdParamName() {
        return appIdParamName;
    }

    public void setAppIdParamName(String appIdParamName) {
        this.appIdParamName = appIdParamName;
    }

    public String getSignParamName() {
        return signParamName;
    }

    public void setSignParamName(String signParamName) {
        this.signParamName = signParamName;
    }

    public String getTimestampParamName() {
        return timestampParamName;
    }

    public void setTimestampParamName(String timestampParamName) {
        this.timestampParamName = timestampParamName;
    }

    public String getNonceParamName() {
        return nonceParamName;
    }

    public void setNonceParamName(String nonceParamName) {
        this.nonceParamName = nonceParamName;
    }

    public Long getTimestampExpireInMillis() {
        return timestampExpireInMillis;
    }

    public void setTimestampExpireInMillis(Long timestampExpireInMillis) {
        this.timestampExpireInMillis = timestampExpireInMillis;
    }

    public boolean isNonceEnabled() {
        return nonceEnabled;
    }

    public void setNonceEnabled(boolean nonceEnabled) {
        this.nonceEnabled = nonceEnabled;
    }

    public SignAlgorithm getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(SignAlgorithm signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    public OpenApiAppDaoProperties getAppDao() {
        return appDao;
    }

    public void setAppDao(OpenApiAppDaoProperties appDao) {
        this.appDao = appDao;
    }
}
