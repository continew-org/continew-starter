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

package top.continew.starter.auth.openapi.interceptor;

/**
 * 开放 API 常量
 *
 * @author Charles7c
 * @since 2.16.0
 */
public final class OpenApiConstants {

    private OpenApiConstants() {
    }

    /**
     * 请求属性 - 应用 ID
     */
    public static final String REQUEST_ATTR_APP_ID = "openapi_app_id";

    /**
     * 请求属性 - 应用信息
     */
    public static final String REQUEST_ATTR_APP = "openapi_app";
}
