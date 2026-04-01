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

package top.continew.starter.auth.openapi.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * 开放 API 应用信息
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class OpenApiApp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用密钥
     */
    private String appSecret;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用状态（0-禁用 1-启用）
     */
    private Integer status;

    /**
     * 签名算法
     */
    private String signAlgorithm;

    public OpenApiApp() {
    }

    public OpenApiApp(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }
}
