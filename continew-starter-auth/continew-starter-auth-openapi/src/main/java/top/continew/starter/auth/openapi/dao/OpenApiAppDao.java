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

package top.continew.starter.auth.openapi.dao;

import top.continew.starter.auth.openapi.model.OpenApiApp;

/**
 * 开放 API 应用 DAO
 *
 * @author Charles7c
 * @since 2.16.0
 */
public interface OpenApiAppDao {

    /**
     * 根据应用 ID 获取应用信息
     *
     * @param appId 应用 ID
     * @return 应用信息
     */
    OpenApiApp getByAppId(String appId);

    /**
     * 检查 nonce 是否已使用（防重放攻击）
     *
     * @param nonce   随机字符串
     * @param appId   应用 ID
     * @param timeout 超时时间（毫秒）
     * @return true: 已使用; false: 未使用
     */
    boolean isNonceUsed(String nonce, String appId, long timeout);

    /**
     * 记录 nonce 已使用
     *
     * @param nonce   随机字符串
     * @param appId   应用 ID
     * @param timeout 超时时间（毫秒）
     */
    void recordNonce(String nonce, String appId, long timeout);
}
