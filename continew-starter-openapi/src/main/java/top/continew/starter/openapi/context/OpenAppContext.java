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

package top.continew.starter.openapi.context;

/**
 * 开放应用上下文
 *
 * <p>承载本次开放调用的应用身份，与登录用户上下文完全隔离：开放调用没有登录用户，
 * 业务层识别开放调用应读取 {@link OpenAppContextHolder}，不得写入任何用户上下文。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class OpenAppContext {

    /**
     * 应用 ID
     */
    private final Long appId;

    /**
     * 应用访问标识（accessKey）
     */
    private final String accessKey;

    /**
     * 本次放行的接口标识（可空），取「METHOD + 路径」
     */
    private final String authorizedApi;

    /**
     * 请求关联 ID
     */
    private final String requestId;

    public OpenAppContext(Long appId, String accessKey, String authorizedApi, String requestId) {
        this.appId = appId;
        this.accessKey = accessKey;
        this.authorizedApi = authorizedApi;
        this.requestId = requestId;
    }

    public Long getAppId() {
        return appId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getAuthorizedApi() {
        return authorizedApi;
    }

    public String getRequestId() {
        return requestId;
    }
}
