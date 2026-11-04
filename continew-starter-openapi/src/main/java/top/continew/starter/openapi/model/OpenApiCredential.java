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

package top.continew.starter.openapi.model;

import java.time.LocalDateTime;

/**
 * 开放应用凭证（凭证查询 SPI 的返回模型）
 *
 * <p>由使用方（如管理端）按 accessKey 查询业务存储后返回。密钥字段不参与
 * {@link #toString()}，防止经日志意外泄漏。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class OpenApiCredential {

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 应用密钥（服务端持有，绝不进日志、异常与审计）
     */
    private String secretKey;

    /**
     * 应用是否已禁用
     */
    private boolean disabled;

    /**
     * 应用过期时间，null 表示不过期
     */
    private LocalDateTime expireTime;

    public OpenApiCredential() {
    }

    public OpenApiCredential(Long appId, String secretKey, boolean disabled,
        LocalDateTime expireTime) {
        this.appId = appId;
        this.secretKey = secretKey;
        this.disabled = disabled;
        this.expireTime = expireTime;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 输出不含密钥的摘要信息，避免密钥经日志泄漏
     *
     * @return 含应用 ID 与禁用状态的描述
     */
    @Override
    public String toString() {
        return "OpenApiCredential{appId=" + appId + ", disabled=" + disabled + "}";
    }
}
